package com.respiroc.product.application.payload

data class UpdateProductPayload(
    val name: String,
    val description: String?,
    val hasVariants: Boolean,
    val productSku: String? = null,
    val productprice: Int? = null,
    val productStockQty: Int? = null,
    val variants: List<UpdateVariantPayload> = emptyList()
)

data class UpdateVariantPayload(
    val id: Long? = null,
    val sku: String,
    val price: Int,
    val stockQty: Int = 0,
    val deleted: Boolean = false,
    val optionCombination: Map<String, String> = emptyMap()
)