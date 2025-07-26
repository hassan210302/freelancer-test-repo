package com.respiroc.invoice.application.payload

import java.math.BigDecimal
import java.time.LocalDate

data class NewInvoicePayload(
    val issueDate: LocalDate,
    val dueDate: LocalDate?,
    val currencyCode: String,
    val supplierId: Long?,
    val customerId: Long?,
    val invoiceLines: List<NewInvoiceLinePayload>
)

data class NewInvoiceLinePayload(
    val itemName: String = "",
    val quantity: Int = 0,
    val unitPrice: BigDecimal = BigDecimal.ZERO,
    val discount: BigDecimal? = null,
    val vatCode: String = ""
)