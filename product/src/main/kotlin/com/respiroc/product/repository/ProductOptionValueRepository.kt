package com.respiroc.product.repository

import com.respiroc.product.domain.ProductOptionValue
import com.respiroc.util.repository.CustomJpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductOptionValueRepository : CustomJpaRepository<ProductOptionValue, Long> {

    fun findByProductOptionId(productOptionId: Long): List<ProductOptionValue>
}