package com.respiroc.ledger.application.payload

import java.math.BigDecimal
import java.time.LocalDate

data class SupplierPostingDTO(
    val accountNumber: String,
    val description: String,
    val amount: BigDecimal,
    val postingDate: LocalDate,
    val currency: String
)

data class SupplierDTO(
    val name: String,
    val organizationNumber: String,
    val postings: List<SupplierPostingDTO>,
    val totalAmount: BigDecimal
)