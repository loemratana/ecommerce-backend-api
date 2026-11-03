package com.example.ecommerce_api.notification.mapper;

import com.example.ecommerce_api.Enum.NotificationEvent;
import com.example.ecommerce_api.notification.dto.NotificationDestinationRequest;
import com.example.ecommerce_api.notification.dto.NotificationDestinationResponse;
import com.example.ecommerce_api.notification.entity.NotificationDestination;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;

@Component
public class NotificationDestinationMapper {

    /** Copies the request onto a new or existing destination; target is expected to be normalized already. */
    public NotificationDestination apply(NotificationDestination destination, NotificationDestinationRequest request,
                                         String target) {
        destination.setName(request.getName().trim());
        destination.setChannel(request.getChannel());
        destination.setTarget(target);
        destination.getEvents().clear();
        destination.getEvents().addAll(request.getEvents());
        destination.setEnabled(request.getEnabled() == null || request.getEnabled());
        return destination;
    }

    public NotificationDestinationResponse toResponse(NotificationDestination destination) {
        return NotificationDestinationResponse.builder()
                .id(destination.getId())
                .name(destination.getName())
                .channel(destination.getChannel())
                .target(destination.getTarget())
                .events(destination.getEvents().isEmpty()
                        ? EnumSet.noneOf(NotificationEvent.class)
                        : EnumSet.copyOf(destination.getEvents()))
                .enabled(destination.isEnabled())
                .createdAt(destination.getCreatedAt())
                .updatedAt(destination.getUpdatedAt())
                .build();
    }

    public List<NotificationDestinationResponse> toResponseList(List<NotificationDestination> destinations) {
        return destinations.stream().map(this::toResponse).toList();
    }
}
