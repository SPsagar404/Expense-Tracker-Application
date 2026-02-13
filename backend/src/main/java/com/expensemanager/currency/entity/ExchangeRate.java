package com.expensemanager.currency.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_rates",
        uniqueConstraints = @UniqueConstraint(columnNames = { "base_currency", "target_currency" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_currency", nullable = false, length = 10)
    private String baseCurrency;

    @Column(name = "target_currency", nullable = false, length = 10)
    private String targetCurrency;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal rate;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}

