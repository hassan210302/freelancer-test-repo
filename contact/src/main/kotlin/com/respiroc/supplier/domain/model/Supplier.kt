package com.respiroc.supplier.domain.model

import com.respiroc.company.domain.model.Company
import com.respiroc.tenant.domain.model.Tenant
import com.respiroc.util.domain.person.PrivatePerson
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.TenantId

@Entity
@Table(name = "suppliers")
open class Supplier {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Long = -1

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    open var tenantId: Long? = null

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false, insertable = false)
    open lateinit var tenant: Tenant

    @Column(name = "company_id", nullable = true, updatable = false, insertable = false)
    open var companyId: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = true)
    open var company: Company? = null

    @Column(name = "private_person_id", nullable = true, updatable = false, insertable = false)
    open var personId: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "private_person_id", nullable = true)
    open var person: PrivatePerson? = null
}