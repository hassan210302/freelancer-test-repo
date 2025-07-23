package com.respiroc.ledger.domain.model

import com.respiroc.tenant.domain.model.Tenant
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.annotations.TenantId
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "expenses")
open class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Long = -1

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    open lateinit var amount: BigDecimal

    @Column(name = "description", nullable = false, length = 500)
    open lateinit var description: String

    @Column(name = "expense_date", nullable = false)
    open lateinit var expenseDate: LocalDate

    @Column(name = "receipt_path", length = 255)
    open var receiptPath: String? = null

    @Column(name = "created_by", nullable = false, length = 100)
    open lateinit var createdBy: String

    @TenantId
    @Column(name = "tenant_id", nullable = false)
    open var tenantId: Long = -1

    @Column(name = "category_id", nullable = false)
    open var categoryId: Long = -1

    @Column(name = "account_number", length = 10)
    open var accountNumber: String? = null  // Links to chart of accounts

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    open lateinit var createdAt: Instant

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    open lateinit var updatedAt: Instant
}