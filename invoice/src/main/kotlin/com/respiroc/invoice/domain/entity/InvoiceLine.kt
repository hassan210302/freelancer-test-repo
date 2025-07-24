package com.respiroc.invoice.domain.entity

import jakarta.persistence.*
import jakarta.validation.constraints.Size
import java.math.BigDecimal

@Entity
@Table(name = "invoice_lines")
class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long = -1

    @Column(name = "invoice_id", nullable = false, updatable = false)
    var invoiceId: Long = -1

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false, updatable = false, insertable = false)
    lateinit var invoice: Invoice

    @Size(max = 25)
    @Column(name = "item_name", nullable = false, length = 25)
    var itemName: String = ""

    @Column(name = "quantity", nullable = false)
    var quantity: Int = 0

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    var unitPrice: BigDecimal = BigDecimal.ZERO

    @Column(name = "discount", precision = 10, scale = 2)
    var discount: BigDecimal? = null

    @Size(max = 10)
    @Column(name = "vat_code", nullable = false, length = 10)
    lateinit var vatCode: String
}