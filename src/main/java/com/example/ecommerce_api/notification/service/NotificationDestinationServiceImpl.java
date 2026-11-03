package com.example.ecommerce_api.notification.service;

import com.example.ecommerce_api.Enum.NotificationType;
import com.example.ecommerce_api.common.exception.BadRequestException;
import com.example.ecommerce_api.common.exception.DuplicateResourceException;
import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.notification.dto.NotificationDestinationRequest;
import com.example.ecommerce_api.notification.dto.NotificationDestinationResponse;
import com.example.ecommerce_api.notification.entity.NotificationDestination;
import com.example.ecommerce_api.notification.mapper.NotificationDestinationMapper;
import com.example.ecommerce_api.notification.repository.NotificationDestinationRepository;
import com.example.ecommerce_api.notification.strategy.NotificationException;
import com.example.ecommerce_api.notification.strategy.NotificationFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationDestinationServiceImpl implements NotificationDestinationService {

    // Group/supergroup ids are negative (supergroups start with -100); public channels can use @username
    private static final Pattern TELEGRAM_CHAT_ID = Pattern.compile("^-?\\d{1,20}$");
    private static final Pattern TELEGRAM_USERNAME = Pattern.compile("^@[A-Za-z][A-Za-z0-9_]{4,31}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final NotificationDestinationRepository destinationRepository;
    private final NotificationDestinationMapper destinationMapper;
    private final NotificationService notificationService;
    private final NotificationFactory notificationFactory;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDestinationResponse> findAll() {
        return destinationMapper.toResponseList(destinationRepository.findAllByOrderByIdAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationDestinationResponse findById(Long id) {
        return destinationMapper.toResponse(findDestination(id));
    }

    @Override
    public NotificationDestinationResponse create(NotificationDestinationRequest request) {
        String target = normalizeTarget(request.getChannel(), request.getTarget());
        if (destinationRepository.existsByChannelAndTargetIgnoreCase(request.getChannel(), target)) {
            throw new DuplicateResourceException("A " + request.getChannel() + " destination for " + target + " already exists");
        }
        NotificationDestination destination = destinationMapper.apply(new NotificationDestination(), request, target);
        return destinationMapper.toResponse(destinationRepository.save(destination));
    }

    @Override
    public NotificationDestinationResponse update(Long id, NotificationDestinationRequest request) {
        NotificationDestination destination = findDestination(id);
        String target = normalizeTarget(request.getChannel(), request.getTarget());
        if (destinationRepository.existsByChannelAndTargetIgnoreCaseAndIdNot(request.getChannel(), target, id)) {
            throw new DuplicateResourceException("A " + request.getChannel() + " destination for " + target + " already exists");
        }
        destinationMapper.apply(destination, request, target);
        return destinationMapper.toResponse(destinationRepository.saveAndFlush(destination));
    }

    @Override
    public void delete(Long id) {
        destinationRepository.delete(findDestination(id));
    }

    @Override
    @Transactional(readOnly = true)
    public void sendTest(Long id) {
        NotificationDestination destination = findDestination(id);
        try {
            notificationService.sendTest(destination);
        } catch (NotificationException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @Override
    public Map<NotificationType, Boolean> channelStatus() {
        return notificationFactory.configuredChannels();
    }

    // ----- helpers ----------------------------------------------------------------------------

    static String normalizeTarget(NotificationType channel, String rawTarget) {
        String target = rawTarget == null ? "" : rawTarget.trim();
        switch (channel) {
            case TELEGRAM -> {
                if (!TELEGRAM_CHAT_ID.matcher(target).matches() && !TELEGRAM_USERNAME.matcher(target).matches()) {
                    throw new BadRequestException(
                            "Telegram target must be a chat id such as -1001234567890, or a @channelusername");
                }
            }
            case EMAIL -> {
                if (!EMAIL.matcher(target).matches()) {
                    throw new BadRequestException("Email target must be a valid email address");
                }
                target = target.toLowerCase(Locale.ROOT);
            }
        }
        return target;
    }

    private NotificationDestination findDestination(Long id) {
        return destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification destination not found"));
    }
}
