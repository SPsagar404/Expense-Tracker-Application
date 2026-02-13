package com.expensemanager.notification.controller;

import com.expensemanager.dto.ApiResponse;
import com.expensemanager.notification.dto.NotificationPreferencesDto;
import com.expensemanager.notification.service.NotificationService;
import com.expensemanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notification-preferences")
@RequiredArgsConstructor
@Tag(name = "Notification Preferences", description = "User notification channel and alert preferences")
public class NotificationPreferencesController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get current user's notification preferences")
    public ResponseEntity<ApiResponse<NotificationPreferencesDto>> getPreferences() {
        Long userId = SecurityUtils.getCurrentUserId();
        NotificationPreferencesDto prefs = notificationService.getPreferences(userId);
        return ResponseEntity.ok(ApiResponse.success(prefs));
    }

    @PutMapping
    @Operation(summary = "Update current user's notification preferences")
    public ResponseEntity<ApiResponse<NotificationPreferencesDto>> updatePreferences(
            @RequestBody NotificationPreferencesDto request) {
        Long userId = SecurityUtils.getCurrentUserId();
        NotificationPreferencesDto updated = notificationService.updatePreferences(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Notification preferences updated", updated));
    }
}

