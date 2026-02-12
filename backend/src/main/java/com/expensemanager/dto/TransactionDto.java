package com.expensemanager.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private Long id;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String currency;

    private String merchant;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    private String notes;

    private Long accountId;
}
