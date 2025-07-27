package com.respiroc.ledger.application

import com.respiroc.ledger.application.payload.CostPayload
import com.respiroc.ledger.application.payload.CreateCostPayload
import com.respiroc.ledger.application.payload.CreateExpensePayload
import com.respiroc.ledger.application.payload.ExpenseAttachmentPayload
import com.respiroc.ledger.application.payload.ExpensePayload
import com.respiroc.ledger.domain.model.*
import com.respiroc.ledger.domain.repository.CostRepository
import com.respiroc.ledger.domain.repository.ExpenseCategoryRepository
import com.respiroc.ledger.domain.repository.ExpenseRepository
import com.respiroc.util.context.ContextAwareApi
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository,
    private val expenseCategoryRepository: ExpenseCategoryRepository,
    private val costRepository: CostRepository
) : ContextAwareApi {

    @Transactional(readOnly = true)
    fun findAllExpensesByTenant(): List<ExpensePayload> {
        val expenses = expenseRepository.findByTenantIdWithCosts(tenantId())
        return expenses.map { expense ->
            val attachmentCount = expenseRepository.findByIdWithAttachments(expense.id)?.attachments?.size ?: 0
            toExpensePayload(expense, attachmentCount)
        }
    }

    @Transactional(readOnly = true)
    fun findExpensesWithFilters(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        status: ExpenseStatus? = null
    ): List<ExpensePayload> {
        val effectiveStartDate = startDate ?: LocalDate.now().withDayOfMonth(1)
        val effectiveEndDate = endDate ?: LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth())
        
        val expenses = when {
            status != null -> expenseRepository.findByTenantIdAndDateRangeAndStatusWithCosts(tenantId(), effectiveStartDate, effectiveEndDate, status)
            else -> expenseRepository.findByTenantIdAndDateRangeWithCosts(tenantId(), effectiveStartDate, effectiveEndDate)
        }
        
        return expenses.map { expense ->
            val attachmentCount = expenseRepository.findByIdWithAttachments(expense.id)?.attachments?.size ?: 0
            toExpensePayload(expense, attachmentCount)
        }
    }

    @Transactional(readOnly = true)
    fun findExpensesByDate(startDate: LocalDate, endDate: LocalDate): List<ExpensePayload> {
        return findExpensesWithFilters(startDate = startDate, endDate = endDate)
    }

    @Transactional(readOnly = true)
    fun findAllActiveCategories(): List<ExpenseCategory> {
        return expenseCategoryRepository.findAllActive()
    }

    @Transactional(readOnly = true)
    fun getTotalExpenseAmount(): BigDecimal {
        return expenseRepository.getTotalAmountByTenantId(tenantId())
    }

    @Transactional
    fun createExpense(payload: CreateExpensePayload, createdBy: String): ExpensePayload {
        val expense = createExpenseEntity(payload, createdBy)
        val savedExpense = expenseRepository.save(expense)
        createVoucherForExpense(savedExpense)
        return toExpensePayload(savedExpense)
    }

    @Transactional
    fun updateExpense(id: Long, payload: CreateExpensePayload): ExpensePayload {
        val expense = expenseRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Expense not found") }
        
        expense.title = payload.title
        expense.description = payload.description
        expense.expenseDate = payload.expenseDate
        expense.categoryId = payload.categoryId
        expense.accountNumber = mapCategoryToAccount(payload.categoryId)
        expense.amount = payload.costs.sumOf { it.amount }
        
        expenseRepository.deleteCostsByExpenseId(id)
        
        val costs = payload.costs.map { costPayload: CreateCostPayload ->
            val cost = Cost()
            cost.title = costPayload.title
            cost.date = costPayload.date
            cost.amount = costPayload.amount
            cost.vat = costPayload.vat
            cost.currency = costPayload.currency
            cost.paymentType = costPayload.paymentType
            cost.chargeable = costPayload.chargeable
            cost.expense = expense
            cost
        }
        
        val savedExpense = expenseRepository.save(expense)
        costRepository.saveAll(costs)
        
        val attachmentCount = expenseRepository.findByIdWithAttachments(savedExpense.id!!)?.attachments?.size ?: 0
        return toExpensePayload(savedExpense, attachmentCount)
    }

    @Transactional
    fun updateExpenseStatus(id: Long): ExpensePayload {
        expenseRepository.updateExpenseStatus(id)
        val expense = expenseRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Expense not found") }
        val attachmentCount = expenseRepository.findByIdWithAttachments(id)?.attachments?.size ?: 0
        return toExpensePayload(expense, attachmentCount)
    }

    @Transactional
    fun deleteExpense(id: Long) {
        val expense = expenseRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Expense not found") }
        expenseRepository.delete(expense)
    }

    @Transactional(readOnly = true)
    fun findExpenseByIdForEdit(id: Long): ExpensePayload? {
        val expense = expenseRepository.findByIdWithCosts(id) ?: return null
        val attachmentCount = expenseRepository.findByIdWithAttachments(id)?.attachments?.size ?: 0
        return toExpensePayload(expense, attachmentCount)
    }

    @Transactional
    fun addAttachmentsToExpense(expenseId: Long, attachments: List<ByteArray>, filenames: List<String>): ExpensePayload {
        val expense = expenseRepository.findByIdWithAttachments(expenseId)
            ?: throw IllegalArgumentException("Expense not found")
        
        for (i in attachments.indices) {
            val attachment = ExpenseAttachment()
            attachment.fileData = attachments[i]
            attachment.filename = if (i < filenames.size) filenames[i] else "attachment_${System.currentTimeMillis()}.pdf"
            attachment.expense = expense
            expense.attachments.add(attachment)
        }
        
        val savedExpense = expenseRepository.save(expense)
        return toExpensePayload(savedExpense)
    }

    @Transactional(readOnly = true)
    fun findExpenseById(id: Long): Expense? {
        return expenseRepository.findByIdWithAttachments(id)
    }

    @Transactional(readOnly = true)
    fun findExpenseAttachments(expenseId: Long): List<ExpenseAttachmentPayload> {
        val expense = expenseRepository.findByIdWithAttachments(expenseId)
            ?: return emptyList()
        
        return expense.attachments.map { attachment: ExpenseAttachment ->
            ExpenseAttachmentPayload(
                id = attachment.id,
                filename = attachment.filename
            )
        }
    }

    private fun createExpenseEntity(payload: CreateExpensePayload, createdBy: String): Expense {
        val expense = Expense()
        expense.title = payload.title
        expense.description = payload.description
        expense.expenseDate = payload.expenseDate
        expense.categoryId = payload.categoryId
        expense.accountNumber = mapCategoryToAccount(payload.categoryId)
        expense.amount = payload.costs.sumOf { it.amount }
        expense.createdBy = createdBy
        expense.tenantId = tenantId()

        val costs = payload.costs.map { costPayload: CreateCostPayload ->
            val cost = Cost()
            cost.title = costPayload.title
            cost.date = costPayload.date
            cost.amount = costPayload.amount
            cost.vat = costPayload.vat
            cost.currency = costPayload.currency
            cost.paymentType = costPayload.paymentType
            cost.chargeable = costPayload.chargeable
            cost.expense = expense
            cost
        }
        
        expense.costs.addAll(costs)
        return expense
    }

    private fun createVoucherForExpense(expense: Expense) {

    }

    private fun mapCategoryToAccount(categoryId: Long): String {
        return when (categoryId) {
            1L -> "7140" // Travel
            2L -> "7350" // Meals & Entertainment
            3L -> "6560" // Office Supplies
            4L -> "7320" // Marketing
            5L -> "6720" // Professional Services
            6L -> "6200" // Utilities
            else -> "7790" // Default
        }
    }

    private fun toExpensePayload(expense: Expense, attachmentCount: Int = 0): ExpensePayload {
        val category = expenseCategoryRepository.findById(expense.categoryId).orElse(null)
        val costPayloads = expense.costs.map { cost: Cost ->
            CostPayload(
                id = cost.id,
                title = cost.title,
                date = cost.date,
                amount = cost.amount,
                vat = cost.vat,
                currency = cost.currency,
                paymentType = cost.paymentType,
                chargeable = cost.chargeable
            )
        }

        return ExpensePayload(
            id = expense.id,
            title = expense.title,
            description = expense.description,
            expenseDate = expense.expenseDate,
            status = expense.status,
            category = category?.name ?: "Unknown",
            amount = expense.amount,
            costs = costPayloads,
            receiptPath = expense.receiptPath,
            createdBy = expense.createdBy,
            attachmentCount = attachmentCount
        )
    }
}