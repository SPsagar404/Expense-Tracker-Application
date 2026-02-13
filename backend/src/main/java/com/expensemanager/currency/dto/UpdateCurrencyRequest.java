package com.expensemanager.currency.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCurrencyRequest {

    @NotBlank
    private String preferredCurrency;
}

