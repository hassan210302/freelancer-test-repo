package com.respiroc.product.application.payload

data class VariantPayload(
    val sku: String,
    val price: Int,
    val stockQty: Int = 0,
    val isDefault: Boolean = false,
    val optionCombination: Map<String, String> = emptyMap()
)