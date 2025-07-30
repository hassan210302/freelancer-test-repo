package com.respiroc.webapp.controller.rest

import com.respiroc.ledger.application.AccountService
import com.respiroc.ledger.domain.model.Account
import com.respiroc.supplier.application.SupplierService
import com.respiroc.supplier.domain.model.Supplier
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SuppliersRestController(
    private val supplierService: SupplierService
) {
    @GetMapping("/api/suppliers")
    fun allSuppliers(): List<Supplier> {
        return supplierService.findAllSupplier().sortedBy { it.company?.name }.toList()
    }
}