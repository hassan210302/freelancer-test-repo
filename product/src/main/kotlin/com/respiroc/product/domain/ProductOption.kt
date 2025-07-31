    package com.respiroc.product.domain

    import jakarta.persistence.*
    import org.hibernate.annotations.OnDelete
    import org.hibernate.annotations.OnDeleteAction

    @Entity
    @Table(name = "product_options")
    open class ProductOption {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id", nullable = false)
        open var id: Long = -1

        @Column(name = "product_id")
        open var productId: Long? = null

        @Column(name = "name", nullable = false, length = 100)
        open lateinit var name: String

        @Column(name = "position", nullable = false)
        open var position: Int = 0

        @ManyToOne(fetch = FetchType.LAZY)
        @OnDelete(action = OnDeleteAction.CASCADE)
        @JoinColumn(name = "product_id", nullable = true, updatable = false, insertable = false)
        open var product: Product? = null

        @OneToMany(mappedBy = "productOption")
        open var values: MutableSet<ProductOptionValue> = mutableSetOf()
    }