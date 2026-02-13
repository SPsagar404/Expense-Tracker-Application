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
public class SubscriptionDto {
    private Long id;

    @NotBlank(message = "Merchant name is required")
    private String merchant;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String category;

    @NotBlank(message = "Interval is required (MONTHLY, WEEKLY, YEARLY)")
    private String interval;

    private LocalDate nextBillingDate;
    private LocalDate lastBilledDate;

    @Builder.Default
    private Boolean autoGenerateTransaction = false;

    @Builder.Default
    private Boolean active = true;
}
