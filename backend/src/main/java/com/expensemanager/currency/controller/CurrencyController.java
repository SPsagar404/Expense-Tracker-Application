package com.expensemanager.currency.controller;

import com.expensemanager.currency.dto.ExchangeRateDto;
import com.expensemanager.currency.dto.UpdateCurrencyRequest;
import com.expensemanager.currency.service.CurrencyService;
import com.expensemanager.dto.ApiResponse;
import com.expensemanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Currency", description = "Global currency management and exchange rates")
public class CurrencyController {

    private final CurrencyService currencyService;

    @GetMapping("/currency/supported")
    @Operation(summary = "Get list of supported currencies")
    public ResponseEntity<ApiResponse<List<String>>> getSupportedCurrencies() {
        return ResponseEntity.ok(ApiResponse.success(currencyService.getSupportedCurrencies()));
    }

    @GetMapping("/currency/rates")
    @Operation(summary = "Get exchange rates for a base currency")
    public ResponseEntity<ApiResponse<List<ExchangeRateDto>>> getRates(
            @RequestParam(name = "base", required = false) String base) {
        return ResponseEntity.ok(ApiResponse.success(currencyService.getRatesForBase(base)));
    }

    @PutMapping("/users/currency")
    @Operation(summary = "Update current user's preferred currency")
    public ResponseEntity<ApiResponse<Void>> updateUserCurrency(
            @Valid @RequestBody UpdateCurrencyRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        currencyService.updateUserPreferredCurrency(userId, request.getPreferredCurrency());
        return ResponseEntity.ok(ApiResponse.success("Currency preference updated", null));
    }
}

