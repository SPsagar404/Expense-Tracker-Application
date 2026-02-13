package com.expensemanager.notification.entity;

import com.expensemanager.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotificationPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Builder.Default
    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Builder.Default
    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled = false;

    @Builder.Default
    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled = true;

    @Builder.Default
    @Column(name = "budget_alert_enabled", nullable = false)
    private boolean budgetAlertEnabled = true;

    @Builder.Default
    @Column(name = "subscription_alert_enabled", nullable = false)
    private boolean subscriptionAlertEnabled = true;

    @Builder.Default
    @Column(name = "goal_alert_enabled", nullable = false)
    private boolean goalAlertEnabled = true;

    @Builder.Default
    @Column(name = "large_expense_alert_enabled", nullable = false)
    private boolean largeExpenseAlertEnabled = true;
}

