package com.expensemanager.service;

import com.expensemanager.dto.BudgetDto;
import com.expensemanager.entity.Budget;
import com.expensemanager.entity.User;
import com.expensemanager.exception.DuplicateResourceException;
import com.expensemanager.exception.ResourceNotFoundException;
import com.expensemanager.repository.BudgetRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private BudgetService budgetService;

    private User testUser;
    private Budget testBudget;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).name("Test").email("test@test.com").build();
        testBudget = Budget.builder()
                .id(1L)
                .user(testUser)
                .category("Food & Groceries")
                .month(1)
                .year(2026)
                .limitAmount(BigDecimal.valueOf(500))
                .build();
    }

    @Test
    void getBudgets_WithMonthYear_ReturnsList() {
        when(budgetRepository.findByUserIdAndMonthAndYear(1L, 1, 2026))
                .thenReturn(List.of(testBudget));
        when(transactionRepository.getTotalSpentByCategory(any(), any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(250));

        List<BudgetDto> result = budgetService.getBudgets(1L, 1, 2026);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(BigDecimal.valueOf(250), result.get(0).getSpent());
        assertEquals(50.0, result.get(0).getUtilizationPercentage(), 0.1);
    }

    @Test
    void createBudget_Success() {
        BudgetDto dto = BudgetDto.builder()
                .category("Food & Groceries")
                .month(2).year(2026)
                .limitAmount(BigDecimal.valueOf(600))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(budgetRepository.findByUserIdAndCategoryAndMonthAndYear(1L, "Food & Groceries", 2, 2026))
                .thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenReturn(
                Budget.builder().id(2L).user(testUser)
                        .category("Food & Groceries").month(2).year(2026)
                        .limitAmount(BigDecimal.valueOf(600)).build());
        when(transactionRepository.getTotalSpentByCategory(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        BudgetDto result = budgetService.createBudget(1L, dto);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(600), result.getLimitAmount());
    }

    @Test
    void createBudget_Duplicate_ThrowsException() {
        BudgetDto dto = BudgetDto.builder()
                .category("Food & Groceries")
                .month(1).year(2026)
                .limitAmount(BigDecimal.valueOf(500))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(budgetRepository.findByUserIdAndCategoryAndMonthAndYear(1L, "Food & Groceries", 1, 2026))
                .thenReturn(Optional.of(testBudget));

        assertThrows(DuplicateResourceException.class,
                () -> budgetService.createBudget(1L, dto));
    }

    @Test
    void deleteBudget_NotOwner_ThrowsException() {
        User otherUser = User.builder().id(2L).build();
        Budget otherBudget = Budget.builder().id(1L).user(otherUser).build();

        when(budgetRepository.findById(1L)).thenReturn(Optional.of(otherBudget));

        assertThrows(ResourceNotFoundException.class,
                () -> budgetService.deleteBudget(1L, 1L));
    }
}
