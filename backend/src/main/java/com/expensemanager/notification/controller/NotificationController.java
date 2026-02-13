package com.expensemanager.notification.controller;

import com.expensemanager.dto.ApiResponse;
import com.expensemanager.notification.dto.CreateCustomNotificationRequest;
import com.expensemanager.notification.dto.NotificationDto;
import com.expensemanager.notification.service.NotificationService;
import com.expensemanager.notification.dto.UnreadCountResponse;
import com.expensemanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app and scheduled notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List notifications for the current user")
    public ResponseEntity<ApiResponse<Page<NotificationDto>>> getNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        Page<NotificationDto> page = notificationService.getNotifications(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(new UnreadCountResponse(count)));
    }

    @PutMapping("/{id}/mark-read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.markAsRead(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @PostMapping("/custom")
    @Operation(summary = "Create a custom reminder for the current user")
    public ResponseEntity<ApiResponse<Void>> createCustomReminder(
            @Valid @RequestBody CreateCustomNotificationRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.createCustomReminder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Custom reminder scheduled", null));
    }
}

