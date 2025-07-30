package com.respiroc.invoice.domain.entity

import com.respiroc.customer.domain.model.Customer
import com.respiroc.tenant.domain.model.Tenant
import jakarta.persistence.*
import org.hibernate.annotations.TenantId
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "invoices")
class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long = -1

    @TenantId
    @Column(name = "tenant_id", nullable = false)
    var tenantId: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false, insertable = false)
    lateinit var tenant: Tenant

    @Column(name = "number", nullable = false, unique = true)
    lateinit var number: String

    @Column(name = "issue_date", nullable = false)
    lateinit var issueDate: LocalDate

    @Column(name = "due_date")
    var dueDate: LocalDate? = null

    @Column(name = "currency_code", nullable = false)
    var currencyCode: String = ""

    @Column(name = "customer_id")
    var customerId: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", updatable = false, insertable = false, nullable = false)
    lateinit var customer: Customer

    @OneToMany(mappedBy = "invoice", cascade = [CascadeType.ALL], orphanRemoval = true)
    var lines: MutableList<InvoiceLine> = mutableListOf()

    @Transient
    lateinit var totalAmount: BigDecimal

}