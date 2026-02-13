package com.expensemanager.service;

import com.expensemanager.dto.SalaryAllocationDto;
import com.expensemanager.dto.SalarySummaryDto;
import com.expensemanager.entity.SalaryAllocation;
import com.expensemanager.entity.User;
import com.expensemanager.repository.SalaryAllocationRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalaryAllocationServiceTest {

    @Mock
    private SalaryAllocationRepository salaryRepo;
    @Mock
    private TransactionRepository transactionRepo;
    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private SalaryAllocationService service;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("test@test.com").name("Test").build();
    }

    @Test
    @DisplayName("Percentage allocation calculates correctly")
    void percentageAllocation() {
        BigDecimal result = SalaryAllocationService.calculateAllocatedAmount(
                SalaryAllocation.AllocationType.PERCENTAGE,
                new BigDecimal("25"),
                new BigDecimal("100000"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("25000.00"));
    }

    @Test
    @DisplayName("Fixed allocation returns value as-is")
    void fixedAllocation() {
        BigDecimal result = SalaryAllocationService.calculateAllocatedAmount(
                SalaryAllocation.AllocationType.FIXED,
                new BigDecimal("15000"),
                new BigDecimal("100000"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("15000"));
    }

    @Test
    @DisplayName("Saving salary plan replaces existing allocations")
    void saveSalaryPlan() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(salaryRepo.findByUserIdAndMonthAndYear(1L, 2, 2026)).thenReturn(List.of());
        when(salaryRepo.save(any())).thenAnswer(inv -> {
            SalaryAllocation sa = inv.getArgument(0);
            sa.setId(1L);
            return sa;
        });

        List<SalaryAllocationDto> dtos = List.of(
                SalaryAllocationDto.builder()
                        .month(2).year(2026).totalSalary(new BigDecimal("100000"))
                        .category("Rent").allocationType("FIXED")
                        .allocationValue(new BigDecimal("30000")).build(),
                SalaryAllocationDto.builder()
                        .month(2).year(2026).totalSalary(new BigDecimal("100000"))
                        .category("Food").allocationType("PERCENTAGE")
                        .allocationValue(new BigDecimal("20")).build());

        List<SalaryAllocationDto> result = service.saveSalaryPlan(1L, dtos);

        assertThat(result).hasSize(2);
        verify(salaryRepo, times(2)).save(any());
    }

    @Test
    @DisplayName("Percentage allocations exceeding 100% throw exception")
    void percentageExceeds100() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(salaryRepo.findByUserIdAndMonthAndYear(1L, 2, 2026)).thenReturn(List.of());

        List<SalaryAllocationDto> dtos = List.of(
                SalaryAllocationDto.builder()
                        .month(2).year(2026).totalSalary(new BigDecimal("100000"))
                        .category("Rent").allocationType("PERCENTAGE")
                        .allocationValue(new BigDecimal("60")).build(),
                SalaryAllocationDto.builder()
                        .month(2).year(2026).totalSalary(new BigDecimal("100000"))
                        .category("Food").allocationType("PERCENTAGE")
                        .allocationValue(new BigDecimal("50")).build());

        assertThatThrownBy(() -> service.saveSalaryPlan(1L, dtos))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceed 100%");
    }

    @Test
    @DisplayName("Total allocations exceeding salary throw exception")
    void allocationsExceedSalary() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(salaryRepo.findByUserIdAndMonthAndYear(1L, 2, 2026)).thenReturn(List.of());

        List<SalaryAllocationDto> dtos = List.of(
                SalaryAllocationDto.builder()
                        .month(2).year(2026).totalSalary(new BigDecimal("50000"))
                        .category("Rent").allocationType("FIXED")
                        .allocationValue(new BigDecimal("30000")).build(),
                SalaryAllocationDto.builder()
                        .month(2).year(2026).totalSalary(new BigDecimal("50000"))
                        .category("Food").allocationType("FIXED")
                        .allocationValue(new BigDecimal("25000")).build());

        assertThatThrownBy(() -> service.saveSalaryPlan(1L, dtos))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceed salary");
    }

    @Test
    @DisplayName("Salary summary includes variance per category")
    void salarySummaryWithVariance() {
        SalaryAllocation alloc = SalaryAllocation.builder()
                .id(1L).user(testUser).month(2).year(2026)
                .totalSalary(new BigDecimal("100000"))
                .category("Food").allocationType(SalaryAllocation.AllocationType.PERCENTAGE)
                .allocationValue(new BigDecimal("20"))
                .allocatedAmount(new BigDecimal("20000"))
                .build();

        when(salaryRepo.findByUserIdAndMonthAndYear(1L, 2, 2026)).thenReturn(List.of(alloc));
        when(transactionRepo.getTotalSpentByCategory(eq(1L), eq("Food"), any(), any()))
                .thenReturn(new BigDecimal("15000"));

        SalarySummaryDto summary = service.getSalarySummary(1L, 2026, 2);

        assertThat(summary.getTotalSalary()).isEqualByComparingTo("100000");
        assertThat(summary.getAllocations()).hasSize(1);

        SalarySummaryDto.CategoryAllocation catAlloc = summary.getAllocations().get(0);
        assertThat(catAlloc.getAllocatedAmount()).isEqualByComparingTo("20000");
        assertThat(catAlloc.getActualSpent()).isEqualByComparingTo("15000");
        assertThat(catAlloc.getVariance()).isEqualByComparingTo("5000"); // under budget
    }
}
