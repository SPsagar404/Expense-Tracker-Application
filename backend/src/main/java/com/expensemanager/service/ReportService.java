package com.expensemanager.service;

import com.expensemanager.dto.ReportDto;
import com.expensemanager.entity.Transaction;
import com.expensemanager.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final TransactionRepository transactionRepository;

    @Cacheable(value = "reports", key = "#userId + '-' + #year + '-' + #month")
    public ReportDto getMonthlyReport(Long userId, int year, int month) {
        log.info("Generating monthly report for user {} - {}/{}", userId, month, year);

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        BigDecimal totalSpent = transactionRepository.getTotalSpent(userId, start, end);
        if (totalSpent == null)
            totalSpent = BigDecimal.ZERO;

        // Category breakdown
        List<Object[]> breakdown = transactionRepository.getCategoryBreakdown(userId, start, end);
        List<ReportDto.CategoryBreakdown> categoryBreakdownList = new ArrayList<>();
        final BigDecimal total = totalSpent;

        for (Object[] row : breakdown) {
            String category = (String) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            double percentage = total.compareTo(BigDecimal.ZERO) > 0
                    ? amount.divide(total, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;

            categoryBreakdownList.add(ReportDto.CategoryBreakdown.builder()
                    .category(category)
                    .amount(amount)
                    .percentage(percentage)
                    .build());
        }

        // Sort by amount descending
        categoryBreakdownList.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));

        // Daily trends
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndTransactionDateBetween(userId, start, end);

        Map<LocalDate, BigDecimal> dailyMap = transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getTransactionDate,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

        List<ReportDto.DailyTrend> dailyTrends = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            dailyTrends.add(ReportDto.DailyTrend.builder()
                    .date(date.toString())
                    .amount(dailyMap.getOrDefault(date, BigDecimal.ZERO))
                    .build());
        }

        return ReportDto.builder()
                .month(month)
                .year(year)
                .totalSpent(totalSpent)
                .categoryBreakdown(categoryBreakdownList)
                .dailyTrends(dailyTrends)
                .build();
    }
}
