package com.expensemanager.service;

import com.expensemanager.dto.TransactionDto;
import com.expensemanager.entity.Account;
import com.expensemanager.entity.Transaction;
import com.expensemanager.entity.User;
import com.expensemanager.exception.CsvImportException;
import com.expensemanager.exception.ResourceNotFoundException;
import com.expensemanager.repository.AccountRepository;
import com.expensemanager.repository.TransactionRepository;
import com.expensemanager.repository.UserRepository;
import com.expensemanager.notification.service.NotificationService;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final NotificationService notificationService;

    public Page<TransactionDto> getTransactions(Long userId, String category,
            LocalDate startDate, LocalDate endDate,
            String merchant, Pageable pageable) {
        Specification<Transaction> spec = Specification.where(userIdEquals(userId));

        if (category != null && !category.isBlank()) {
            spec = spec.and(categoryEquals(category));
        }
        if (startDate != null) {
            spec = spec.and(dateAfterOrEqual(startDate));
        }
        if (endDate != null) {
            spec = spec.and(dateBeforeOrEqual(endDate));
        }
        if (merchant != null && !merchant.isBlank()) {
            spec = spec.and(merchantContains(merchant));
        }

        return transactionRepository.findAll(spec, pageable).map(this::toDto);
    }

    @Transactional
    public TransactionDto createTransaction(Long userId, TransactionDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Transaction transaction = Transaction.builder()
                .user(user)
                .amount(dto.getAmount())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "USD")
                .merchant(dto.getMerchant())
                .category(dto.getCategory())
                .transactionDate(dto.getTransactionDate())
                .notes(dto.getNotes())
                .build();

        if (dto.getAccountId() != null) {
            Account account = accountRepository.findById(dto.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account", dto.getAccountId()));
            transaction.setAccount(account);
        }

        transaction = transactionRepository.save(transaction);
        log.info("Transaction created: id={}, user={}, amount={}", transaction.getId(), userId, dto.getAmount());

        // Trigger notifications related to this transaction (budget, salary, large expense)
        notificationService.handleTransactionCreated(transaction);

        return toDto(transaction);
    }

    @Transactional
    public TransactionDto updateTransaction(Long userId, Long transactionId, TransactionDto dto) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Transaction", transactionId);
        }

        transaction.setAmount(dto.getAmount());
        transaction.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : transaction.getCurrency());
        transaction.setMerchant(dto.getMerchant());
        transaction.setCategory(dto.getCategory());
        transaction.setTransactionDate(dto.getTransactionDate());
        transaction.setNotes(dto.getNotes());

        transaction = transactionRepository.save(transaction);
        log.info("Transaction updated: id={}", transactionId);
        return toDto(transaction);
    }

    @Transactional
    public void deleteTransaction(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Transaction", transactionId);
        }

        transactionRepository.delete(transaction);
        log.info("Transaction deleted: id={}", transactionId);
    }

    @Transactional
    public List<TransactionDto> importCsv(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        List<Transaction> transactions = new ArrayList<>();
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
        };

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] header = reader.readNext(); // Skip header
            if (header == null) {
                throw new CsvImportException("CSV file is empty");
            }

            String[] line;
            int lineNum = 1;
            while ((line = reader.readNext()) != null) {
                lineNum++;
                if (line.length < 4) {
                    log.warn("Skipping invalid CSV line {}: insufficient columns", lineNum);
                    continue;
                }

                try {
                    BigDecimal amount = new BigDecimal(line[0].trim());
                    String merchant = line[1].trim();
                    String category = line[2].trim();
                    LocalDate date = parseDate(line[3].trim(), formatters);
                    String notes = line.length > 4 ? line[4].trim() : "";

                    Transaction txn = Transaction.builder()
                            .user(user)
                            .amount(amount)
                            .currency("USD")
                            .merchant(merchant)
                            .category(category)
                            .transactionDate(date)
                            .notes(notes)
                            .build();
                    transactions.add(txn);
                } catch (NumberFormatException | DateTimeParseException e) {
                    log.warn("Skipping invalid CSV line {}: {}", lineNum, e.getMessage());
                }
            }
        } catch (CsvValidationException | java.io.IOException e) {
            throw new CsvImportException("Failed to parse CSV file: " + e.getMessage(), e);
        }

        if (transactions.isEmpty()) {
            throw new CsvImportException("No valid transactions found in CSV file");
        }

        List<Transaction> saved = transactionRepository.saveAll(transactions);
        log.info("Imported {} transactions for user {}", saved.size(), userId);
        return saved.stream().map(this::toDto).toList();
    }

    private LocalDate parseDate(String dateStr, DateTimeFormatter[] formatters) {
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new DateTimeParseException("Unable to parse date: " + dateStr, dateStr, 0);
    }

    private TransactionDto toDto(Transaction t) {
        return TransactionDto.builder()
                .id(t.getId())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .merchant(t.getMerchant())
                .category(t.getCategory())
                .transactionDate(t.getTransactionDate())
                .notes(t.getNotes())
                .accountId(t.getAccount() != null ? t.getAccount().getId() : null)
                .build();
    }

    // Specification helpers
    private Specification<Transaction> userIdEquals(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    private Specification<Transaction> categoryEquals(String category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    private Specification<Transaction> dateAfterOrEqual(LocalDate start) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("transactionDate"), start);
    }

    private Specification<Transaction> dateBeforeOrEqual(LocalDate end) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("transactionDate"), end);
    }

    private Specification<Transaction> merchantContains(String merchant) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("merchant")), "%" + merchant.toLowerCase() + "%");
    }
}
