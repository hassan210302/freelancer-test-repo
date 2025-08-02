package com.respiroc.webapp.controller.web

import com.respiroc.invoice.application.InvoicePdfGenerator
import com.respiroc.invoice.application.InvoiceService
import com.respiroc.util.currency.CurrencyService
import com.respiroc.webapp.controller.BaseController
import com.respiroc.webapp.controller.request.NewInvoiceRequest
import com.respiroc.webapp.controller.request.toPayload
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import java.util.*

@Controller
@RequestMapping(value = ["/invoice"])
class InvoiceWebController(
    private val invoiceService: InvoiceService,
    private val currencyService: CurrencyService
) : BaseController() {

    @GetMapping
    fun getInvoices(model: Model): String {
        addCommonAttributesForCurrentTenant(model, "Invoices")
        model.addAttribute("invoices", invoiceService.getInvoicesWithLines())
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

    @GetMapping("/{id}/pdf", produces = [MediaType.APPLICATION_PDF_VALUE])
    fun downloadInvoicePdf(
        @PathVariable id: Long,
        response: HttpServletResponse
    ) {
        val invoice = invoiceService.getInvoiceWithLines(id)
        val pdfBytes = InvoicePdfGenerator().generate(invoice)
        response.contentType = MediaType.APPLICATION_PDF_VALUE
        response.setHeader("Content-Disposition", "attachment; filename=invoice-${invoice.number}.pdf")
        response.setContentLength(pdfBytes.size)
        response.outputStream.use { it.write(pdfBytes) }
    }

    @GetMapping("/{id}/pdf/embed")
    fun embeddedInvoicePdf(@PathVariable id: Long): ResponseEntity<String> {
        val invoice = invoiceService.getInvoiceWithLines(id)
        val pdfBytes = InvoicePdfGenerator().generate(invoice)

        val base64Data = Base64.getEncoder().encodeToString(pdfBytes)
        val dataUrl = "data:application/pdf;base64,$base64Data"
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML)
            .body("""<embed id="pdf-embed" type="application/pdf" src="$dataUrl" style="width: 100%; height: 100%; border: none;"/>""")
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