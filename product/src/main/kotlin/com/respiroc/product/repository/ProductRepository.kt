package com.respiroc.product.repository

import com.respiroc.product.domain.Product
import com.respiroc.util.repository.CustomJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : CustomJpaRepository<Product, Long> {
    fun findByIdAndTenantId(productId: Long, tenantId: Long): Product?
}