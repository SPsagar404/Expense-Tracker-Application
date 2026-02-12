package com.expensemanager.service;

import com.expensemanager.dto.ReportDto;
import com.expensemanager.entity.Transaction;
import com.expensemanager.entity.User;
import com.expensemanager.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @InjectMocks
    private ReportService reportService;

    @Test
    void getMonthlyReport_ReturnsCorrectTotals() {
        when(transactionRepository.getTotalSpent(eq(1L), any(), any()))
                .thenReturn(BigDecimal.valueOf(1500));

        when(transactionRepository.getCategoryBreakdown(eq(1L), any(), any()))
                .thenReturn(List.of(
                        new Object[] { "Food & Groceries", BigDecimal.valueOf(500) },
                        new Object[] { "Transportation", BigDecimal.valueOf(400) },
                        new Object[] { "Shopping", BigDecimal.valueOf(600) }));

        User user = User.builder().id(1L).build();
        when(transactionRepository.findByUserIdAndTransactionDateBetween(eq(1L), any(), any()))
                .thenReturn(List.of(
                        Transaction.builder().user(user)
                                .amount(BigDecimal.valueOf(100))
                                .transactionDate(LocalDate.of(2026, 1, 5)).build(),
                        Transaction.builder().user(user)
                                .amount(BigDecimal.valueOf(200))
                                .transactionDate(LocalDate.of(2026, 1, 10)).build()));

        ReportDto report = reportService.getMonthlyReport(1L, 2026, 1);

        assertNotNull(report);
        assertEquals(BigDecimal.valueOf(1500), report.getTotalSpent());
        assertEquals(3, report.getCategoryBreakdown().size());
        assertEquals(31, report.getDailyTrends().size()); // January has 31 days
        assertEquals(1, report.getMonth());
        assertEquals(2026, report.getYear());
    }

    @Test
    void getMonthlyReport_NoTransactions_ReturnsZeros() {
        when(transactionRepository.getTotalSpent(eq(1L), any(), any()))
                .thenReturn(null);
        when(transactionRepository.getCategoryBreakdown(eq(1L), any(), any()))
                .thenReturn(List.of());
        when(transactionRepository.findByUserIdAndTransactionDateBetween(eq(1L), any(), any()))
                .thenReturn(List.of());

        ReportDto report = reportService.getMonthlyReport(1L, 2026, 2);

        assertNotNull(report);
        assertEquals(BigDecimal.ZERO, report.getTotalSpent());
        assertTrue(report.getCategoryBreakdown().isEmpty());
    }
}
