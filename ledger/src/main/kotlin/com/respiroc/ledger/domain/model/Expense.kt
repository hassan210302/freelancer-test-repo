package com.respiroc.ledger.domain.model

import com.respiroc.tenant.domain.model.Tenant
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.TenantId
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

enum class ExpenseStatus {
    OPEN,
    DELIVERED,
    APPROVED
}

@Entity
@Table(name = "expenses")
open class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Long = -1

    @Column(name = "title", nullable = false, length = 255)
    open lateinit var title: String

    @Column(name = "description", nullable = false, length = 500)
    open lateinit var description: String

    @Column(name = "expense_date", nullable = false)
    open lateinit var expenseDate: LocalDate

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    open var status: ExpenseStatus = ExpenseStatus.OPEN

    @OneToMany(mappedBy = "expense", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    open var attachments: MutableList<ExpenseAttachment> = mutableListOf()

    @Column(name = "created_by", nullable = false, length = 100)
    open lateinit var createdBy: String

    @OneToMany(mappedBy = "expense", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    open var costs: MutableList<Cost> = mutableListOf()

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    open var amount: BigDecimal = BigDecimal.ZERO

    @TenantId
    @Column(name = "tenant_id", nullable = false)
    open var tenantId: Long = -1

    @Column(name = "category_id", nullable = false)
    open var categoryId: Short = -1
}