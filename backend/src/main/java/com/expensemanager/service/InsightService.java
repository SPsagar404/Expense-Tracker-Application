package com.expensemanager.service;

import com.expensemanager.dto.InsightDto;
import com.expensemanager.entity.Subscription;
import com.expensemanager.repository.SubscriptionRepository;
import com.expensemanager.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InsightService {

    private final TransactionRepository transactionRepository;
    private final SubscriptionRepository subscriptionRepository;

    public InsightDto getSummary(Long userId) {
        log.info("Generating insights for user {}", userId);
        List<InsightDto.InsightItem> insights = new ArrayList<>();

        // 1. Flag high expenses vs. 3-month average
        flagHighExpenses(userId, insights);

        // 2. Flag duplicate subscriptions
        flagDuplicateSubscriptions(userId, insights);

        return InsightDto.builder()
                .insights(insights)
                .totalFlags(insights.size())
                .build();
    }

    private void flagHighExpenses(Long userId, List<InsightDto.InsightItem> insights) {
        LocalDate now = LocalDate.now();
        LocalDate currentMonthStart = now.withDayOfMonth(1);
        LocalDate currentMonthEnd = now.withDayOfMonth(now.lengthOfMonth());

        // Get category spending for current month
        List<Object[]> currentBreakdown = transactionRepository
                .getCategoryBreakdown(userId, currentMonthStart, currentMonthEnd);

        // Get 3-month average per category
        LocalDate threeMonthsAgo = currentMonthStart.minusMonths(3);
        List<Object[]> historicalBreakdown = transactionRepository
                .getCategoryBreakdown(userId, threeMonthsAgo, currentMonthStart.minusDays(1));

        Map<String, BigDecimal> historicalAvg = new HashMap<>();
        for (Object[] row : historicalBreakdown) {
            String category = (String) row[0];
            BigDecimal totalAmount = (BigDecimal) row[1];
            historicalAvg.put(category, totalAmount.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP));
        }

        for (Object[] row : currentBreakdown) {
            String category = (String) row[0];
            BigDecimal currentAmount = (BigDecimal) row[1];
            BigDecimal avgAmount = historicalAvg.getOrDefault(category, BigDecimal.ZERO);

            if (avgAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal threshold = avgAmount.multiply(BigDecimal.valueOf(1.5));
                if (currentAmount.compareTo(threshold) > 0) {
                    insights.add(InsightDto.InsightItem.builder()
                            .type("HIGH_EXPENSE")
                            .severity("WARNING")
                            .message(String.format(
                                    "%s spending ($%s) is %.0f%% higher than your 3-month average ($%s)",
                                    category, currentAmount,
                                    currentAmount.subtract(avgAmount)
                                            .divide(avgAmount, 2, RoundingMode.HALF_UP)
                                            .multiply(BigDecimal.valueOf(100)).doubleValue(),
                                    avgAmount))
                            .category(category)
                            .amount(currentAmount)
                            .build());
                }
            }
        }
    }

    private void flagDuplicateSubscriptions(Long userId, List<InsightDto.InsightItem> insights) {
        List<Subscription> subs = subscriptionRepository.findByUserIdAndActiveTrue(userId);

        // Group by merchant (case-insensitive)
        Map<String, List<Subscription>> grouped = subs.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getMerchant().toLowerCase()));

        for (Map.Entry<String, List<Subscription>> entry : grouped.entrySet()) {
            if (entry.getValue().size() > 1) {
                BigDecimal totalAmount = entry.getValue().stream()
                        .map(Subscription::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                insights.add(InsightDto.InsightItem.builder()
                        .type("DUPLICATE_SUBSCRIPTION")
                        .severity("INFO")
                        .message(String.format(
                                "You have %d active subscriptions for %s totaling $%s/month",
                                entry.getValue().size(),
                                entry.getValue().get(0).getMerchant(),
                                totalAmount))
                        .category("Subscription")
                        .amount(totalAmount)
                        .build());
            }
        }
    }
}
