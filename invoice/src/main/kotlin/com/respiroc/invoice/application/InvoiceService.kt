package com.respiroc.invoice.application

import com.respiroc.invoice.application.payload.NewInvoiceLinePayload
import com.respiroc.invoice.application.payload.NewInvoicePayload
import com.respiroc.invoice.domain.entity.Invoice
import com.respiroc.invoice.domain.entity.InvoiceLine
import com.respiroc.invoice.domain.repository.InvoiceRepository
import com.respiroc.ledger.application.VatService
import com.respiroc.util.context.ContextAwareApi
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
@Transactional
class InvoiceService(
    private val invoiceRepository: InvoiceRepository,
    private val vatService: VatService
) : ContextAwareApi {

    fun save(newInvoice: NewInvoicePayload): Invoice {
        val invoice = toInvoice(newInvoice).apply {
            number = getNextInvoiceNumber(issueDate.year)
        }
        val savedInvoice = invoiceRepository.save(invoice)
        savedInvoice.lines = newInvoice.invoiceLines.map { linePayload ->
            toInvoiceLine(linePayload, savedInvoice.id)
        }.toMutableList()
        return invoiceRepository.save(savedInvoice)
    }


    private fun toInvoice(payload: NewInvoicePayload): Invoice {
        val invoice = Invoice()
        invoice.issueDate = payload.issueDate
        invoice.dueDate = payload.dueDate
        invoice.currencyCode = payload.currencyCode
        invoice.customerId = payload.customerId
        return invoice
    }

    private fun toInvoiceLine(payload: NewInvoiceLinePayload, invoiceId: Long): InvoiceLine {
        val entity = InvoiceLine()
        entity.invoiceId = invoiceId
        entity.itemName = payload.itemName
        entity.quantity = payload.quantity
        entity.unitPrice = payload.unitPrice
        entity.discount = payload.discount
        entity.vatCode = payload.vatCode
        return entity
    }

    fun getNextInvoiceNumber(year: Int): String {
        val yearInvoiceCount = invoiceRepository.countInvoicesByIssueDateYear(year)
        return "${year}-${yearInvoiceCount + 1}"
    }

    fun getInvoicesWithLines(): List<Invoice> {
        val invoices = invoiceRepository.findAllInvoices()
        val invoiceIds = invoices.map { it.id }
        val invoiceMapWithLines = if (invoiceIds.isNotEmpty()) {
            invoiceRepository.fetchLinesByInvoiceIds(invoiceIds)
                .associateBy { it.id }
        } else {
            emptyMap()
        }

        invoices.forEach { invoice ->
            val invoiceWithLines = invoiceMapWithLines[invoice.id]
            if (invoiceWithLines != null) {
                invoice.lines = invoiceWithLines.lines
                invoice.totalAmount = calculateTotalAmount(invoice.lines)
            }
        }
        return invoices
    }

    fun getInvoiceWithLines(invoiceId: Long): Invoice {
        val invoice = invoiceRepository.findInvoiceById(invoiceId)!!
        invoice.totalAmount = calculateTotalAmount(invoice.lines)
        return invoice
    }

    private fun calculateTotalAmount(lines: List<InvoiceLine>): BigDecimal {
        var totalAmount = BigDecimal.ZERO
        lines.forEach { line ->
            val quantity = BigDecimal.valueOf(line.quantity.toLong())
            val subTotal = line.unitPrice.multiply(quantity)
            val discountPercent = line.discount ?: BigDecimal.ZERO
            val discountAmount = subTotal.multiply(discountPercent).divide(BigDecimal(100), RoundingMode.HALF_UP)
            val discountedSubtotal = subTotal.subtract(discountAmount)
            val vatCode = vatService.findVatCodeByCode(line.vatCode)
            val vatAmount = vatService.calculateVatAmount(discountedSubtotal, vatCode!!)
            line.vatRate = vatCode.rate
            line.vat = vatAmount
            line.discountAmount = discountAmount
            line.subTotal = subTotal
            line.totalAmount = discountedSubtotal + vatAmount
            totalAmount = totalAmount + discountedSubtotal + vatAmount
        }
        return totalAmount
    }
}