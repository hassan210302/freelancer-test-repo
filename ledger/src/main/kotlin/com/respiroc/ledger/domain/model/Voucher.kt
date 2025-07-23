package com.respiroc.ledger.domain.model

import com.respiroc.tenant.domain.model.Tenant
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.annotations.TenantId
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "vouchers")
open class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Long = -1

    @Column(name = "number", nullable = false)
    open var number: Short = 0

    @Column(name = "date", nullable = false)
    open lateinit var date: LocalDate

    @Column(name = "description", length = Integer.MAX_VALUE)
    open var description: String? = null

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    open var tenantId: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false, insertable = false)
    open lateinit var tenant: Tenant

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    open lateinit var createdAt: Instant

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    open lateinit var updatedAt: Instant

    @OneToMany(mappedBy = "voucher")
    open var postings: MutableSet<Posting> = mutableSetOf()

    open fun getDisplayNumber(): String {
        return "${number}-${date.year}"
    }
}