package com.respiroc.ledger.domain.model

import jakarta.persistence.*

@Entity
@Table(name = "expense_attachments")
open class ExpenseAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Long = -1

    @Column(name = "file_data", nullable = false, columnDefinition = "BYTEA")
    open lateinit var fileData: ByteArray

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    open lateinit var expense: Expense
}