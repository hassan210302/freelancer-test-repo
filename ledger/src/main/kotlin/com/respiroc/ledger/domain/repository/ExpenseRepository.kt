package com.respiroc.ledger.domain.repository

import com.respiroc.ledger.domain.model.Expense
import com.respiroc.ledger.domain.model.ExpenseStatus
import com.respiroc.util.repository.CustomJpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
interface ExpenseRepository : CustomJpaRepository<Expense, Long> {

    @Query("SELECT e FROM Expense e ORDER BY e.expenseDate DESC")
    fun findAllOrderedByDate(): List<Expense>

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.costs ORDER BY e.expenseDate DESC") 
    fun findAllWithCosts(): List<Expense>

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.attachments ORDER BY e.expenseDate DESC")
    fun findAllWithAttachments(): List<Expense>

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e")
    fun getTotalAmount(): BigDecimal

    @Query("SELECT e FROM Expense e WHERE e.expenseDate BETWEEN :startDate AND :endDate ORDER BY e.expenseDate DESC")
    fun findByDateRange(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<Expense>

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.costs WHERE e.expenseDate BETWEEN :startDate AND :endDate ORDER BY e.expenseDate DESC")
    fun findByDateRangeWithCosts(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<Expense>

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.attachments WHERE e.expenseDate BETWEEN :startDate AND :endDate ORDER BY e.expenseDate DESC")
    fun findByDateRangeWithAttachments(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<Expense>

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.costs WHERE e.status = :status ORDER BY e.expenseDate DESC")
    fun findByStatusWithCosts(
        @Param("status") status: ExpenseStatus
    ): List<Expense>

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.costs WHERE e.expenseDate BETWEEN :startDate AND :endDate AND e.status = :status ORDER BY e.expenseDate DESC")
    fun findByDateRangeAndStatusWithCosts(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("status") status: ExpenseStatus
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