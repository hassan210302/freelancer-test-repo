package com.respiroc.product.application

import com.respiroc.product.application.payload.CreateProductPayload
import com.respiroc.product.application.payload.ProductOptionPayload
import com.respiroc.product.application.payload.VariantPayload
import com.respiroc.product.domain.Product
import com.respiroc.product.domain.ProductOption
import com.respiroc.product.domain.ProductOptionValue
import com.respiroc.product.domain.Variant
import com.respiroc.product.repository.ProductRepository
import com.respiroc.product.repository.ProductOptionRepository
import com.respiroc.product.repository.ProductOptionValueRepository
import com.respiroc.product.repository.VariantRepository
import com.respiroc.product.application.payload.ProductOptionResponse
import com.respiroc.product.application.payload.ProductOptionValueResponse
import com.respiroc.product.application.payload.ProductOverviewResponse
import com.respiroc.product.application.payload.SkuListResponse
import com.respiroc.product.application.payload.UpdateProductPayload
import com.respiroc.product.application.payload.UpdateVariantPayload
import com.respiroc.product.application.payload.VariantResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId

@Service
@Transactional
class ProductService(
    private val productRepository: ProductRepository,
    private val productOptionRepository: ProductOptionRepository,
    private val productOptionValueRepository: ProductOptionValueRepository,
    private val variantRepository: VariantRepository,
) {

    @Transactional
    fun createProduct(request: CreateProductPayload): Product {
        val product = Product().apply {
            this.name = request.name
            this.description = request.description
            this.hasVariants = request.hasVariants
        }
        val savedProduct = productRepository.save(product)
        createStandardProductOptionsWithValues(savedProduct.id)
        createVariants(savedProduct.id, request)
        return savedProduct
    }

    fun getAllProductsWithDetails(): List<ProductOverviewResponse> {
        val products = productRepository.findAll()

        return products.map { product ->
            ProductOverviewResponse(
                id = product.id,
                name = product.name,
                description = product.description,
                hasVariants = product.hasVariants,
                createdAt = product.createdAt.atZone(ZoneId.systemDefault()).toLocalDateTime(),
                options = product.options.map { option ->
                    ProductOptionResponse(
                        id = option.id,
                        name = option.name,
                        position = option.position,
                        values = option.values.map { value ->
                            ProductOptionValueResponse(
                                id = value.id,
                                value = value.value,
                                position = value.position
                            )
                        }
                    )
                },
                variants = product.variants.map { variant ->
                    VariantResponse(
                        id = variant.id,
                        sku = variant.sku,
                        priceCents = variant.priceCents,
                        stockQty = variant.stockQty,
                        optionMap = variant.optionMap,
                        isDefault = variant.isDefault
                    )
                }
            )
        }
    }

    fun getAllProductsWithDetailsByTenant(): List<ProductOverviewResponse> {
        val products = productRepository.findAll()
        return products.map { product ->
            ProductOverviewResponse(
                id = product.id,
                name = product.name,
                description = product.description,
                hasVariants = product.hasVariants,
                createdAt = product.createdAt.atZone(ZoneId.systemDefault()).toLocalDateTime(),
                options = product.options.map { option ->
                    ProductOptionResponse(
                        id = option.id,
                        name = option.name,
                        position = option.position,
                        values = option.values.map { value ->
                            ProductOptionValueResponse(
                                id = value.id,
                                value = value.value,
                                position = value.position
                            )
                        }
                    )
                },
                variants = product.variants.map { variant ->
                    VariantResponse(
                        id = variant.id,
                        sku = variant.sku,
                        priceCents = variant.priceCents,
                        stockQty = variant.stockQty,
                        optionMap = variant.optionMap,
                        isDefault = variant.isDefault
                    )
                }
            )
        }
    }

    fun getProductWithDetailsByIdAndTenant(productId: Long, tenantId: Long): ProductOverviewResponse? {
        val product = productRepository.findByIdAndTenantId(productId, tenantId) ?: return null

        return ProductOverviewResponse(
            id = product.id,
            name = product.name,
            description = product.description,
            hasVariants = product.hasVariants,
            createdAt = product.createdAt.atZone(ZoneId.systemDefault()).toLocalDateTime(),
            options = product.options.map { option ->
                ProductOptionResponse(
                    id = option.id,
                    name = option.name,
                    position = option.position,
                    values = option.values.map { value ->
                        ProductOptionValueResponse(
                            id = value.id,
                            value = value.value,
                            position = value.position
                        )
                    }
                )
            },
            variants = product.variants.map { variant ->
                VariantResponse(
                    id = variant.id,
                    sku = variant.sku,
                    priceCents = variant.priceCents,
                    stockQty = variant.stockQty,
                    optionMap = variant.optionMap,
                    isDefault = variant.isDefault
                )
            }
        )
    }

    fun getProduct(id: Long): Product? {
        return productRepository.findById(id).orElse(null)
    }

    // I used save all before but the json (from scheduler can have duplicate variant for that i added this logic)
    private fun createVariants(productId: Long, request: CreateProductPayload) {
        if (!request.hasVariants && request.productSku != null) {
            val singleVariant = Variant().apply {
                this.productId = productId
                this.sku = request.productSku
                this.priceCents = request.productPriceCents ?: 0
                this.stockQty = request.productStockQty ?: 0
                this.optionMap = emptyMap()
            }
            variantRepository.save(singleVariant)
        } else {
            request.variants.forEach { variantPayload ->
                val existingVariant = variantRepository.findByProductIdAndOptionMap(
                    productId, variantPayload.optionCombination
                )

                if (existingVariant != null) {
                    existingVariant.stockQty += variantPayload.stockQty
                    existingVariant.priceCents = variantPayload.priceCents
                    variantRepository.save(existingVariant)
                } else {
                    val variant = Variant().apply {
                        this.productId = productId
                        this.sku = variantPayload.sku
                        this.priceCents = variantPayload.priceCents
                        this.stockQty = variantPayload.stockQty
                        this.optionMap = variantPayload.optionCombination
                    }
                    variantRepository.save(variant)
                }
            }
        }
    }

    private fun createStandardProductOptionsWithValues(productId: Long) {
        val colorOption = ProductOption().apply {
            this.productId = productId
            this.name = "color"
            this.position = 0
        }
        val sizeOption = ProductOption().apply {
            this.productId = productId
            this.name = "size"
            this.position = 1
        }

        val savedOptions = productOptionRepository.saveAll(listOf(colorOption, sizeOption))
        val colorOptionId = savedOptions[0].id
        val sizeOptionId = savedOptions[1].id

        val colorValues = getStandardColors().mapIndexed { position, color ->
            ProductOptionValue().apply {
                this.productOptionId = colorOptionId
                this.value = color
                this.position = position
            }
        }

        val sizeValues = getStandardSizes().mapIndexed { position, size ->
            ProductOptionValue().apply {
                this.productOptionId = sizeOptionId
                this.value = size
                this.position = position
            }
        }

        productOptionValueRepository.saveAll(colorValues + sizeValues)
    }

    private fun getStandardColors(): List<String> {
        return listOf("Black", "White", "Red", "Blue", "Green", "Beige", "Dark Blue")
    }

    private fun getStandardSizes(): List<String> {
        return listOf("XXS", "XS", "S", "M", "L", "XL", "XXL")
    }
    fun getExistingSkus(): SkuListResponse {
        val skus = variantRepository.findAllSkus()
        return SkuListResponse(skus)
    }

    @Transactional
    fun updateProduct(productId: Long, request: UpdateProductPayload): Product {
        val product = productRepository.findById(productId).orElseThrow {
            RuntimeException("Product not found")
        }

        product.name = request.name
        product.description = request.description
        product.hasVariants = request.hasVariants

        if (!request.hasVariants && request.productSku != null) {
            variantRepository.deleteByProductId(productId)
            val singleVariant = Variant().apply {
                this.productId = productId
                this.sku = request.productSku
                this.priceCents = request.productPriceCents ?: 0
                this.stockQty = request.productStockQty ?: 0
                this.optionMap = emptyMap()
            }
            variantRepository.save(singleVariant)
        } else {
            processVariantUpdates(productId, request.variants)
        }

        return productRepository.save(product)
    }

    private fun processVariantUpdates(productId: Long, variantUpdates: List<UpdateVariantPayload>) {
        variantUpdates.forEach { variantUpdate ->
            when {
                variantUpdate.deleted && variantUpdate.id != null -> {
                    variantRepository.deleteById(variantUpdate.id)
                }
                variantUpdate.id != null && !variantUpdate.deleted -> {
                    val existingVariant = variantRepository.findById(variantUpdate.id).orElse(null)
                    if (existingVariant != null) {
                        existingVariant.stockQty = variantUpdate.stockQty
                        variantRepository.save(existingVariant)
                    }
                }
                variantUpdate.id == null && !variantUpdate.deleted -> {
                    val newVariant = Variant().apply {
                        this.productId = productId
                        this.sku = variantUpdate.sku
                        this.priceCents = variantUpdate.priceCents
                        this.stockQty = variantUpdate.stockQty
                        this.optionMap = variantUpdate.optionCombination
                    }
                    variantRepository.save(newVariant)
                }
            }
        }
    }
    // to show the options on the frontend (we are saving on creating the product)
    fun getStandardProductOptions(): List<ProductOptionPayload> {
        return listOf(
            ProductOptionPayload(
                name = "color",
                values = getStandardColors()
            ),
            ProductOptionPayload(
                name = "size",
                values = getStandardSizes()
            )
        )
    }
    @Transactional
    fun deleteProduct(productId: Long) {
        val product = productRepository.findById(productId).orElse(null)
            ?: throw RuntimeException("Product not found")
        productRepository.deleteById(productId)
    }

}