package com.respiroc.product.repository

import com.respiroc.product.domain.Variant
import com.respiroc.util.repository.CustomJpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface VariantRepository : CustomJpaRepository<Variant, Long> {

    fun findByProductId(productId: Long): List<Variant>

    fun existsBySku(sku: String): Boolean
    fun findByProductIdAndOptionMap(productId: Long, optionMap: Map<String, String>): Variant?
    @Query("SELECT v.sku FROM Variant v")
    fun findAllSkus(): List<String>
    fun deleteByProductId(productId: Long)


}