package com.respiroc.ledger.domain.model

import com.respiroc.tenant.domain.model.Tenant
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.Size
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.annotations.TenantId
import org.hibernate.annotations.UpdateTimestamp
import java.io.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "postings")
open class Posting : Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Long = -1

    @Size(max = 10)
    @Column(name = "account_number", nullable = false, length = 10)
    open lateinit var accountNumber: String

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    open lateinit var amount: BigDecimal

    @Size(max = 3)
    @Column(name = "currency", nullable = false, length = 3)
    open lateinit var currency: String

    @Column(name = "original_amount", precision = 15, scale = 2)
    open var originalAmount: BigDecimal? = null

    @Size(max = 3)
    @Column(name = "original_currency", length = 3)
    open var originalCurrency: String? = null

    @Size(max = 10)
    @Column(name = "vat_code", length = 10)
    open var vatCode: String? = null

    @Column(name = "posting_date", nullable = false)
    open lateinit var postingDate: LocalDate

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

    @Column(name = "voucher_id")
    open var voucherId: Long? = null

    @Column(name = "row_number", nullable = false)
    open var rowNumber: Int = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "voucher_id", nullable = true, updatable = false, insertable = false)
    open var voucher: Voucher? = null
}
