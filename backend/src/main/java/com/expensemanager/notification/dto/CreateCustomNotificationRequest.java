package com.expensemanager.notification.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomNotificationRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String message;

    @NotNull
    @FutureOrPresent
    private LocalDateTime scheduledAt;
}

