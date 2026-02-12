package com.expensemanager.service;

import com.expensemanager.dto.TransactionDto;
import com.expensemanager.entity.Transaction;
import com.expensemanager.entity.User;
import com.expensemanager.exception.ResourceNotFoundException;
import com.expensemanager.repository.AccountRepository;
import com.expensemanager.repository.TransactionRepository;
import com.expensemanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountRepository accountRepository;
    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).name("Test").email("test@test.com").build();
        testTransaction = Transaction.builder()
                .id(1L)
                .user(testUser)
                .amount(BigDecimal.valueOf(45.99))
                .currency("USD")
                .merchant("Grocery Store")
                .category("Food & Groceries")
                .transactionDate(LocalDate.of(2026, 1, 15))
                .notes("Weekly groceries")
                .build();
    }

    @Test
    void getTransactions_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Transaction> page = new PageImpl<>(List.of(testTransaction));

        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<TransactionDto> result = transactionService.getTransactions(
                1L, null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Food & Groceries", result.getContent().get(0).getCategory());
    }

    @Test
    void createTransaction_Success() {
        TransactionDto dto = TransactionDto.builder()
                .amount(BigDecimal.valueOf(100))
                .category("Shopping")
                .transactionDate(LocalDate.now())
                .merchant("Amazon")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(
                Transaction.builder()
                        .id(2L)
                        .user(testUser)
                        .amount(dto.getAmount())
                        .currency("USD")
                        .merchant(dto.getMerchant())
                        .category(dto.getCategory())
                        .transactionDate(dto.getTransactionDate())
                        .build());

        TransactionDto result = transactionService.createTransaction(1L, dto);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(100), result.getAmount());
        assertEquals("Shopping", result.getCategory());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void deleteTransaction_Success() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));

        assertDoesNotThrow(() -> transactionService.deleteTransaction(1L, 1L));
        verify(transactionRepository).delete(testTransaction);
    }

    @Test
    void deleteTransaction_NotFound_ThrowsException() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.deleteTransaction(1L, 99L));
    }

    @Test
    void deleteTransaction_WrongUser_ThrowsException() {
        User otherUser = User.builder().id(2L).build();
        Transaction otherTxn = Transaction.builder().id(1L).user(otherUser).build();

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(otherTxn));

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.deleteTransaction(1L, 1L));
    }
}
