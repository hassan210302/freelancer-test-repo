package com.respiroc.invoice.domain.repository

import com.respiroc.invoice.domain.entity.Invoice
import com.respiroc.util.repository.CustomJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface InvoiceRepository : CustomJpaRepository<Invoice, Long> {
    @Query(
        value = """
        SELECT DISTINCT i FROM Invoice i
        LEFT JOIN FETCH i.customer customer
        LEFT JOIN FETCH i.supplier supplier
        LEFT JOIN FETCH customer.company customerCompany
        LEFT JOIN FETCH customer.person customerPerson
        LEFT JOIN FETCH supplier.company supplierCompany
        LEFT JOIN FETCH supplier.person supplierPerson
    """,
        countQuery = """
        SELECT COUNT(i) FROM Invoice i
    """
    )
    fun findAllInvoices(pageable: Pageable): Page<Invoice>

    @Query(
        """
    SELECT DISTINCT i FROM Invoice i
    LEFT JOIN FETCH i.lines
    WHERE i.id IN :ids
    """
    )
    fun fetchLinesByInvoiceIds(@Param("ids") ids: List<Long>): List<Invoice>

    @Query(
        value = """
        SELECT DISTINCT i FROM Invoice i
        LEFT JOIN FETCH i.customer customer
        LEFT JOIN FETCH i.supplier supplier
        LEFT JOIN FETCH i.lines
        LEFT JOIN FETCH customer.company customerCompany
        LEFT JOIN FETCH customer.person customerPerson
        LEFT JOIN FETCH supplier.company supplierCompany
        LEFT JOIN FETCH supplier.person supplierPerson
        WHERE i.id = :id
    """
    )
    fun findInvoiceById(@Param("id") id:Long): Invoice?
}