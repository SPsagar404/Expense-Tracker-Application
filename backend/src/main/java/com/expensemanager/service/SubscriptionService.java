package com.expensemanager.service;

import com.expensemanager.dto.SubscriptionDto;
import com.expensemanager.dto.SubscriptionSummaryDto;
import com.expensemanager.dto.WasteAnalysisDto;
import com.expensemanager.entity.Subscription;
import com.expensemanager.entity.Transaction;
import com.expensemanager.entity.User;
import com.expensemanager.exception.ResourceNotFoundException;
import com.expensemanager.repository.SalaryAllocationRepository;
import com.expensemanager.repository.SubscriptionRepository;
import com.expensemanager.repository.TransactionRepository;
import com.expensemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepo;
    private final TransactionRepository transactionRepo;
    private final UserRepository userRepo;
    private final SalaryAllocationRepository salaryRepo;

    @Transactional
    public SubscriptionDto createSubscription(Long userId, SubscriptionDto dto) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Subscription sub = Subscription.builder()
                .user(user)
                .merchant(dto.getMerchant())
                .amount(dto.getAmount())
                .category(dto.getCategory())
                .interval(dto.getInterval().toUpperCase())
                .nextBillingDate(dto.getNextBillingDate())
                .lastBilledDate(dto.getLastBilledDate())
                .autoGenerateTransaction(dto.getAutoGenerateTransaction() != null && dto.getAutoGenerateTransaction())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();

        return toDto(subscriptionRepo.save(sub));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionDto> getUserSubscriptions(Long userId) {
        return subscriptionRepo.findByUserId(userId).stream()
                .map(this::toDto).toList();
    }

    @Transactional
    public SubscriptionDto updateSubscription(Long userId, Long subId, SubscriptionDto dto) {
        Subscription sub = subscriptionRepo.findById(subId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", subId));

        if (!sub.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Subscription", subId);
        }

        sub.setMerchant(dto.getMerchant());
        sub.setAmount(dto.getAmount());
        sub.setCategory(dto.getCategory());
        sub.setInterval(dto.getInterval().toUpperCase());
        sub.setNextBillingDate(dto.getNextBillingDate());
        sub.setAutoGenerateTransaction(dto.getAutoGenerateTransaction() != null && dto.getAutoGenerateTransaction());
        sub.setActive(dto.getActive() != null ? dto.getActive() : sub.getActive());

        return toDto(subscriptionRepo.save(sub));
    }

    @Transactional
    public void deleteSubscription(Long userId, Long subId) {
        Subscription sub = subscriptionRepo.findById(subId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", subId));

        if (!sub.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Subscription", subId);
        }

        subscriptionRepo.delete(sub);
    }

    @Transactional(readOnly = true)
    public SubscriptionSummaryDto getSummary(Long userId) {
        List<Subscription> active = subscriptionRepo.findByUserIdAndActiveTrue(userId);

        BigDecimal monthlyTotal = BigDecimal.ZERO;
        BigDecimal yearlyTotal = BigDecimal.ZERO;
        LocalDate sevenDaysLater = LocalDate.now().plusDays(7);
        List<SubscriptionDto> upcoming = new ArrayList<>();

        for (Subscription sub : active) {
            BigDecimal monthly = toMonthlyAmount(sub.getAmount(), sub.getInterval());
            monthlyTotal = monthlyTotal.add(monthly);
            yearlyTotal = yearlyTotal.add(monthly.multiply(BigDecimal.valueOf(12)));

            if (sub.getNextBillingDate() != null &&
                    !sub.getNextBillingDate().isAfter(sevenDaysLater) &&
                    !sub.getNextBillingDate().isBefore(LocalDate.now())) {
                upcoming.add(toDto(sub));
            }
        }

        return SubscriptionSummaryDto.builder()
                .totalMonthlyCommitment(monthlyTotal.setScale(2, RoundingMode.HALF_UP))
                .totalYearlyCommitment(yearlyTotal.setScale(2, RoundingMode.HALF_UP))
                .activeSubscriptions(active.size())
                .upcomingIn7Days(upcoming)
                .build();
    }

    @Transactional(readOnly = true)
    public WasteAnalysisDto analyzeWaste(Long userId) {
        List<Subscription> active = subscriptionRepo.findByUserIdAndActiveTrue(userId);
        List<String> warnings = new ArrayList<>();

        // 1. Duplicate merchants
        Map<String, Long> merchantCounts = active.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getMerchant().toLowerCase().trim(),
                        Collectors.counting()));
        merchantCounts.forEach((merchant, count) -> {
            if (count > 1) {
                warnings.add("You have " + count + " " + merchant + " subscriptions");
            }
        });

        // 2. Subscriptions > 30% of salary
        BigDecimal monthlyTotal = active.stream()
                .map(s -> toMonthlyAmount(s.getAmount(), s.getInterval()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate now = LocalDate.now();
        BigDecimal salaryTotal = salaryRepo.getTotalAllocatedAmount(userId, now.getMonthValue(), now.getYear());
        // Use totalSalary from the first allocation if available
        var salaryAllocations = salaryRepo.findByUserIdAndMonthAndYear(userId, now.getMonthValue(), now.getYear());
        if (!salaryAllocations.isEmpty()) {
            BigDecimal totalSalary = salaryAllocations.get(0).getTotalSalary();
            if (totalSalary.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ratio = monthlyTotal.divide(totalSalary, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                if (ratio.compareTo(BigDecimal.valueOf(30)) > 0) {
                    warnings.add("Subscriptions exceed " + ratio.setScale(0, RoundingMode.HALF_UP) +
                            "% of your salary (threshold: 30%)");
                }
            }
        }

        // 3. Potentially unused subscriptions (not billed in 60+ days)
        for (Subscription sub : active) {
            if (sub.getLastBilledDate() != null) {
                long daysSinceLastBill = ChronoUnit.DAYS.between(sub.getLastBilledDate(), now);
                if (daysSinceLastBill > 60) {
                    warnings.add(sub.getMerchant() + " subscription unused for " + daysSinceLastBill + " days");
                }
            }
        }

        return WasteAnalysisDto.builder().warnings(warnings).build();
    }

    // ---- Auto-Billing Engine ----

    @Scheduled(cron = "0 0 1 * * ?") // 1 AM daily
    @Transactional
    public void processAutoBilling() {
        LocalDate today = LocalDate.now();
        List<Subscription> dueSubs = subscriptionRepo
                .findByActiveTrueAndAutoGenerateTransactionTrueAndNextBillingDate(today);

        log.info("Auto-billing: found {} subscriptions due for {}", dueSubs.size(), today);

        for (Subscription sub : dueSubs) {
            try {
                // Create transaction
                Transaction txn = Transaction.builder()
                        .user(sub.getUser())
                        .amount(sub.getAmount())
                        .currency("USD")
                        .merchant(sub.getMerchant())
                        .category(sub.getCategory() != null ? sub.getCategory() : "Subscriptions")
                        .transactionDate(today)
                        .notes("Auto-generated from subscription: " + sub.getMerchant())
                        .build();
                transactionRepo.save(txn);

                // Update subscription dates
                sub.setLastBilledDate(today);
                sub.setNextBillingDate(advanceDate(today, sub.getInterval()));
                subscriptionRepo.save(sub);

                log.info("Auto-billed subscription {} ({}) for user {}",
                        sub.getId(), sub.getMerchant(), sub.getUser().getId());
            } catch (Exception e) {
                log.error("Failed to auto-bill subscription {}: {}", sub.getId(), e.getMessage());
            }
        }
    }

    // ---- Helpers ----

    private LocalDate advanceDate(LocalDate date, String interval) {
        return switch (interval.toUpperCase()) {
            case "WEEKLY" -> date.plusWeeks(1);
            case "MONTHLY" -> date.plusMonths(1);
            case "YEARLY" -> date.plusYears(1);
            default -> date.plusMonths(1);
        };
    }

    private BigDecimal toMonthlyAmount(BigDecimal amount, String interval) {
        return switch (interval.toUpperCase()) {
            case "WEEKLY" -> amount.multiply(BigDecimal.valueOf(4.33)).setScale(2, RoundingMode.HALF_UP);
            case "YEARLY" -> amount.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            default -> amount;
        };
    }

    private SubscriptionDto toDto(Subscription s) {
        return SubscriptionDto.builder()
                .id(s.getId())
                .merchant(s.getMerchant())
                .amount(s.getAmount())
                .category(s.getCategory())
                .interval(s.getInterval())
                .nextBillingDate(s.getNextBillingDate())
                .lastBilledDate(s.getLastBilledDate())
                .autoGenerateTransaction(s.getAutoGenerateTransaction())
                .active(s.getActive())
                .build();
    }
}
