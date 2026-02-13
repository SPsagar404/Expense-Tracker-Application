package com.expensemanager.service;

import com.expensemanager.dto.SubscriptionDto;
import com.expensemanager.dto.SubscriptionSummaryDto;
import com.expensemanager.dto.WasteAnalysisDto;
import com.expensemanager.entity.SalaryAllocation;
import com.expensemanager.entity.Subscription;
import com.expensemanager.entity.Transaction;
import com.expensemanager.entity.User;
import com.expensemanager.repository.SalaryAllocationRepository;
import com.expensemanager.repository.SubscriptionRepository;
import com.expensemanager.repository.TransactionRepository;
import com.expensemanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepo;
    @Mock
    private TransactionRepository transactionRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private SalaryAllocationRepository salaryRepo;

    @InjectMocks
    private SubscriptionService service;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("test@test.com").name("Test").build();
    }

    @Test
    @DisplayName("Create subscription returns saved DTO")
    void createSubscription() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(subscriptionRepo.save(any())).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        SubscriptionDto dto = SubscriptionDto.builder()
                .merchant("Netflix").amount(new BigDecimal("699"))
                .category("Entertainment").interval("MONTHLY")
                .nextBillingDate(LocalDate.of(2026, 3, 1))
                .autoGenerateTransaction(true).active(true).build();

        SubscriptionDto result = service.createSubscription(1L, dto);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getMerchant()).isEqualTo("Netflix");
        assertThat(result.getAutoGenerateTransaction()).isTrue();
    }

    @Test
    @DisplayName("Summary calculates monthly and yearly totals")
    void summaryCalcTotals() {
        List<Subscription> active = List.of(
                Subscription.builder().id(1L).user(testUser).merchant("Netflix")
                        .amount(new BigDecimal("699")).interval("MONTHLY")
                        .active(true).nextBillingDate(LocalDate.now().plusDays(3)).build(),
                Subscription.builder().id(2L).user(testUser).merchant("Gym")
                        .amount(new BigDecimal("2000")).interval("MONTHLY")
                        .active(true).nextBillingDate(LocalDate.now().plusDays(10)).build());

        when(subscriptionRepo.findByUserIdAndActiveTrue(1L)).thenReturn(active);

        SubscriptionSummaryDto summary = service.getSummary(1L);

        assertThat(summary.getActiveSubscriptions()).isEqualTo(2);
        assertThat(summary.getTotalMonthlyCommitment()).isEqualByComparingTo("2699.00");
        assertThat(summary.getTotalYearlyCommitment()).isEqualByComparingTo("32388.00");
        assertThat(summary.getUpcomingIn7Days()).hasSize(1); // only Netflix (3 days)
    }

    @Test
    @DisplayName("Auto-billing creates transaction and advances date")
    void autoBilling() {
        LocalDate today = LocalDate.now();
        Subscription sub = Subscription.builder()
                .id(1L).user(testUser).merchant("Netflix")
                .amount(new BigDecimal("699")).category("Entertainment")
                .interval("MONTHLY").nextBillingDate(today)
                .autoGenerateTransaction(true).active(true).build();

        when(subscriptionRepo.findByActiveTrueAndAutoGenerateTransactionTrueAndNextBillingDate(today))
                .thenReturn(List.of(sub));
        when(transactionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(subscriptionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processAutoBilling();

        verify(transactionRepo).save(argThat(txn -> txn.getMerchant().equals("Netflix") &&
                txn.getAmount().compareTo(new BigDecimal("699")) == 0 &&
                txn.getCategory().equals("Entertainment")));

        verify(subscriptionRepo).save(argThat(s -> s.getLastBilledDate().equals(today) &&
                s.getNextBillingDate().equals(today.plusMonths(1))));
    }

    @Test
    @DisplayName("Waste detection finds duplicate merchants")
    void wasteDetectsDuplicates() {
        List<Subscription> active = List.of(
                Subscription.builder().id(1L).user(testUser).merchant("Netflix")
                        .amount(new BigDecimal("699")).interval("MONTHLY").active(true).build(),
                Subscription.builder().id(2L).user(testUser).merchant("netflix")
                        .amount(new BigDecimal("999")).interval("MONTHLY").active(true).build());

        when(subscriptionRepo.findByUserIdAndActiveTrue(1L)).thenReturn(active);
        when(salaryRepo.findByUserIdAndMonthAndYear(eq(1L), anyInt(), anyInt())).thenReturn(List.of());

        WasteAnalysisDto result = service.analyzeWaste(1L);

        assertThat(result.getWarnings())
                .anyMatch(w -> w.contains("2") && w.contains("netflix"));
    }

    @Test
    @DisplayName("Waste detection warns when subscriptions exceed 30% of salary")
    void wasteDetectsHighBurden() {
        List<Subscription> active = List.of(
                Subscription.builder().id(1L).user(testUser).merchant("Sub1")
                        .amount(new BigDecimal("20000")).interval("MONTHLY").active(true).build());

        SalaryAllocation salaryAlloc = SalaryAllocation.builder()
                .totalSalary(new BigDecimal("50000")).build();

        when(subscriptionRepo.findByUserIdAndActiveTrue(1L)).thenReturn(active);
        when(salaryRepo.findByUserIdAndMonthAndYear(eq(1L), anyInt(), anyInt()))
                .thenReturn(List.of(salaryAlloc));

        WasteAnalysisDto result = service.analyzeWaste(1L);

        assertThat(result.getWarnings())
                .anyMatch(w -> w.contains("exceed") && w.contains("salary"));
    }

    @Test
    @DisplayName("Waste detection flags unused subscriptions")
    void wasteDetectsUnused() {
        List<Subscription> active = List.of(
                Subscription.builder().id(1L).user(testUser).merchant("Old Gym")
                        .amount(new BigDecimal("2000")).interval("MONTHLY")
                        .lastBilledDate(LocalDate.now().minusDays(90))
                        .active(true).build());

        when(subscriptionRepo.findByUserIdAndActiveTrue(1L)).thenReturn(active);
        when(salaryRepo.findByUserIdAndMonthAndYear(eq(1L), anyInt(), anyInt())).thenReturn(List.of());

        WasteAnalysisDto result = service.analyzeWaste(1L);

        assertThat(result.getWarnings())
                .anyMatch(w -> w.contains("Old Gym") && w.contains("90 days"));
    }
}
