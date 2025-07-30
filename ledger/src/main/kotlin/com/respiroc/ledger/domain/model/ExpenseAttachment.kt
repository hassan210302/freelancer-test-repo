package com.respiroc.ledger.domain.model

import com.respiroc.util.attachment.Attachment
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.TenantId
import java.time.Instant

@Entity
@Table(name = "expense_attachments")
open class ExpenseAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Long = -1

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    open lateinit var expense: Expense

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id", nullable = false)
    open lateinit var attachment: Attachment

    @TenantId
    @Column(name = "tenant_id", nullable = false)
    open var tenantId: Long = -1

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    open var createdAt: Instant? = null
}