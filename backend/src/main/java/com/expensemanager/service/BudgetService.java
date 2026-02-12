package com.expensemanager.service;

import com.expensemanager.dto.BudgetDto;
import com.expensemanager.entity.Budget;
import com.expensemanager.entity.User;
import com.expensemanager.exception.DuplicateResourceException;
import com.expensemanager.exception.ResourceNotFoundException;
import com.expensemanager.repository.BudgetRepository;
import com.expensemanager.repository.TransactionRepository;
import com.expensemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public List<BudgetDto> getBudgets(Long userId, Integer month, Integer year) {
        List<Budget> budgets;
        if (month != null && year != null) {
            budgets = budgetRepository.findByUserIdAndMonthAndYear(userId, month, year);
        } else {
            budgets = budgetRepository.findByUserId(userId);
        }
        return budgets.stream().map(b -> toDtoWithUtilization(b, userId)).toList();
    }

    @Transactional
    public BudgetDto createBudget(Long userId, BudgetDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Check for duplicate budget
        budgetRepository.findByUserIdAndCategoryAndMonthAndYear(
                userId, dto.getCategory(), dto.getMonth(), dto.getYear())
                .ifPresent(b -> {
                    throw new DuplicateResourceException(
                            "Budget already exists for " + dto.getCategory() +
                                    " in " + dto.getMonth() + "/" + dto.getYear());
                });

        Budget budget = Budget.builder()
                .user(user)
                .category(dto.getCategory())
                .month(dto.getMonth())
                .year(dto.getYear())
                .limitAmount(dto.getLimitAmount())
                .build();

        budget = budgetRepository.save(budget);
        log.info("Budget created: id={}, category={}, limit={}", budget.getId(), dto.getCategory(),
                dto.getLimitAmount());
        return toDtoWithUtilization(budget, userId);
    }

    @Transactional
    public BudgetDto updateBudget(Long userId, Long budgetId, BudgetDto dto) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", budgetId));

        if (!budget.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Budget", budgetId);
        }

        budget.setCategory(dto.getCategory());
        budget.setMonth(dto.getMonth());
        budget.setYear(dto.getYear());
        budget.setLimitAmount(dto.getLimitAmount());

        budget = budgetRepository.save(budget);
        log.info("Budget updated: id={}", budgetId);
        return toDtoWithUtilization(budget, userId);
    }

    @Transactional
    public void deleteBudget(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", budgetId));

        if (!budget.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Budget", budgetId);
        }

        budgetRepository.delete(budget);
        log.info("Budget deleted: id={}", budgetId);
    }

    private BudgetDto toDtoWithUtilization(Budget budget, Long userId) {
        LocalDate start = LocalDate.of(budget.getYear(), budget.getMonth(), 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        BigDecimal spent = transactionRepository.getTotalSpentByCategory(
                userId, budget.getCategory(), start, end);
        if (spent == null)
            spent = BigDecimal.ZERO;

        BigDecimal remaining = budget.getLimitAmount().subtract(spent);
        double utilization = budget.getLimitAmount().compareTo(BigDecimal.ZERO) > 0
                ? spent.divide(budget.getLimitAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        return BudgetDto.builder()
                .id(budget.getId())
                .category(budget.getCategory())
                .month(budget.getMonth())
                .year(budget.getYear())
                .limitAmount(budget.getLimitAmount())
                .spent(spent)
                .remaining(remaining)
                .utilizationPercentage(utilization)
                .build();
    }
}
