package com.expensemanager.notification.service;

import com.expensemanager.entity.Budget;
import com.expensemanager.entity.SalaryAllocation;
import com.expensemanager.entity.Transaction;
import com.expensemanager.entity.User;
import com.expensemanager.notification.entity.Notification;
import com.expensemanager.notification.entity.NotificationType;
import com.expensemanager.notification.entity.UserNotificationPreferences;
import com.expensemanager.notification.repository.NotificationRepository;
import com.expensemanager.notification.repository.UserNotificationPreferencesRepository;
import com.expensemanager.repository.BudgetRepository;
import com.expensemanager.repository.SalaryAllocationRepository;
import com.expensemanager.repository.SubscriptionRepository;
import com.expensemanager.repository.TransactionRepository;
import com.expensemanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepo;
    @Mock
    private UserNotificationPreferencesRepository preferencesRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private BudgetRepository budgetRepo;
    @Mock
    private TransactionRepository transactionRepo;
    @Mock
    private SubscriptionRepository subscriptionRepo;
    @Mock
    private SalaryAllocationRepository salaryRepo;
    @Mock
    private EmailNotificationService emailService;
    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private NotificationService notificationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();
    }

    @Test
    void handleTransactionCreated_WhenBudgetExceeded_CreatesBudgetAlert() {
        LocalDate txDate = LocalDate.of(2026, 1, 15);
        Transaction tx = Transaction.builder()
                .id(10L)
                .user(user)
                .amount(BigDecimal.valueOf(600))
                .category("Food & Groceries")
                .transactionDate(txDate)
                .build();

        Budget budget = Budget.builder()
                .id(100L)
                .user(user)
                .category("Food & Groceries")
                .month(1)
                .year(2026)
                .limitAmount(BigDecimal.valueOf(500))
                .build();

        when(budgetRepo.findByUserIdAndCategoryAndMonthAndYear(1L, "Food & Groceries", 1, 2026))
                .thenReturn(Optional.of(budget));
        when(transactionRepo.getTotalSpentByCategory(anyLong(), anyString(), any(), any()))
                .thenReturn(BigDecimal.valueOf(600));
        when(notificationRepo.existsByUserIdAndTypeAndReferenceIdAndReadFalse(1L,
                NotificationType.BUDGET_ALERT, 100L)).thenReturn(false);
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(notificationRepo.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(1L);
            return n;
        });

        notificationService.handleTransactionCreated(tx);

        verify(notificationRepo).save(argThat(n ->
                n.getType() == NotificationType.BUDGET_ALERT
                        && n.getUser().getId().equals(1L)
                        && n.getReferenceId().equals(100L)
        ));
    }

    @Test
    void runHourlyNotificationJobInternal_SubscriptionReminder_CreatesNotification() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 13, 10, 0);
        LocalDate reminderDate = now.toLocalDate().plusDays(2);

        com.expensemanager.entity.Subscription sub = com.expensemanager.entity.Subscription.builder()
                .id(200L)
                .user(user)
                .merchant("Netflix")
                .amount(BigDecimal.valueOf(15))
                .interval("MONTHLY")
                .nextBillingDate(reminderDate)
                .active(true)
                .build();

        when(subscriptionRepo.findByActiveTrueAndNextBillingDate(reminderDate))
                .thenReturn(List.of(sub));
        when(preferencesRepo.findByUserId(1L)).thenReturn(Optional.empty()); // default prefs = enabled
        when(notificationRepo.existsByUserIdAndTypeAndReferenceIdAndReadFalse(1L,
                NotificationType.SUBSCRIPTION_REMINDER, 200L)).thenReturn(false);
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(notificationRepo.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(2L);
            return n;
        });

        notificationService.runHourlyNotificationJobInternal(now);

        verify(notificationRepo).save(argThat(n ->
                n.getType() == NotificationType.SUBSCRIPTION_REMINDER
                        && n.getUser().getId().equals(1L)
                        && n.getReferenceId().equals(200L)
        ));
    }

    @Test
    void handleTransactionCreated_WhenBudgetAlertsDisabled_DoesNotCreateNotification() {
        LocalDate txDate = LocalDate.of(2026, 1, 10);
        Transaction tx = Transaction.builder()
                .id(11L)
                .user(user)
                .amount(BigDecimal.valueOf(600))
                .category("Food & Groceries")
                .transactionDate(txDate)
                .build();

        Budget budget = Budget.builder()
                .id(101L)
                .user(user)
                .category("Food & Groceries")
                .month(1)
                .year(2026)
                .limitAmount(BigDecimal.valueOf(500))
                .build();

        UserNotificationPreferences prefs = UserNotificationPreferences.builder()
                .user(user)
                .emailEnabled(true)
                .inAppEnabled(true)
                .pushEnabled(false)
                .budgetAlertEnabled(false) // disabled
                .subscriptionAlertEnabled(true)
                .goalAlertEnabled(true)
                .largeExpenseAlertEnabled(true)
                .build();

        when(preferencesRepo.findByUserId(1L)).thenReturn(Optional.of(prefs));
        when(budgetRepo.findByUserIdAndCategoryAndMonthAndYear(1L, "Food & Groceries", 1, 2026))
                .thenReturn(Optional.of(budget));

        notificationService.handleTransactionCreated(tx);

        verify(notificationRepo, never()).save(any(Notification.class));
    }

    @Test
    void updatePreferences_PersistsAndReturnsUpdatedValues() {
        UserNotificationPreferences existing = UserNotificationPreferences.builder()
                .id(1L)
                .user(user)
                .emailEnabled(true)
                .pushEnabled(false)
                .inAppEnabled(true)
                .budgetAlertEnabled(true)
                .subscriptionAlertEnabled(true)
                .goalAlertEnabled(true)
                .largeExpenseAlertEnabled(true)
                .build();

        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(preferencesRepo.findByUserId(1L)).thenReturn(Optional.of(existing));
        when(preferencesRepo.save(any(UserNotificationPreferences.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var request = com.expensemanager.notification.dto.NotificationPreferencesDto.builder()
                .emailEnabled(false)
                .pushEnabled(true)
                .inAppEnabled(true)
                .budgetAlertEnabled(false)
                .subscriptionAlertEnabled(true)
                .goalAlertEnabled(true)
                .largeExpenseAlertEnabled(false)
                .build();

        var result = notificationService.updatePreferences(1L, request);

        assertEquals(false, result.isEmailEnabled());
        assertEquals(true, result.isPushEnabled());
        assertEquals(false, result.isBudgetAlertEnabled());
        assertEquals(false, result.isLargeExpenseAlertEnabled());
    }
}

