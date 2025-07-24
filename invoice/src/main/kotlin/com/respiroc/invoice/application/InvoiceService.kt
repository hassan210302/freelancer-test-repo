package com.respiroc.invoice.application

import com.respiroc.invoice.domain.entity.Invoice
import com.respiroc.invoice.domain.entity.InvoiceLine
import com.respiroc.invoice.domain.repository.InvoiceRepository
import com.respiroc.ledger.application.VatService
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@Transactional
class InvoiceService(
    private val invoiceRepository: InvoiceRepository,
    private val vatService: VatService
) {

    fun getInvoicesWithLines(page: Int, size: Int): Page<Invoice> {
        val pageable = PageRequest.of(page, size)
        val invoicePage = invoiceRepository.findAllInvoices(pageable)
        val invoiceIds = invoicePage.content.map { it.id }
        val invoiceMapWithLines = if (invoiceIds.isNotEmpty()) {
            invoiceRepository.fetchLinesByInvoiceIds(invoiceIds)
                .associateBy { it.id }
        } else {
            emptyMap()
        }

        invoicePage.content.forEach { invoice ->
            val invoiceWithLines = invoiceMapWithLines[invoice.id]
            if (invoiceWithLines != null) {
                invoice.lines = invoiceWithLines.lines
                invoice.totalAmount = calculateTotalAmount(invoice.lines)
            }
        }
        return invoicePage
    }

    fun getInvoiceWithLines(invoiceId: Long): Invoice {
        val invoice = invoiceRepository.findInvoiceById(invoiceId)!!
        invoice.totalAmount = calculateTotalAmount(invoice.lines)
        return invoice
    }

    private fun calculateTotalAmount(lines: List<InvoiceLine>): BigDecimal {
        return lines.fold(BigDecimal.ZERO) { acc, line ->
            val subtotal = line.unitPrice
                .multiply(BigDecimal.valueOf(line.quantity.toLong()))
                .minus(line.discount ?: BigDecimal.ZERO)

            val vatCode = vatService.findVatCodeByCode(line.vatCode)
            val vatAmount = vatService.calculateVatAmount(subtotal, vatCode!!)
            acc + subtotal + vatAmount
        }
    }
}