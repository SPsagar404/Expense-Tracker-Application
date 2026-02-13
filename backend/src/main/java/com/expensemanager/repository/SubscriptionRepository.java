package com.expensemanager.repository;

import com.expensemanager.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserId(Long userId);

    List<Subscription> findByUserIdAndActiveTrue(Long userId);

    List<Subscription> findByActiveTrueAndAutoGenerateTransactionTrueAndNextBillingDate(LocalDate date);

    List<Subscription> findByActiveTrueAndNextBillingDate(LocalDate date);

    List<Subscription> findByUserIdAndMerchantIgnoreCase(Long userId, String merchant);

    long countByUserIdAndActiveTrue(Long userId);

    void deleteAllByUserId(Long userId);
}
