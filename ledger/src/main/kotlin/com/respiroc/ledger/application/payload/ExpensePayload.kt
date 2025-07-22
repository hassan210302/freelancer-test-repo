package com.respiroc.ledger.application.payload

import java.math.BigDecimal
import java.time.LocalDate

data class ExpensePayload(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val amount: BigDecimal,
    val description: String,
    val expenseDate: LocalDate,
    val receiptPath: String?,
    val createdBy: String
)