package com.expensemanager.service;

import com.expensemanager.entity.User;
import com.expensemanager.exception.ResourceNotFoundException;
import com.expensemanager.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GdprService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AccountRepository accountRepository;

    public Map<String, Object> exportUserData(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Map<String, Object> data = new HashMap<>();
        data.put("user", Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""));
        data.put("accounts", accountRepository.findByUserId(userId));
        data.put("transactions", transactionRepository.findByUserIdAndTransactionDateBetween(
                userId,
                java.time.LocalDate.of(2000, 1, 1),
                java.time.LocalDate.of(2099, 12, 31)));
        data.put("budgets", budgetRepository.findByUserId(userId));
        data.put("subscriptions", subscriptionRepository.findByUserId(userId));

        log.info("Exported all data for user {}", userId);
        return data;
    }

    @Transactional
    public void deleteUserData(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        subscriptionRepository.deleteAllByUserId(userId);
        budgetRepository.deleteAllByUserId(userId);
        transactionRepository.deleteAllByUserId(userId);
        accountRepository.findByUserId(userId).forEach(a -> accountRepository.delete(a));
        userRepository.delete(user);

        log.info("Deleted all data for user {}", userId);
    }
}
