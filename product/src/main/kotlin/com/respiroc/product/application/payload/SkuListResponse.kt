package com.respiroc.product.application.payload


// I am adding this because of batch update, in batch update we can update only once so if a user add
// new varaint sku must be unique
data class SkuListResponse(
    val skus: List<String>
)