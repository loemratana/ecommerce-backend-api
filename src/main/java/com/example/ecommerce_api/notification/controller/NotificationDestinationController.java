package com.example.ecommerce_api.notification.controller;

import com.example.ecommerce_api.Enum.NotificationType;
import com.example.ecommerce_api.common.response.ApiResponse;
import com.example.ecommerce_api.notification.dto.NotificationDestinationRequest;
import com.example.ecommerce_api.notification.dto.NotificationDestinationResponse;
import com.example.ecommerce_api.notification.service.NotificationDestinationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Settings → Notifications: which Telegram groups and email addresses get which store events. */
@RestController
@RequestMapping("api/v1/settings/notification-destinations")
@Tag(name = "settings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class NotificationDestinationController {

    private final NotificationDestinationService destinationService;

    @Operation(summary = "List notification destinations")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDestinationResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(destinationService.findAll()));
    }

    @Operation(summary = "Whether each channel is configured",
            description = "true when the server has the channel's credentials (TELEGRAM_BOT_TOKEN; SMTP_HOST and EMAIL_FROM)")
    @GetMapping("/channels")
    public ResponseEntity<ApiResponse<Map<NotificationType, Boolean>>> getChannelStatus() {
        return ResponseEntity.ok(ApiResponse.success(destinationService.channelStatus()));
    }

    @Operation(summary = "Get a notification destination")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationDestinationResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(destinationService.findById(id)));
    }

    @Operation(summary = "Add a notification destination",
            description = "TELEGRAM target: the group's chat id (e.g. -1001234567890; add the bot to the group first) "
                    + "or @channelusername. EMAIL target: an email address.")
    @PostMapping
    public ResponseEntity<ApiResponse<NotificationDestinationResponse>> create(
            @Valid @RequestBody NotificationDestinationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(destinationService.create(request), "Notification destination added"));
    }

    @Operation(summary = "Update a notification destination")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationDestinationResponse>> update(
            @PathVariable Long id, @Valid @RequestBody NotificationDestinationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(destinationService.update(id, request), "Notification destination saved"));
    }

    @Operation(summary = "Delete a notification destination")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        destinationService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification destination deleted"));
    }

    @Operation(summary = "Send a test message to a destination",
            description = "Sent right away; a 400 carries the provider's reason, e.g. \"chat not found\"")
    @PostMapping("/{id}/test")
    public ResponseEntity<ApiResponse<Void>> sendTest(@PathVariable Long id) {
        destinationService.sendTest(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Test message sent"));
    }
}
