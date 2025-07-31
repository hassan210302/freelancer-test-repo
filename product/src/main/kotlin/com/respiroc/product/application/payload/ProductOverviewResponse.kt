package com.respiroc.product.application.payload

import java.time.LocalDateTime


data class ProductOverviewResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val hasVariants: Boolean,
    val createdAt: LocalDateTime,
    val options: List<ProductOptionResponse>,
    val variants: List<VariantResponse>
)

data class ProductOptionResponse(
    val id: Long,
    val name: String,
    val position: Int,
    val values: List<ProductOptionValueResponse>
)

data class ProductOptionValueResponse(
    val id: Long,
    val value: String,
    val position: Int
)

data class VariantResponse(
    val id: Long,
    val sku: String,
    val priceCents: Int,
    val stockQty: Int,
    val optionMap: Map<String, String>,
    val isDefault: Boolean
)