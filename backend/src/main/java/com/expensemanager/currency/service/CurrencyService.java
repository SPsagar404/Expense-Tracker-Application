package com.expensemanager.currency.service;

import com.expensemanager.currency.dto.ExchangeRateDto;
import com.expensemanager.currency.repository.ExchangeRateRepository;
import com.expensemanager.entity.User;
import com.expensemanager.exception.ResourceNotFoundException;
import com.expensemanager.currency.entity.ExchangeRate;
import com.expensemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final UserRepository userRepository;

    @Value("${app.currency.base:INR}")
    private String baseCurrency;

    @Value("${app.currency.supported:INR,USD,EUR,GBP,AED}")
    private String supportedCurrenciesConfig;

    @Value("${app.currency.api-url:https://api.exchangerate.host/latest}")
    private String apiUrl;

    public List<String> getSupportedCurrencies() {
        return Arrays.stream(supportedCurrenciesConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Transactional
    public void updateUserPreferredCurrency(Long userId, String currency) {
        String upper = currency.toUpperCase(Locale.ROOT);
        if (!getSupportedCurrencies().contains(upper)) {
            throw new IllegalArgumentException("Unsupported currency: " + currency);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setPreferredCurrency(upper);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public String getUserPreferredCurrency(Long userId) {
        return userRepository.findById(userId)
                .map(User::getPreferredCurrency)
                .orElse(baseCurrency);
    }

    @Transactional(readOnly = true)
    public List<ExchangeRateDto> getRatesForBase(String base) {
        String baseCode = base == null || base.isBlank() ? baseCurrency : base.toUpperCase(Locale.ROOT);
        List<ExchangeRate> rates = exchangeRateRepository.findByBaseCurrency(baseCode);
        if (rates.isEmpty()) {
            // Always include 1:1 for base currency
            return List.of(ExchangeRateDto.builder()
                    .baseCurrency(baseCode)
                    .targetCurrency(baseCode)
                    .rate(BigDecimal.ONE)
                    .build());
        }
        List<ExchangeRateDto> dtos = new ArrayList<>();
        for (ExchangeRate r : rates) {
            dtos.add(ExchangeRateDto.builder()
                    .baseCurrency(r.getBaseCurrency())
                    .targetCurrency(r.getTargetCurrency())
                    .rate(r.getRate())
                    .build());
        }
        return dtos;
    }

    @Transactional(readOnly = true)
    public BigDecimal convertAmount(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) {
            return null;
        }
        String from = fromCurrency.toUpperCase(Locale.ROOT);
        String to = toCurrency.toUpperCase(Locale.ROOT);

        if (from.equals(to)) {
            return amount;
        }

        Optional<ExchangeRate> direct = exchangeRateRepository.findByBaseCurrencyAndTargetCurrency(from, to);
        if (direct.isPresent()) {
            return amount.multiply(direct.get().getRate()).setScale(2, RoundingMode.HALF_UP);
        }

        // Fallback: try via configured base currency (e.g. INR)
        Optional<ExchangeRate> toBase = exchangeRateRepository.findByBaseCurrencyAndTargetCurrency(from, baseCurrency);
        Optional<ExchangeRate> fromBase = exchangeRateRepository.findByBaseCurrencyAndTargetCurrency(baseCurrency, to);
        if (toBase.isPresent() && fromBase.isPresent()) {
            BigDecimal inBase = amount.multiply(toBase.get().getRate());
            return inBase.multiply(fromBase.get().getRate()).setScale(2, RoundingMode.HALF_UP);
        }

        // As a last resort, return the original amount
        return amount;
    }

    @Transactional(readOnly = true)
    public BigDecimal convertForUser(BigDecimal amount, Long userId, String storedCurrency) {
        String preferred = getUserPreferredCurrency(userId);
        return convertAmount(amount, storedCurrency, preferred);
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void refreshExchangeRates() {
        try {
            List<String> targets = getSupportedCurrencies();
            targets.removeIf(c -> c.equalsIgnoreCase(baseCurrency));
            if (targets.isEmpty()) {
                return;
            }

            String symbols = String.join(",", targets);
            String url = apiUrl + "?base=" + baseCurrency + "&symbols=" + symbols;

            RestTemplate restTemplate = new RestTemplate();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || !response.containsKey("rates")) {
                log.warn("Currency API did not return rates for {}", baseCurrency);
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rates = (Map<String, Object>) response.get("rates");
            for (String target : targets) {
                Object value = rates.get(target);
                if (value instanceof Number number) {
                    BigDecimal rate = BigDecimal.valueOf(number.doubleValue());
                    upsertRate(baseCurrency, target, rate);
                    // Also store inverse for convenience
                    if (rate.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal inverse = BigDecimal.ONE.divide(rate, 6, RoundingMode.HALF_UP);
                        upsertRate(target, baseCurrency, inverse);
                    }
                }
            }

            // Always ensure base-to-base is 1
            upsertRate(baseCurrency, baseCurrency, BigDecimal.ONE);

            log.info("Exchange rates refreshed for base {}", baseCurrency);
        } catch (Exception ex) {
            log.error("Failed to refresh exchange rates: {}", ex.getMessage());
        }
    }

    private void upsertRate(String base, String target, BigDecimal rate) {
        ExchangeRate existing = exchangeRateRepository
                .findByBaseCurrencyAndTargetCurrency(base, target)
                .orElseGet(() -> ExchangeRate.builder()
                        .baseCurrency(base)
                        .targetCurrency(target)
                        .build());
        existing.setRate(rate);
        exchangeRateRepository.save(existing);
    }
}

