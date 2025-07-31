package com.respiroc.webapp.controller.web

import com.respiroc.product.application.ProductService
import com.respiroc.product.application.payload.CreateProductPayload
import com.respiroc.product.application.payload.SkuListResponse
import com.respiroc.product.application.payload.UpdateProductPayload
import com.respiroc.util.exception.ResourceNotFoundException
import com.respiroc.webapp.controller.BaseController
import com.respiroc.webapp.controller.response.Callout
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping(value = ["/product"])
class ProductWebController(
    private val productService: ProductService
) : BaseController() {

    @GetMapping(value = [])
    fun product(): String {
        return "redirect:/product/overview"
    }

    @GetMapping(value = ["/overview"])
    fun overview(model: Model): String {
        val products = productService.getAllProductsWithDetailsByTenant()

        addCommonAttributesForCurrentTenant(model, "Product Overview")
        model.addAttribute("products", products)
        return "product/overview"
    }

    @GetMapping(value = ["/new"])
    fun newProduct(model: Model): String {
        addCommonAttributesForCurrentTenant(model, "New Product")
        model.addAttribute("productOptions", productService.getStandardProductOptions())
        return "product/new"
    }

    @GetMapping(value = ["/{id}"])
    fun productDetail(@PathVariable id: Long, model: Model): String {
        val product = productService.getProductWithDetailsByIdAndTenant(id, tenantId())
            ?: throw ResourceNotFoundException("Product not found")

        addCommonAttributesForCurrentTenant(model, "Product Details")
        model.addAttribute("product", product)
        return "product/detail"
    }
}

@Controller
@RequestMapping("/htmx/product")
class ProductHTMXController(
    private val productService: ProductService
) : BaseController() {

    @GetMapping("/overview")
    @HxRequest
    fun overviewHTMX(model: Model): String {
        val products = productService.getAllProductsWithDetailsByTenant()
        model.addAttribute("products", products)
        model.addAttribute(userAttributeName, springUser())
        return "product/overview :: tableContent"
    }

    @GetMapping("/skus")
    @ResponseBody
    fun getExistingSkus(): SkuListResponse {
        return productService.getExistingSkus()
    }

    @PostMapping("/create")
    @HxRequest
    fun createProduct(
        @Valid @ModelAttribute createRequest: CreateProductPayload,
        bindingResult: BindingResult,
        model: Model,
        response: HttpServletResponse
    ): String {

        if (bindingResult.hasErrors()) {
            val errorMessages = bindingResult.allErrors.joinToString(", ") { it.defaultMessage ?: "Validation error" }
            model.addAttribute("callout", Callout.Error(errorMessages))
            return "fragments/r-callout"
        }

        try {
            productService.createProduct(createRequest)
            response.setHeader("HX-Redirect", "/product/overview")
            return "fragments/empty"
        } catch (e: Exception) {
            model.addAttribute("callout", Callout.Error("Failed to create product: ${e.message}"))
            return "fragments/r-callout"
        }
    }

    @DeleteMapping("/{id}")
    @HxRequest
    fun deleteProduct(
        @PathVariable id: Long,
        model: Model,
        response: HttpServletResponse
    ): String {
        try {
            productService.deleteProduct(id)
            val products = productService.getAllProductsWithDetailsByTenant()
            model.addAttribute("products", products)
            model.addAttribute(userAttributeName, springUser())
            return "product/overview :: tableContent"
        } catch (e: Exception) {
            model.addAttribute("callout", Callout.Error("Failed to delete product: ${e.message}"))
            return "fragments/r-callout"
        }
    }
    @PostMapping("/{id}/update")
    @HxRequest
    fun updateProduct(
        @PathVariable id: Long,
        @Valid @ModelAttribute updateRequest: UpdateProductPayload,
        bindingResult: BindingResult,
        model: Model,
        response: HttpServletResponse
    ): String {

        if (bindingResult.hasErrors()) {
            val errorMessages = bindingResult.allErrors.joinToString(", ") { it.defaultMessage ?: "Validation error" }
            model.addAttribute("callout", Callout.Error(errorMessages))
            return "fragments/r-callout"
        }

        try {
            productService.updateProduct(id, updateRequest)
            response.setHeader("HX-Redirect", "/product/${id}")
            return "fragments/empty"
        } catch (e: Exception) {
            model.addAttribute("callout", Callout.Error("Failed to update product: ${e.message}"))
            return "fragments/r-callout"
        }
    }
}