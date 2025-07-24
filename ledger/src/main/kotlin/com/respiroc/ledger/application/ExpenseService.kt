package com.respiroc.ledger.application

import com.respiroc.ledger.application.payload.CreateExpensePayload
import com.respiroc.ledger.application.payload.CreatePostingPayload
import com.respiroc.ledger.application.payload.CreateVoucherPayload
import com.respiroc.ledger.application.payload.ExpensePayload
import com.respiroc.ledger.domain.model.Expense
import com.respiroc.ledger.domain.model.ExpenseCategory
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
    private val voucherService: VoucherService  // Add this injection
) : ContextAwareApi {

    @Transactional(readOnly = true)
    fun findAllExpensesByTenant(): List<ExpensePayload> {
        val expenses = expenseRepository.findByTenantId(tenantId())
        return expenses.map { expense -> toExpensePayload(expense) }
    }

    fun findExpensesByDate(startDate: LocalDate, endDate: LocalDate): List<ExpensePayload> {
        val expenses = expenseRepository.findByTenantIdAndDateRange(tenantId(), startDate, endDate)
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

        // Create voucher with postings for proper accounting
        createVoucherForExpense(savedExpense)

        return toExpensePayload(savedExpense)
    }

    private fun createExpenseEntity(payload: CreateExpensePayload, createdBy: String): Expense {
        val expense = Expense()
        expense.tenantId = tenantId()
        expense.categoryId = payload.categoryId
        expense.amount = payload.amount
        expense.description = payload.description
        expense.expenseDate = payload.expenseDate
        expense.receiptPath = payload.receiptPath
        expense.createdBy = createdBy
        expense.accountNumber = mapCategoryToAccount(payload.categoryId)
        return expense
    }

    private fun createVoucherForExpense(expense: Expense) {
        val voucherPayload = CreateVoucherPayload(
            date = expense.expenseDate,
            description = "Expense: ${expense.description}",
            postings = listOf(
                // Debit expense account
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
                // Credit bank account (cash paid)
                CreatePostingPayload(
                    accountNumber = "1920", // Bank deposits
                    amount = expense.amount.negate(),
                    currency = "NOK",
                    postingDate = expense.expenseDate,
                    description = "Payment: ${expense.description}",
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

        // Verify account exists in chart of accounts
        val account = accountService.findAccountByNumber(accountNumber)
        return if (account != null) {
            accountNumber
        } else {
            "7790" // Fallback to miscellaneous if account not found
        }
    }

    private fun toExpensePayload(expense: Expense): ExpensePayload {
        val category = expenseCategoryRepository.findById(expense.categoryId).orElse(null)
        return ExpensePayload(
            id = expense.id,
            categoryId = expense.categoryId,
            categoryName = category?.name ?: "Unknown",
            amount = expense.amount,
            description = expense.description,
            expenseDate = expense.expenseDate,
            receiptPath = expense.receiptPath,
            createdBy = expense.createdBy
        )
    }
}