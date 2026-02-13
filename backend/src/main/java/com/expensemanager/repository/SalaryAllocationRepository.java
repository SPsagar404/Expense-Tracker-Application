package com.expensemanager.repository;

import com.expensemanager.entity.SalaryAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryAllocationRepository extends JpaRepository<SalaryAllocation, Long> {

    List<SalaryAllocation> findByUserIdAndMonthAndYear(Long userId, Integer month, Integer year);

    Optional<SalaryAllocation> findByUserIdAndMonthAndYearAndCategory(
            Long userId, Integer month, Integer year, String category);

    @Query("SELECT SUM(s.allocationValue) FROM SalaryAllocation s " +
            "WHERE s.user.id = :userId AND s.month = :month AND s.year = :year " +
            "AND s.allocationType = 'PERCENTAGE'")
    BigDecimal getTotalPercentageAllocated(
            @Param("userId") Long userId,
            @Param("month") Integer month,
            @Param("year") Integer year);

    @Query("SELECT SUM(s.allocatedAmount) FROM SalaryAllocation s " +
            "WHERE s.user.id = :userId AND s.month = :month AND s.year = :year")
    BigDecimal getTotalAllocatedAmount(
            @Param("userId") Long userId,
            @Param("month") Integer month,
            @Param("year") Integer year);

    void deleteAllByUserId(Long userId);
}
