package com.respiroc.product.application.payload

//In future when we have to add products and options
data class ProductOptionPayload(
    val name: String,
    val values: List<String>
)
