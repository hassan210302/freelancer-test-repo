package com.respiroc.webapp.controller.request

import com.respiroc.invoice.application.payload.NewInvoiceLinePayload
import com.respiroc.invoice.application.payload.NewInvoicePayload
import java.math.BigDecimal
import java.time.LocalDate

data class NewInvoiceRequest(
    var issueDate: LocalDate? = null,
    var dueDate: LocalDate? = null,
    var currencyCode: String = "",
    var customerId: Long,
    var invoiceLines: List<NewInvoiceLineRequest>
)

data class NewInvoiceLineRequest(
    val itemName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val discount: BigDecimal? = null,
    val vatCode: String
)

fun NewInvoiceRequest.toPayload(): NewInvoicePayload {
    return NewInvoicePayload(
        issueDate = this.issueDate ?: error("issueDate is required"),
        dueDate = this.dueDate,
        currencyCode = this.currencyCode,
        customerId = this.customerId,
        invoiceLines = this.invoiceLines.map { it.toPayload() }
    )
}

fun NewInvoiceLineRequest.toPayload(): NewInvoiceLinePayload {
    return NewInvoiceLinePayload(
        itemName = this.itemName,
        quantity = this.quantity,
        unitPrice = this.unitPrice,
        discount = this.discount,
        vatCode = this.vatCode
    )
}
