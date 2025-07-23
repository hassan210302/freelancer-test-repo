package com.respiroc.invoice.domain.repository

import com.respiroc.invoice.domain.entity.Invoice
import com.respiroc.util.repository.CustomJpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InvoiceRepository: CustomJpaRepository<Invoice, Long>{
}