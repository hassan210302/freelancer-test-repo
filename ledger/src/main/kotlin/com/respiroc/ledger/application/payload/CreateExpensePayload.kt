package com.respiroc.ledger.application.payload

import com.respiroc.ledger.domain.model.PaymentType
import java.math.BigDecimal
import java.time.LocalDate

data class CreateCostPayload(
    val title: String,
    val date: LocalDate,
    val amount: BigDecimal,
    val vat: Int = 0,
    val currency: String = "NOK",
    val paymentType: PaymentType,
    val chargeable: Boolean = false
)

data class CreateExpensePayload(
    val title: String,
    val description: String,
    val expenseDate: LocalDate,
    val categoryId: Long,
    val costs: List<CreateCostPayload>,
    val receiptPath: String? = null
)