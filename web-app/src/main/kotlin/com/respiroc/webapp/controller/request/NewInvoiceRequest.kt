package com.respiroc.webapp.controller.request

import java.math.BigDecimal
import java.time.LocalDate


data class NewInvoiceRequest(
    var number: String = "",
    var issueDate: LocalDate? = null,
    var dueDate: LocalDate? = null,
    var currencyCode: String = "",
    var supplierId: Long? = null,
    var customerId: Long? = null,
    var invoiceLines: List<NewInvoiceLineRequest>
)

data class NewInvoiceLineRequest(
    val itemName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val discount: BigDecimal? = null,
    val vatCode: String
)