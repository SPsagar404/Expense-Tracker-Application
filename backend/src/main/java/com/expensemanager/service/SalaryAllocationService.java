package com.expensemanager.service;

import com.expensemanager.dto.SalaryAllocationDto;
import com.expensemanager.dto.SalarySummaryDto;
import com.expensemanager.entity.SalaryAllocation;
import com.expensemanager.entity.User;
import com.expensemanager.exception.ResourceNotFoundException;
import com.expensemanager.repository.SalaryAllocationRepository;
import com.expensemanager.repository.TransactionRepository;
import com.expensemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalaryAllocationService {

    private final SalaryAllocationRepository salaryRepo;
    private final TransactionRepository transactionRepo;
    private final UserRepository userRepo;

    @Transactional
    public List<SalaryAllocationDto> saveSalaryPlan(Long userId, List<SalaryAllocationDto> dtos) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (dtos.isEmpty()) {
            throw new IllegalArgumentException("At least one allocation is required");
        }

        Integer month = dtos.get(0).getMonth();
        Integer year = dtos.get(0).getYear();
        BigDecimal totalSalary = dtos.get(0).getTotalSalary();

        // Validate totals
        validateAllocations(dtos, totalSalary);

        // Delete existing allocations for this period and replace
        List<SalaryAllocation> existing = salaryRepo.findByUserIdAndMonthAndYear(userId, month, year);
        if (!existing.isEmpty()) {
            salaryRepo.deleteAll(existing);
            salaryRepo.flush();
        }

        List<SalaryAllocation> saved = new ArrayList<>();
        for (SalaryAllocationDto dto : dtos) {
            SalaryAllocation.AllocationType type = SalaryAllocation.AllocationType.valueOf(dto.getAllocationType());
            BigDecimal allocatedAmount = calculateAllocatedAmount(type, dto.getAllocationValue(), totalSalary);

            SalaryAllocation allocation = SalaryAllocation.builder()
                    .user(user)
                    .month(month)
                    .year(year)
                    .totalSalary(totalSalary)
                    .category(dto.getCategory())
                    .allocationType(type)
                    .allocationValue(dto.getAllocationValue())
                    .allocatedAmount(allocatedAmount)
                    .build();

            saved.add(salaryRepo.save(allocation));
        }

        log.info("Saved {} salary allocations for user {} ({}/{})", saved.size(), userId, month, year);
        return saved.stream().map(this::toDto).toList();
    }

    @Transactional
    public SalaryAllocationDto addAllocation(Long userId, Long salaryId, SalaryAllocationDto dto) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        SalaryAllocation reference = salaryRepo.findById(salaryId)
                .orElseThrow(() -> new ResourceNotFoundException("SalaryAllocation", salaryId));

        if (!reference.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("SalaryAllocation", salaryId);
        }

        SalaryAllocation.AllocationType type = SalaryAllocation.AllocationType.valueOf(dto.getAllocationType());
        BigDecimal allocatedAmount = calculateAllocatedAmount(type, dto.getAllocationValue(),
                reference.getTotalSalary());

        // Check existing allocations + new one don't exceed limits
        BigDecimal currentTotalAllocated = salaryRepo.getTotalAllocatedAmount(
                userId, reference.getMonth(), reference.getYear());
        if (currentTotalAllocated == null)
            currentTotalAllocated = BigDecimal.ZERO;

        if (currentTotalAllocated.add(allocatedAmount).compareTo(reference.getTotalSalary()) > 0) {
            throw new IllegalArgumentException("Total allocations exceed salary. Available: " +
                    reference.getTotalSalary().subtract(currentTotalAllocated));
        }

        if (type == SalaryAllocation.AllocationType.PERCENTAGE) {
            BigDecimal currentPct = salaryRepo.getTotalPercentageAllocated(
                    userId, reference.getMonth(), reference.getYear());
            if (currentPct == null)
                currentPct = BigDecimal.ZERO;
            if (currentPct.add(dto.getAllocationValue()).compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Total percentage exceeds 100%. Current: " + currentPct + "%");
            }
        }

        SalaryAllocation allocation = SalaryAllocation.builder()
                .user(user)
                .month(reference.getMonth())
                .year(reference.getYear())
                .totalSalary(reference.getTotalSalary())
                .category(dto.getCategory())
                .allocationType(type)
                .allocationValue(dto.getAllocationValue())
                .allocatedAmount(allocatedAmount)
                .build();

        return toDto(salaryRepo.save(allocation));
    }

    @Transactional(readOnly = true)
    public SalarySummaryDto getSalarySummary(Long userId, Integer year, Integer month) {
        List<SalaryAllocation> allocations = salaryRepo.findByUserIdAndMonthAndYear(userId, month, year);

        if (allocations.isEmpty()) {
            return SalarySummaryDto.builder()
                    .month(month)
                    .year(year)
                    .totalSalary(BigDecimal.ZERO)
                    .totalPlanned(BigDecimal.ZERO)
                    .totalActualSpent(BigDecimal.ZERO)
                    .totalSavings(BigDecimal.ZERO)
                    .unallocatedAmount(BigDecimal.ZERO)
                    .allocations(List.of())
                    .build();
        }

        BigDecimal totalSalary = allocations.get(0).getTotalSalary();
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        BigDecimal totalPlanned = BigDecimal.ZERO;
        BigDecimal totalActual = BigDecimal.ZERO;
        List<SalarySummaryDto.CategoryAllocation> categoryAllocations = new ArrayList<>();

        for (SalaryAllocation alloc : allocations) {
            BigDecimal actualSpent = transactionRepo.getTotalSpentByCategory(
                    userId, alloc.getCategory(), startDate, endDate);
            if (actualSpent == null)
                actualSpent = BigDecimal.ZERO;

            BigDecimal variance = alloc.getAllocatedAmount().subtract(actualSpent);

            categoryAllocations.add(SalarySummaryDto.CategoryAllocation.builder()
                    .id(alloc.getId())
                    .category(alloc.getCategory())
                    .allocationType(alloc.getAllocationType().name())
                    .allocationValue(alloc.getAllocationValue())
                    .allocatedAmount(alloc.getAllocatedAmount())
                    .actualSpent(actualSpent)
                    .variance(variance)
                    .build());

            totalPlanned = totalPlanned.add(alloc.getAllocatedAmount());
            totalActual = totalActual.add(actualSpent);
        }

        return SalarySummaryDto.builder()
                .month(month)
                .year(year)
                .totalSalary(totalSalary)
                .totalPlanned(totalPlanned)
                .totalActualSpent(totalActual)
                .totalSavings(totalSalary.subtract(totalActual))
                .unallocatedAmount(totalSalary.subtract(totalPlanned))
                .allocations(categoryAllocations)
                .build();
    }

    @Transactional(readOnly = true)
    public List<SalaryAllocationDto> getAllocations(Long userId, Integer year, Integer month) {
        return salaryRepo.findByUserIdAndMonthAndYear(userId, month, year)
                .stream().map(this::toDto).toList();
    }

    // ---- helpers ----

    private void validateAllocations(List<SalaryAllocationDto> dtos, BigDecimal totalSalary) {
        BigDecimal totalPct = BigDecimal.ZERO;
        BigDecimal totalFixed = BigDecimal.ZERO;
        BigDecimal totalAllocated = BigDecimal.ZERO;

        for (SalaryAllocationDto dto : dtos) {
            SalaryAllocation.AllocationType type;
            try {
                type = SalaryAllocation.AllocationType.valueOf(dto.getAllocationType());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid allocation type: " + dto.getAllocationType());
            }

            BigDecimal allocated = calculateAllocatedAmount(type, dto.getAllocationValue(), totalSalary);
            totalAllocated = totalAllocated.add(allocated);

            if (type == SalaryAllocation.AllocationType.PERCENTAGE) {
                totalPct = totalPct.add(dto.getAllocationValue());
            } else {
                totalFixed = totalFixed.add(dto.getAllocationValue());
            }
        }

        if (totalPct.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Total percentage allocations exceed 100% (current: " + totalPct + "%)");
        }
        if (totalAllocated.compareTo(totalSalary) > 0) {
            throw new IllegalArgumentException(
                    "Total allocations (" + totalAllocated + ") exceed salary (" + totalSalary + ")");
        }
    }

    public static BigDecimal calculateAllocatedAmount(
            SalaryAllocation.AllocationType type, BigDecimal value, BigDecimal totalSalary) {
        if (type == SalaryAllocation.AllocationType.PERCENTAGE) {
            return totalSalary.multiply(value).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        return value;
    }

    private SalaryAllocationDto toDto(SalaryAllocation s) {
        return SalaryAllocationDto.builder()
                .id(s.getId())
                .month(s.getMonth())
                .year(s.getYear())
                .totalSalary(s.getTotalSalary())
                .category(s.getCategory())
                .allocationType(s.getAllocationType().name())
                .allocationValue(s.getAllocationValue())
                .allocatedAmount(s.getAllocatedAmount())
                .build();
    }
}
