package com.expensemanager.notification.service;

import com.expensemanager.dto.ApiResponse;
import com.expensemanager.entity.Budget;
import com.expensemanager.entity.SalaryAllocation;
import com.expensemanager.entity.Transaction;
import com.expensemanager.entity.User;
import com.expensemanager.exception.ResourceNotFoundException;
import com.expensemanager.notification.dto.CreateCustomNotificationRequest;
import com.expensemanager.notification.dto.NotificationDto;
import com.expensemanager.notification.dto.NotificationPreferencesDto;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserNotificationPreferencesRepository preferencesRepo;
    private final UserRepository userRepo;
    private final BudgetRepository budgetRepo;
    private final TransactionRepository transactionRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final SalaryAllocationRepository salaryRepo;
    private final EmailNotificationService emailService;
    private final PushNotificationService pushNotificationService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.notifications.large-expense-threshold:5000}")
    private BigDecimal largeExpenseThreshold;

    // -------------------------------------------------------------------------
    // Public API used by controllers
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotifications(Long userId, Pageable pageable) {
        Page<Notification> page = notificationRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        String key = buildUnreadKey(userId);
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof Number number) {
            return number.longValue();
        }
        long count = notificationRepo.countByUserIdAndReadFalse(userId);
        redisTemplate.opsForValue().set(key, count);
        return count;
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification", notificationId);
        }

        notification.setRead(true);
        notificationRepo.save(notification);
        evictUnreadCount(userId);
    }

    @Transactional
    public void createCustomReminder(Long userId, CreateCustomNotificationRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Notification notification = Notification.builder()
                .user(user)
                .type(NotificationType.CUSTOM_REMINDER)
                .title(request.getTitle())
                .message(request.getMessage())
                .scheduledAt(request.getScheduledAt())
                .build();

        notificationRepo.save(notification);
        log.info("Custom reminder created for user {} at {}", userId, request.getScheduledAt());
        evictUnreadCount(userId);
    }

    @Transactional(readOnly = true)
    public NotificationPreferencesDto getPreferences(Long userId) {
        return toDto(getPreferencesOrDefault(userId));
    }

    @Transactional
    public NotificationPreferencesDto updatePreferences(Long userId, NotificationPreferencesDto dto) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        UserNotificationPreferences prefs = preferencesRepo.findByUserId(userId)
                .orElse(UserNotificationPreferences.builder().user(user).build());

        prefs.setEmailEnabled(dto.isEmailEnabled());
        prefs.setPushEnabled(dto.isPushEnabled());
        prefs.setInAppEnabled(dto.isInAppEnabled());
        prefs.setBudgetAlertEnabled(dto.isBudgetAlertEnabled());
        prefs.setSubscriptionAlertEnabled(dto.isSubscriptionAlertEnabled());
        prefs.setGoalAlertEnabled(dto.isGoalAlertEnabled());
        prefs.setLargeExpenseAlertEnabled(dto.isLargeExpenseAlertEnabled());

        preferencesRepo.save(prefs);
        return toDto(prefs);
    }

    // -------------------------------------------------------------------------
    // Domain triggers
    // -------------------------------------------------------------------------

    /**
     * Triggered when a new transaction is created.
     * Handles:
     * - Budget exceeded
     * - Salary allocation overspending
     * - Large/unusual expense
     */
    @Transactional
    public void handleTransactionCreated(Transaction transaction) {
        Long userId = transaction.getUser().getId();
        LocalDate txDate = transaction.getTransactionDate();
        int month = txDate.getMonthValue();
        int year = txDate.getYear();

        UserNotificationPreferences prefs = getPreferencesOrDefault(userId);

        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

        // 1. Budget exceeded: actual_spent > budget_limit
        if (prefs.isBudgetAlertEnabled()) {
            budgetRepo.findByUserIdAndCategoryAndMonthAndYear(
                    userId, transaction.getCategory(), month, year
            ).ifPresent(budget -> checkBudgetExceeded(transaction, budget, startOfMonth, endOfMonth));
        }

        // 2. Salary overspending: actual_spent > allocated_amount
        // Reuse "goal_alert_enabled" as the toggle for planning-related alerts (salary, goals).
        if (prefs.isGoalAlertEnabled()) {
            salaryRepo.findByUserIdAndMonthAndYearAndCategory(userId, month, year, transaction.getCategory())
                    .ifPresent(allocation -> checkSalaryOverspending(transaction, allocation, startOfMonth, endOfMonth));
        }

        // 3. Large expense alert: transaction_amount > configurable_threshold
        if (prefs.isLargeExpenseAlertEnabled()
                && transaction.getAmount() != null
                && transaction.getAmount().compareTo(largeExpenseThreshold) > 0) {
            String title = "Large expense detected";
            String message = String.format(
                    "A large transaction of %s in category '%s' was recorded at %s.",
                    transaction.getAmount(), transaction.getCategory(), transaction.getMerchant()
            );
            createNotificationInternal(
                    userId,
                    NotificationType.LARGE_EXPENSE_ALERT,
                    title,
                    message,
                    transaction.getId(),
                    null
            );
        }
    }

    private void checkBudgetExceeded(Transaction transaction, Budget budget,
            LocalDate start, LocalDate end) {
        Long userId = transaction.getUser().getId();
        BigDecimal spent = transactionRepo.getTotalSpentByCategory(
                userId, budget.getCategory(), start, end);
        if (spent == null) {
            spent = BigDecimal.ZERO;
        }

        if (spent.compareTo(budget.getLimitAmount()) > 0) {
            if (notificationRepo.existsByUserIdAndTypeAndReferenceIdAndReadFalse(
                    userId, NotificationType.BUDGET_ALERT, budget.getId())) {
                return;
            }

            String title = "Budget limit exceeded";
            String message = String.format(
                    "You have spent %s in '%s' for %d/%d, exceeding your budget limit of %s.",
                    spent, budget.getCategory(), budget.getMonth(), budget.getYear(), budget.getLimitAmount()
            );

            createNotificationInternal(
                    userId,
                    NotificationType.BUDGET_ALERT,
                    title,
                    message,
                    budget.getId(),
                    null
            );
        }
    }

    private void checkSalaryOverspending(Transaction transaction, SalaryAllocation allocation,
            LocalDate start, LocalDate end) {
        Long userId = transaction.getUser().getId();
        BigDecimal spent = transactionRepo.getTotalSpentByCategory(
                userId, allocation.getCategory(), start, end);
        if (spent == null) {
            spent = BigDecimal.ZERO;
        }

        if (spent.compareTo(allocation.getAllocatedAmount()) > 0) {
            if (notificationRepo.existsByUserIdAndTypeAndReferenceIdAndReadFalse(
                    userId, NotificationType.SALARY_VARIANCE, allocation.getId())) {
                return;
            }

            String title = "Salary allocation overspent";
            String message = String.format(
                    "Spending in '%s' has exceeded the allocated amount of %s for %d/%d.",
                    allocation.getCategory(),
                    allocation.getAllocatedAmount(),
                    allocation.getMonth(),
                    allocation.getYear()
            );

            createNotificationInternal(
                    userId,
                    NotificationType.SALARY_VARIANCE,
                    title,
                    message,
                    allocation.getId(),
                    null
            );
        }
    }

    // -------------------------------------------------------------------------
    // Scheduler
    // -------------------------------------------------------------------------

    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void runHourlyNotificationJob() {
        runHourlyNotificationJobInternal(LocalDateTime.now());
    }

    @Transactional
    void runHourlyNotificationJobInternal(LocalDateTime now) {
        LocalDate today = now.toLocalDate();

        log.debug("Running hourly notification job at {}", now);

        processSubscriptionReminders(today);
        // Goal deadline reminders would be added here once goal entities exist.
        deliverPendingNotifications(now);
    }

    private void processSubscriptionReminders(LocalDate today) {
        LocalDate reminderDate = today.plusDays(2);
        subscriptionRepo.findByActiveTrueAndNextBillingDate(reminderDate)
                .forEach(sub -> {
                    Long userId = sub.getUser().getId();
                    UserNotificationPreferences prefs = getPreferencesOrDefault(userId);
                    if (!prefs.isSubscriptionAlertEnabled()) {
                        return;
                    }

                    if (notificationRepo.existsByUserIdAndTypeAndReferenceIdAndReadFalse(
                            userId, NotificationType.SUBSCRIPTION_REMINDER, sub.getId())) {
                        return;
                    }

                    String title = "Upcoming subscription billing";
                    String message = String.format(
                            "Your subscription '%s' will be billed on %s for %s.",
                            sub.getMerchant(), sub.getNextBillingDate(), sub.getAmount()
                    );

                    createNotificationInternal(
                            userId,
                            NotificationType.SUBSCRIPTION_REMINDER,
                            title,
                            message,
                            sub.getId(),
                            null
                    );
                });
    }

    private void deliverPendingNotifications(LocalDateTime now) {
        List<Notification> pending = notificationRepo.findBySentFalseAndScheduledAtIsNull();
        pending.addAll(notificationRepo.findBySentFalseAndScheduledAtBefore(now));

        for (Notification notification : pending) {
            Long userId = notification.getUser().getId();
            UserNotificationPreferences prefs = getPreferencesOrDefault(userId);

            try {
                if (prefs.isEmailEnabled()) {
                    emailService.sendNotificationEmail(notification.getUser(), notification);
                }
                if (prefs.isPushEnabled()) {
                    pushNotificationService.sendPushNotification(notification);
                }
                notification.setSent(true);
            } catch (Exception ex) {
                log.error("Failed to deliver notification {} for user {}: {}",
                        notification.getId(), userId, ex.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    protected Notification createNotificationInternal(
            Long userId,
            NotificationType type,
            String title,
            String message,
            Long referenceId,
            LocalDateTime scheduledAt
    ) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .scheduledAt(scheduledAt)
                .build();

        Notification saved = notificationRepo.save(notification);
        log.debug("Created notification {} of type {} for user {}", saved.getId(), type, userId);
        evictUnreadCount(userId);
        return saved;
    }

    private UserNotificationPreferences getPreferencesOrDefault(Long userId) {
        Optional<UserNotificationPreferences> existing = preferencesRepo.findByUserId(userId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Defaults (match DB defaults)
        return UserNotificationPreferences.builder()
                .emailEnabled(true)
                .pushEnabled(false)
                .inAppEnabled(true)
                .budgetAlertEnabled(true)
                .subscriptionAlertEnabled(true)
                .goalAlertEnabled(true)
                .largeExpenseAlertEnabled(true)
                .build();
    }

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .type(n.getType().name())
                .title(n.getTitle())
                .message(n.getMessage())
                .referenceId(n.getReferenceId())
                .read(n.isRead())
                .sent(n.isSent())
                .createdAt(n.getCreatedAt())
                .scheduledAt(n.getScheduledAt())
                .build();
    }

    private NotificationPreferencesDto toDto(UserNotificationPreferences prefs) {
        return NotificationPreferencesDto.builder()
                .emailEnabled(prefs.isEmailEnabled())
                .pushEnabled(prefs.isPushEnabled())
                .inAppEnabled(prefs.isInAppEnabled())
                .budgetAlertEnabled(prefs.isBudgetAlertEnabled())
                .subscriptionAlertEnabled(prefs.isSubscriptionAlertEnabled())
                .goalAlertEnabled(prefs.isGoalAlertEnabled())
                .largeExpenseAlertEnabled(prefs.isLargeExpenseAlertEnabled())
                .build();
    }

    private String buildUnreadKey(Long userId) {
        return "user:" + userId + ":notifications:unread";
    }

    private void evictUnreadCount(Long userId) {
        redisTemplate.delete(buildUnreadKey(userId));
    }
}

