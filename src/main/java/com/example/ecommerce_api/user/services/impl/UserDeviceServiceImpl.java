package com.example.ecommerce_api.user.services.impl;

import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.user.dto.response.DeviceResponse;
import com.example.ecommerce_api.user.entity.User;
import com.example.ecommerce_api.user.entity.UserDevice;
import com.example.ecommerce_api.user.repository.RefreshTokenRepository;
import com.example.ecommerce_api.user.repository.UserDeviceRepository;
import com.example.ecommerce_api.user.services.UserDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeviceServiceImpl implements UserDeviceService {

    private final UserDeviceRepository userDeviceRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public UserDevice findOrCreateDevice(User user, String deviceId, String deviceName, String deviceType) {
        Instant now = Instant.now();

        return userDeviceRepository.findByUserIdAndDeviceId(user.getId(), deviceId)
                .map(device -> {
                    device.setDeviceName(deviceName);
                    device.setDeviceType(deviceType);
                    device.touch(now);
                    return device;
                })
                .orElseGet(() -> createDevice(user, deviceId, deviceName, deviceType, now));
    }

    /**
     * The uk_user_device constraint is the real guard against duplicate devices;
     * this handles the race where two logins from a brand-new device land here
     * at the same time by falling back to the row the loser's competitor created.
     */
    private UserDevice createDevice(User user, String deviceId, String deviceName, String deviceType, Instant now) {
        UserDevice device = UserDevice.builder()
                .user(user)
                .deviceId(deviceId)
                .deviceName(deviceName)
                .deviceType(deviceType)
                .lastSeenAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            UserDevice saved = userDeviceRepository.saveAndFlush(device);
            log.info("Registered new device {} for user {}", saved.getId(), user.getId());
            return saved;
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent device registration detected for user {}, deviceId {}", user.getId(), deviceId);
            return userDeviceRepository.findByUserIdAndDeviceId(user.getId(), deviceId)
                    .orElseThrow(() -> e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponse> listDevices(Long userId, Long currentDeviceId) {
        Instant now = Instant.now();
        return userDeviceRepository.findByUserIdOrderByLastSeenAtDesc(userId).stream()
                .map(device -> toResponse(device, currentDeviceId, now))
                .toList();
    }

    @Override
    @Transactional
    public void revokeDevice(Long userId, Long deviceId) {
        UserDevice device = userDeviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        refreshTokenRepository.revokeAllForDevice(device.getId(), Instant.now());
        log.info("Revoked device {} for user {}", device.getId(), userId);
    }

    private DeviceResponse toResponse(UserDevice device, Long currentDeviceId, Instant now) {
        boolean active = refreshTokenRepository.existsByDevice_IdAndRevokedFalseAndExpiresAtAfter(device.getId(), now);

        return DeviceResponse.builder()
                .id(device.getId())
                .deviceId(device.getDeviceId())
                .deviceName(device.getDeviceName())
                .deviceType(device.getDeviceType())
                .lastSeenAt(device.getLastSeenAt())
                .createdAt(device.getCreatedAt())
                .current(device.getId().equals(currentDeviceId))
                .active(active)
                .build();
    }
}
