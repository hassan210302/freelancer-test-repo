package com.respiroc.product.application.payload

data class CreateProductPayload(
    val name: String,
    val description: String?,
    val hasVariants: Boolean,
    val productSku: String? = null,  //these fields are added for default product (no variant)
    val productprice: Int? = null,
    val productStockQty: Int? = null,
    val variants: List<VariantPayload> = emptyList()
)