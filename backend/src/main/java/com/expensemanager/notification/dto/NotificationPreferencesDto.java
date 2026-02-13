package com.expensemanager.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesDto {
    private boolean emailEnabled;
    private boolean pushEnabled;
    private boolean inAppEnabled;
    private boolean budgetAlertEnabled;
    private boolean subscriptionAlertEnabled;
    private boolean goalAlertEnabled;
    private boolean largeExpenseAlertEnabled;
}

