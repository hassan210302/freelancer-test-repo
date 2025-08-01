package com.respiroc.ledger.application.payload

import java.math.BigDecimal
import java.time.LocalDate

data class CreatePostingPayload(
    val accountNumber: String,
    val amount: BigDecimal,
    val currency: String,
    val postingDate: LocalDate,
    val description: String?,
    val originalAmount: BigDecimal? = null,
    val originalCurrency: String? = null,
    val vatCode: String? = null,
    val rowNumber: Int = 0,
    val supplierId: Long? = null
)
