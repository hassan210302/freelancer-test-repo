package com.respiroc.ledger.domain.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

enum class PaymentType {
    PRIVAT_UTLEGG,
    REFUSJON,
    BEDRIFTS_UTLEGG,
    KONTANT_BETALING,
    FAKTURABETALING
}

@Entity
@Table(name = "costs")
open class Cost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Long = -1

    @Column(name = "title", nullable = false, length = 255)
    open lateinit var title: String

    @Column(name = "date", nullable = false)
    open lateinit var date: LocalDate

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    open lateinit var amount: BigDecimal

    @Column(name = "vat", nullable = false)
    open var vat: Int = 0

    @Column(name = "currency", nullable = false, length = 3)
    open lateinit var currency: String

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    open lateinit var paymentType: PaymentType

    @Column(name = "chargeable", nullable = false)
    open var chargeable: Boolean = false

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    open lateinit var expense: Expense

} 