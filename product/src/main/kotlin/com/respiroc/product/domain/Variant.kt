package com.respiroc.product.domain

import com.respiroc.tenant.domain.model.Tenant
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.annotations.TenantId
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "variants")
open class Variant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Long = -1

    @Column(name = "product_id")
    open var productId: Long? = null

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    open var tenantId: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false, insertable = false)
    open lateinit var tenant: Tenant

    @Column(name = "sku", nullable = false, length = 100)
    open lateinit var sku: String

    @Column(name = "price_cents", nullable = false)
    open var priceCents: Int = 0

    @Column(name = "stock_qty", nullable = false)
    open var stockQty: Int = 0

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "option_map", nullable = false)
    open var optionMap: Map<String, String> = emptyMap()

    @Column(name = "is_default", nullable = false, insertable = false, updatable = false)
    open var isDefault: Boolean = false

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    open lateinit var createdAt: Instant

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    open lateinit var updatedAt: Instant

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "product_id", nullable = true, updatable = false, insertable = false)
    open var product: Product? = null
}