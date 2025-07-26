package com.respiroc.ledger.domain.repository

import com.respiroc.ledger.domain.model.Expense
import com.respiroc.util.repository.CustomJpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
interface ExpenseRepository : CustomJpaRepository<Expense, Long> {

    @Query("SELECT e FROM Expense e WHERE e.tenantId = :tenantId ORDER BY e.expenseDate DESC")
    fun findByTenantId(@Param("tenantId") tenantId: Long): List<Expense>

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.costs WHERE e.tenantId = :tenantId ORDER BY e.expenseDate DESC")
    fun findByTenantIdWithCosts(@Param("tenantId") tenantId: Long): List<Expense>

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.attachments WHERE e.tenantId = :tenantId ORDER BY e.expenseDate DESC")
    fun findByTenantIdWithAttachments(@Param("tenantId") tenantId: Long): List<Expense>

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.tenantId = :tenantId")
    fun getTotalAmountByTenantId(@Param("tenantId") tenantId: Long): BigDecimal

    @Query("SELECT e FROM Expense e WHERE e.tenantId = :tenantId AND e.expenseDate BETWEEN :startDate AND :endDate ORDER BY e.expenseDate DESC")
    fun findByTenantIdAndDateRange(
        @Param("tenantId") tenantId: Long,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<Expense>

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.costs WHERE e.tenantId = :tenantId AND e.expenseDate BETWEEN :startDate AND :endDate ORDER BY e.expenseDate DESC")
    fun findByTenantIdAndDateRangeWithCosts(
        @Param("tenantId") tenantId: Long,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<Expense>

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.attachments WHERE e.tenantId = :tenantId AND e.expenseDate BETWEEN :startDate AND :endDate ORDER BY e.expenseDate DESC")
    fun findByTenantIdAndDateRangeWithAttachments(
        @Param("tenantId") tenantId: Long,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<Expense>

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.attachments WHERE e.id = :id")
    fun findByIdWithAttachments(@Param("id") id: Long): Expense?

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.costs WHERE e.id = :id")
    fun findByIdWithCosts(@Param("id") id: Long): Expense?

    @Modifying
    @Query("DELETE FROM Cost c WHERE c.expense.id = :expenseId")
    fun deleteCostsByExpenseId(@Param("expenseId") expenseId: Long)

    @Modifying
    @Query("UPDATE Expense e SET e.status = CASE WHEN e.status = 'OPEN' THEN 'DELIVERED' WHEN e.status = 'DELIVERED' THEN 'APPROVED' ELSE e.status END WHERE e.id = :id")
    fun updateExpenseStatus(@Param("id") id: Long)
}