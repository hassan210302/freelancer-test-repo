package com.respiroc.product.domain

import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(name = "product_option_values")
open class ProductOptionValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Long = -1

    @Column(name = "product_option_id")
    open var productOptionId: Long? = null

    @Column(name = "value", nullable = false, length = 100)
    open lateinit var value: String

    @Column(name = "position", nullable = false)
    open var position: Int = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "product_option_id", nullable = true, updatable = false, insertable = false)
    open var productOption: ProductOption? = null
}