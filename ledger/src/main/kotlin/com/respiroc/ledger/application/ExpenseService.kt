package com.respiroc.ledger.application

import com.respiroc.ledger.application.payload.*
import com.respiroc.ledger.domain.model.Cost
import com.respiroc.ledger.domain.model.Expense
import com.respiroc.ledger.domain.model.ExpenseCategory
import com.respiroc.ledger.domain.model.ExpenseStatus
import com.respiroc.ledger.domain.repository.ExpenseRepository
import com.respiroc.ledger.domain.repository.ExpenseCategoryRepository
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
    private val accountService: AccountService,
    private val voucherService: VoucherService
) : ContextAwareApi {

    @Transactional(readOnly = true)
    fun findAllExpensesByTenant(): List<ExpensePayload> {
        val expenses = expenseRepository.findByTenantIdWithCosts(tenantId())
        return expenses.map { expense -> toExpensePayload(expense) }
    }

    fun findExpensesByDate(startDate: LocalDate, endDate: LocalDate): List<ExpensePayload> {
        val expenses = expenseRepository.findByTenantIdAndDateRangeWithCosts(tenantId(), startDate, endDate)
        return expenses.map { expense -> toExpensePayload(expense) }
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

    private fun toExpensePayload(expense: Expense): ExpensePayload {
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
            categoryId = expense.categoryId,
            categoryName = category?.name ?: "Unknown",
            project = "-",
            chargeable = expense.costs.any { it.chargeable },
            amount = expense.amount,
            costs = costPayloads,
            receiptPath = expense.receiptPath,
            createdBy = expense.createdBy
        )
    }
}