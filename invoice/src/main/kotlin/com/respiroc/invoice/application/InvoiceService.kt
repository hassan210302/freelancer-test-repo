package com.respiroc.invoice.application

import com.respiroc.invoice.domain.repository.InvoiceRepository
import org.springframework.stereotype.Service

@Service
class InvoiceService(private val invoiceRepository: InvoiceRepository) {
}