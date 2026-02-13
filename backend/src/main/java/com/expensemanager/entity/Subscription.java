package com.expensemanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String merchant;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 100)
    private String category;

    @Column(nullable = false, length = 50)
    private String interval; // MONTHLY, WEEKLY, YEARLY

    @Column(name = "next_billing_date")
    private LocalDate nextBillingDate;

    @Column(name = "last_billed_date")
    private LocalDate lastBilledDate;

    @Column(name = "auto_generate_transaction")
    @Builder.Default
    private Boolean autoGenerateTransaction = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
