package com.respiroc.ledger.application

import com.respiroc.ledger.application.payload.*
import com.respiroc.ledger.domain.model.Cost
import com.respiroc.ledger.domain.model.Expense
import com.respiroc.ledger.domain.model.ExpenseAttachment
import com.respiroc.ledger.domain.model.ExpenseCategory
import com.respiroc.ledger.domain.model.ExpenseStatus
import com.respiroc.ledger.domain.repository.ExpenseRepository
import com.respiroc.ledger.domain.repository.ExpenseCategoryRepository
import com.respiroc.ledger.domain.repository.CostRepository
import com.respiroc.util.context.ContextAwareApi
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
@Transactional
class ExpenseService(
    private val expenseRepository: ExpenseRepository,
    private val expenseCategoryRepository: ExpenseCategoryRepository,
    private val costRepository: CostRepository,
    private val accountService: AccountService,
    private val voucherService: VoucherService
) : ContextAwareApi {

    @Transactional(readOnly = true)
    fun findAllExpensesByTenant(): List<ExpensePayload> {
        val expenses = expenseRepository.findByTenantIdWithCostsAndAttachments(tenantId())
        return expenses.map { expense -> 
            // Access attachments within transaction to avoid lazy loading issues
            val attachmentCount = expense.attachments.size
            toExpensePayload(expense, attachmentCount)
        }
    }

    fun findExpensesByDate(startDate: LocalDate, endDate: LocalDate): List<ExpensePayload> {
        val expenses = expenseRepository.findByTenantIdAndDateRangeWithCostsAndAttachments(tenantId(), startDate, endDate)
        return expenses.map { expense -> 
            // Access attachments within transaction to avoid lazy loading issues
            val attachmentCount = expense.attachments.size
            toExpensePayload(expense, attachmentCount)
        }
    }

    @Transactional(readOnly = true)
    fun findAllActiveCategories(): List<ExpenseCategory> {
        return expenseCategoryRepository.findAllActive()
    }

    @Transactional(readOnly = true)
    fun getTotalExpenseAmount(): BigDecimal {
        return expenseRepository.getTotalAmountByTenantId(tenantId())
    }

    fun createExpense(payload: CreateExpensePayload, createdBy: String): ExpensePayload {
        val expense = createExpenseEntity(payload, createdBy)
        val savedExpense = expenseRepository.save(expense)
        createVoucherForExpense(savedExpense)
        return toExpensePayload(savedExpense)
    }

    fun updateExpense(id: Long, payload: CreateExpensePayload): ExpensePayload {
        val expense = expenseRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Expense not found") }
        
        // Update expense fields
        expense.title = payload.title
        expense.description = payload.description
        expense.expenseDate = payload.expenseDate
        expense.categoryId = payload.categoryId
        expense.accountNumber = mapCategoryToAccount(payload.categoryId)
        expense.amount = payload.costs.sumOf { it.amount }
        
        // Delete all existing costs for this expense
        expenseRepository.deleteCostsByExpenseId(id)
        
        // Create new costs
        val costs = payload.costs.map { costPayload ->
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
        
        // Save expense and costs
        val savedExpense = expenseRepository.save(expense)
        costRepository.saveAll(costs)
        
        val attachmentCount = expenseRepository.findByIdWithAttachments(savedExpense.id!!)?.attachments?.size ?: 0
        return toExpensePayload(savedExpense, attachmentCount)
    }

    fun deleteExpense(id: Long) {
        val expense = expenseRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Expense not found") }
        expenseRepository.delete(expense)
    }

    @Transactional(readOnly = true)
    fun findExpenseByIdForEdit(id: Long): ExpensePayload? {
        val expense = expenseRepository.findByIdWithCosts(id) ?: return null
        // Get attachment count separately since we can't fetch both collections at once
        val attachmentCount = expenseRepository.findByIdWithAttachments(id)?.attachments?.size ?: 0
        return toExpensePayload(expense, attachmentCount)
    }

    fun addAttachmentsToExpense(expenseId: Long, attachments: List<ByteArray>): ExpensePayload {
        val expense = expenseRepository.findByIdWithAttachments(expenseId)
            ?: throw IllegalArgumentException("Expense not found")
        
        attachments.forEach { fileData ->
            val attachment = ExpenseAttachment()
            attachment.fileData = fileData
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

    private fun createExpenseEntity(payload: CreateExpensePayload, createdBy: String): Expense {
        val expense = Expense()
        expense.tenantId = tenantId()
        expense.title = payload.title
        expense.description = payload.description
        expense.expenseDate = payload.expenseDate
        expense.status = ExpenseStatus.OPEN
        expense.categoryId = payload.categoryId
        expense.receiptPath = payload.receiptPath
        expense.createdBy = createdBy
        expense.accountNumber = mapCategoryToAccount(payload.categoryId)
        
        val costs = payload.costs.map { costPayload ->
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
        expense.amount = costs.sumOf { it.amount }
        
        return expense
    }

    private fun createVoucherForExpense(expense: Expense) {
        val voucherPayload = CreateVoucherPayload(
            date = expense.expenseDate,
            description = "Expense: ${expense.title}",
            postings = listOf(
                CreatePostingPayload(
                    accountNumber = expense.accountNumber ?: "7790",
                    amount = expense.amount,
                    currency = "NOK",
                    postingDate = expense.expenseDate,
                    description = expense.description,
                    originalAmount = expense.amount,
                    originalCurrency = "NOK",
                    vatCode = null,
                    rowNumber = 1
                ),
                CreatePostingPayload(
                    accountNumber = "1920",
                    amount = expense.amount.negate(),
                    currency = "NOK",
                    postingDate = expense.expenseDate,
                    description = "Payment: ${expense.title}",
                    originalAmount = expense.amount.negate(),
                    originalCurrency = "NOK",
                    vatCode = null,
                    rowNumber = 2
                )
            )
        )
        voucherService.createVoucher(voucherPayload)
    }

    private fun mapCategoryToAccount(categoryId: Long): String {
        val category = expenseCategoryRepository.findById(categoryId).orElse(null)
        val accountNumber = when (category?.name) {
            "Travel" -> "7140"
            "Meals & Entertainment" -> "7350"
            "Office Supplies" -> "6560"
            "Marketing" -> "7320"
            "Professional Services" -> "6720"
            "Utilities" -> "6200"
            else -> "7790"
        }
        val account = accountService.findAccountByNumber(accountNumber)
        return if (account != null) accountNumber else "7790"
    }

    private fun toExpensePayload(expense: Expense, attachmentCount: Int = 0): ExpensePayload {
        val category = expenseCategoryRepository.findById(expense.categoryId).orElse(null)
        val costPayloads = expense.costs.map { cost ->
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