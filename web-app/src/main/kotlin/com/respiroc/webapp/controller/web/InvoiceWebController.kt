package com.respiroc.webapp.controller.web

import com.respiroc.invoice.application.InvoiceService
import com.respiroc.util.currency.CurrencyService
import com.respiroc.webapp.controller.BaseController
import com.respiroc.webapp.controller.request.NewInvoiceRequest
import com.respiroc.webapp.controller.request.toPayload
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping(value = ["/invoice"])
class InvoiceWebController(
    private val invoiceService: InvoiceService,
    private val currencyService: CurrencyService
) : BaseController() {

    @GetMapping
    fun getInvoices(model: Model): String {
        addCommonAttributesForCurrentTenant(model, "Invoices")
        // TODO handle pagination
        model.addAttribute("invoices", invoiceService.getInvoicesWithLines(0, 1000).content)
        return "invoice/invoice"
    }

    @GetMapping("/{id}")
    fun getInvoice(model: Model, @PathVariable id: Long): String {
        addCommonAttributesForCurrentTenant(model, "Invoice")
        model.addAttribute("invoice", invoiceService.getInvoiceWithLines(id))
        return "invoice/invoice-detail"
    }

    @GetMapping("/new")
    fun getInvoiceForm(model: Model): String {
        model.addAttribute("supportedCurrencies", currencyService.getSupportedCurrencies())
        addCommonAttributesForCurrentTenant(model, "New Invoice")
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
class InvoiceHTNXWebController(private val invoiceService: InvoiceService) : BaseController() {

    @PostMapping
    fun registerInvoice(@ModelAttribute request: NewInvoiceRequest): String {
        invoiceService.save(request.toPayload())
        return "redirect:htmx:/invoice"
    }
}