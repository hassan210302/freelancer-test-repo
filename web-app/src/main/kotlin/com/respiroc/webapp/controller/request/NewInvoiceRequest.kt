package com.respiroc.webapp.controller.request

import java.time.LocalDate


data class NewInvoiceRequest(
    var number: String = "",

    var issueDate: LocalDate? = null,

    var dueDate: LocalDate? = null,

    var currencyCode: String = "",

    var supplierId: Long? = null,

    var customerId: Long? = null


    // TODO: add invoice lines

)