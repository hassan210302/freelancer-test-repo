package com.respiroc.webapp.controller.web

import com.respiroc.invoice.application.InvoiceService
import com.respiroc.webapp.controller.BaseController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping(value = ["/invoice"])
class InvoiceWebController(private val invoiceService: InvoiceService): BaseController() {
}