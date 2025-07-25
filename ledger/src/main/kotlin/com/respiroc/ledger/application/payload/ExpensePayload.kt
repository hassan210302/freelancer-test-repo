package com.respiroc.ledger.application.payload

import com.respiroc.ledger.domain.model.ExpenseStatus
import com.respiroc.ledger.domain.model.PaymentType
import java.math.BigDecimal
import java.time.LocalDate

data class CostPayload(
    val id: Long,
    val title: String,
    val date: LocalDate,
    val amount: BigDecimal,
    val vat: Int,
    val currency: String,
    val paymentType: PaymentType,
    val chargeable: Boolean
)

data class ExpensePayload(
    val id: Long,
    val title: String,
    val description: String,
    val expenseDate: LocalDate,
    val status: ExpenseStatus,
    val category: String,
    val amount: BigDecimal,
    val costs: List<CostPayload>,
    val receiptPath: String? = null,
    val createdBy: String,
    val attachmentCount: Int = 0
)