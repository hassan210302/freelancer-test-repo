package com.respiroc.webapp.controller.web

import com.respiroc.invoice.application.InvoiceService
import com.respiroc.invoice.application.payload.NewInvoicePayload
import com.respiroc.webapp.controller.BaseController
import com.respiroc.webapp.controller.request.NewInvoiceRequest
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping(value = ["/invoice"])
class InvoiceWebController(private val invoiceService: InvoiceService): BaseController() {

    @GetMapping
    fun getInvoices(model: Model): String {
        addCommonAttributesForCurrentTenant(model, "Invoices")
        model.addAttribute("invoices", emptyList<Any>()) // TODO: Replace with invoiceService.getAll()
        return "invoice/invoice"
    }

    @GetMapping("/new")
    fun getInvoiceForm(model: Model): String {
        addCommonAttributesForCurrentTenant(model, "New Invoice")
        model.addAttribute("invoice", NewInvoiceRequest())
        return "invoice/invoice-form"
    }

    @GetMapping("/{id}/pdf")
    fun viewInvoicePdf(@PathVariable id: Long): String {
        // TODO: Implement logic to generate/view PDF
//        return "invoice/invoice-pdf-view"
        return "invoice/invoice"
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    fun deleteInvoice(@PathVariable id: Long) {
        // TODO: Implement delete logic
//        invoiceService.deleteById(id)
    }

}

@Controller
@RequestMapping(value = ["/htmx/invoice"])
class InvoiceHTNXWebController(private val invoiceService: InvoiceService): BaseController() {

    @PostMapping
    fun registerInvoice(@ModelAttribute request: NewInvoiceRequest): String {
        val payload = NewInvoicePayload(
            number = request.number,
            issueDate = request.issueDate!!,
            dueDate = request.dueDate,
            currencyCode = request.currencyCode,
            supplierId = request.supplierId,
            customerId = request.customerId
            // TODO: Add line items if applicable
        )
//        invoiceService.save(payload)
        return "redirect:htmx:/invoice"
    }
}