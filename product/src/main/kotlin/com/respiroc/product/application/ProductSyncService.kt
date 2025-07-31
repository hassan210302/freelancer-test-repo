package com.respiroc.product.application

import com.respiroc.product.application.payload.CreateProductPayload
import com.respiroc.product.application.payload.FammeProduct
import com.respiroc.product.application.payload.FammeProductResponse
import com.respiroc.product.application.payload.VariantPayload
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class ProductSyncService(
    private val fammeRestClient: RestClient,
    private val productService: ProductService
) {

    companion object {
        private val currentTenantId = ThreadLocal<Long>()

        fun getCurrentTenantId(): Long? {
            return currentTenantId.get()
        }

        private fun setCurrentTenantId(tenantId: Long) {
            currentTenantId.set(tenantId)
        }

        private fun clearCurrentTenantId() {
            currentTenantId.remove()
        }
    }

    @Scheduled(initialDelay = 0, fixedRate = 3600000)
    fun syncProductsFromFamme() {
        try {
            // Set tenant context to 1
            setCurrentTenantId(1L)

            val response = fammeRestClient.get()
                .uri("/products.json")
                .retrieve()
                .body(FammeProductResponse::class.java)

            response?.products?.take(50)?.forEach { fammeProduct ->
                val CreateProductPayload = convertToCreateProductPayload(fammeProduct)
                productService.createProduct(CreateProductPayload)
            }

            println("Successfully synced ${response?.products?.take(50)?.size ?: 0} products from Famme")

        } catch (e: Exception) {
            println("Error syncing products from Famme: ${e.message}")
        } finally {
            clearCurrentTenantId()
        }
    }

    private fun convertToCreateProductPayload(fammeProduct: FammeProduct): CreateProductPayload {
        val variants = fammeProduct.variants.map { fammeVariant ->
            val color = fammeVariant.option1
            val size = fammeVariant.option3 ?: fammeVariant.option2  //sizes in the api is either in option2 or option3

            VariantPayload(
                sku = fammeVariant.sku,
                priceCents = (fammeVariant.price.toDouble() * 100).toInt(),
                stockQty = if (fammeVariant.available) 1 else 0, // Only available is given
                isDefault = false,
                optionCombination = buildMap {
                    color?.let { put("color", it) }
                    size?.let { put("size", it) }
                }
            )
        }

        return CreateProductPayload(
            name = fammeProduct.title,
            description = stripHtml(fammeProduct.bodyHtml),
            hasVariants = variants.size > 1,
            variants = variants
        )
    }

    private fun stripHtml(html: String?): String? {
        return html?.replace(Regex("<[^>]*>"), "")?.trim()
    }
}