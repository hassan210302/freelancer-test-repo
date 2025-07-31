package com.respiroc.product.repository

import com.respiroc.product.domain.ProductOption
import com.respiroc.util.repository.CustomJpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductOptionRepository : CustomJpaRepository<ProductOption, Long> {

    fun findByProductId(productId: Long): List<ProductOption>
    fun findByProductIdAndName(productId: Long, name: String): ProductOption?
}