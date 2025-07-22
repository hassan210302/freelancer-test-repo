package com.respiroc.ledger.application.payload

import java.math.BigDecimal
import java.time.LocalDate

data class CreateExpensePayload(
    val categoryId: Long,
    val amount: BigDecimal,
    val description: String,
    val expenseDate: LocalDate,
    val receiptPath: String? = null
)