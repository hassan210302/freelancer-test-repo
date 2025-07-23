package com.respiroc.invoice.domain.entity

import com.respiroc.tenant.domain.model.Tenant
import jakarta.persistence.*
import org.hibernate.annotations.TenantId
import java.time.LocalDate

@Entity
@Table(name = "invoices")
open class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Long = -1

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    open var tenantId: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false, insertable = false)
    open lateinit var tenant: Tenant

    @Column(name = "number", nullable = false, unique = true, length = 10)
    open lateinit var number: String

    @Column(name = "issue_date", nullable = false)
    open lateinit var issueDate: LocalDate

    @Column(name = "due_date")
    open var dueDate: LocalDate? = null

    @Column(name = "currency_code", nullable = false, length = 5)
    open lateinit var currencyCode: String

    @Column(name = "supplier_id", nullable = false)
    open var supplierId: Long = -1

    @Column(name = "customer_id", nullable = false)
    open var customerId: Long = -1

    @OneToMany(mappedBy = "invoice", cascade = [CascadeType.ALL], orphanRemoval = true)
    open var lines: MutableList<InvoiceLine> = mutableListOf()
}