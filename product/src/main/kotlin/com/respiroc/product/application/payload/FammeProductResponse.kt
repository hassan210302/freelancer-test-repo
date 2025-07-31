package com.respiroc.product.application.payload

import com.fasterxml.jackson.annotation.JsonProperty

data class FammeProductResponse(
    val products: List<FammeProduct>
)


data class FammeProduct(
    val title: String,
    @JsonProperty("body_html")
    val bodyHtml: String?,
    val variants: List<FammeVariant>
)

data class FammeVariant(
    val option1: String?,
    val option2: String?,
    val option3: String?,
    val sku: String,
    val available: Boolean,
    val price: String
)