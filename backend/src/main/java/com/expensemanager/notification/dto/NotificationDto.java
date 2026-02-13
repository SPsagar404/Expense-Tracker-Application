package com.expensemanager.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String type;
    private String title;
    private String message;
    private Long referenceId;
    private boolean read;
    private boolean sent;
    private LocalDateTime createdAt;
    private LocalDateTime scheduledAt;
}

