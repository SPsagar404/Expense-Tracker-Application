package com.expensemanager.repository;

import com.expensemanager.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    Page<Transaction> findByUserId(Long userId, Pageable pageable);

    List<Transaction> findByUserIdAndTransactionDateBetween(
            Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT t.category, SUM(t.amount) FROM Transaction t " +
            "WHERE t.user.id = :userId AND t.transactionDate BETWEEN :start AND :end " +
            "GROUP BY t.category")
    List<Object[]> getCategoryBreakdown(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("SELECT SUM(t.amount) FROM Transaction t " +
            "WHERE t.user.id = :userId AND t.transactionDate BETWEEN :start AND :end")
    BigDecimal getTotalSpent(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("SELECT SUM(t.amount) FROM Transaction t " +
            "WHERE t.user.id = :userId AND t.category = :category " +
            "AND t.transactionDate BETWEEN :start AND :end")
    BigDecimal getTotalSpentByCategory(
            @Param("userId") Long userId,
            @Param("category") String category,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    void deleteAllByUserId(Long userId);
}
