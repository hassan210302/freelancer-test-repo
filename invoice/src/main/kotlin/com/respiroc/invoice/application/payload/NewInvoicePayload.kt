package com.respiroc.invoice.application.payload

import java.time.LocalDate

data class NewInvoicePayload(
    val number: String,
    val issueDate: LocalDate,
    val dueDate: LocalDate?,
    val currencyCode: String,
    val supplierId: Long?,
    val customerId: Long?

    // TODO: Add line items
)