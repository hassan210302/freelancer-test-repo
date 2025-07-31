package com.respiroc.invoice.application

import com.respiroc.customer.application.CustomerService
import com.respiroc.customer.domain.model.Customer
import com.respiroc.invoice.application.payload.NewInvoiceLinePayload
import com.respiroc.invoice.application.payload.NewInvoicePayload
import com.respiroc.invoice.domain.entity.Invoice
import com.respiroc.invoice.domain.entity.InvoiceLine
import com.respiroc.invoice.domain.repository.InvoiceRepository
import com.respiroc.ledger.application.VatService
import com.respiroc.ledger.application.VoucherService
import com.respiroc.ledger.application.payload.CreatePostingPayload
import com.respiroc.ledger.application.payload.CreateVoucherPayload
import com.respiroc.ledger.application.payload.VoucherPayload
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
@Transactional
class InvoiceService(
    private val invoiceRepository: InvoiceRepository,
    private val vatService: VatService,
    private val voucherService: VoucherService,
    private val customerService: CustomerService
) {

    fun save(newInvoice: NewInvoicePayload): Invoice {
        val invoice = toInvoice(newInvoice).apply {
            number = getNextInvoiceNumber(issueDate.year)
        }
        var savedInvoice = invoiceRepository.save(invoice)
        savedInvoice.lines = newInvoice.invoiceLines.map { linePayload ->
            toInvoiceLine(linePayload, savedInvoice.id)
        }.toMutableList()
        savedInvoice = invoiceRepository.save(savedInvoice)
        populateInvoiceAmounts(savedInvoice)
        val customer = customerService.findById(invoice.customerId!!)
        createVoucher(savedInvoice, customer)
        return savedInvoice
    }

    fun createVoucher(invoice: Invoice, customer: Customer): VoucherPayload {
        val description = "Invoice number ${invoice.number} to ${customer.getName()}"
        val now = LocalDate.now()
        val postings = mutableListOf<CreatePostingPayload>()
        postings +=
            CreatePostingPayload(
                accountNumber = "1500", // Accounts Receivables
                amount = invoice.totalAmount,
                currency = invoice.currencyCode,
                postingDate = now,
                description = description,
                originalAmount = invoice.totalAmount,
                originalCurrency = invoice.currencyCode,
                rowNumber = 0
            )
        postings +=
            CreatePostingPayload(
                accountNumber = "3000", // Sales – Goods – High VAT Rate
                amount = invoice.discountedSubTotal.negate(),
                currency = invoice.currencyCode,
                postingDate = now,
                description = description,
                originalAmount = invoice.discountedSubTotal.negate(),
                originalCurrency = invoice.currencyCode,
                rowNumber = 1
            )
        invoice.lines
            .filter { it.vatRate != BigDecimal.ZERO }
            .forEachIndexed { index, invoiceLine ->
                postings +=
                    CreatePostingPayload(
                        accountNumber = "2700", // Output VAT
                        amount = invoiceLine.vat.negate(),
                        currency = invoice.currencyCode,
                        postingDate = now,
                        description = description,
                        originalAmount = invoiceLine.vat.negate(),
                        originalCurrency = invoice.currencyCode,
                        vatCode = invoiceLine.vatCode,
                        rowNumber = index + 2
                    )
            }
        val voucherPayload = CreateVoucherPayload(
            description = description,
            date = now,
            postings = postings
        )
        return voucherService.createVoucher(voucherPayload)
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
                populateInvoiceAmounts(invoice)
            }
        }
        return invoices
    }

    fun getInvoiceWithLines(invoiceId: Long): Invoice {
        val invoice = invoiceRepository.findInvoiceById(invoiceId)!!
        populateInvoiceAmounts(invoice)
        return invoice
    }

    private fun populateInvoiceAmounts(invoice: Invoice) {
        invoice.totalAmount = BigDecimal.ZERO
        invoice.discountedSubTotal = BigDecimal.ZERO
        invoice.lines.forEach { line ->
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
            invoice.totalAmount += line.totalAmount
            invoice.discountedSubTotal += discountedSubtotal
        }
    }
}