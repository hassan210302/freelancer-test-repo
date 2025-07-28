package com.respiroc.invoice.application

import com.respiroc.invoice.application.payload.NewInvoiceLinePayload
import com.respiroc.invoice.application.payload.NewInvoicePayload
import com.respiroc.invoice.domain.entity.Invoice
import com.respiroc.invoice.domain.entity.InvoiceLine
import com.respiroc.invoice.domain.repository.InvoiceRepository
import com.respiroc.ledger.application.VatService
import com.respiroc.util.context.ContextAwareApi
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@Transactional
class InvoiceService(
    private val invoiceRepository: InvoiceRepository,
    private val vatService: VatService,
    private val entityManager: EntityManager,
) : ContextAwareApi {
    private final val invoiceSequenceSeed = 1000

    fun save(newInvoice: NewInvoicePayload): Invoice {
        val invoice = toInvoice(newInvoice).apply {
            number = getNextInvoiceNumber(tenantId(), issueDate.year).toString()
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
        invoice.supplierId = payload.supplierId
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

    fun getNextInvoiceNumber(tenantId: Long, year: Int): Int {
        val query = entityManager.createNativeQuery(
            """
            WITH upsert AS (
              INSERT INTO invoice_sequences (tenant_id, year, next_number)
              VALUES (:tenantId, :year, :invoiceSequenceSeed)
              ON CONFLICT (tenant_id, year)
              DO UPDATE SET next_number = invoice_sequences.next_number + 1
              RETURNING next_number
            )
            SELECT next_number FROM upsert
        """.trimIndent()
        )
        query.setParameter("tenantId", tenantId)
        query.setParameter("year", year)
        query.setParameter("invoiceSequenceSeed", invoiceSequenceSeed)
        return (query.singleResult as Number).toInt()
    }

    fun getInvoicesWithLines(page: Int, size: Int): Page<Invoice> {
        val pageable = PageRequest.of(page, size).withSort(Sort.by("id").descending())
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
            val quantity = BigDecimal.valueOf(line.quantity.toLong())
            val subtotal = line.unitPrice.multiply(quantity)
            val discountPercent = line.discount ?: BigDecimal.ZERO
            val discountAmount = subtotal.multiply(discountPercent).divide(BigDecimal(100))
            val discountedSubtotal = subtotal.subtract(discountAmount)
            val vatCode = vatService.findVatCodeByCode(line.vatCode)
            val vatAmount = vatService.calculateVatAmount(discountedSubtotal, vatCode!!)
            line.totalAmount = discountedSubtotal + vatAmount
            acc + discountedSubtotal + vatAmount
        }
    }
}