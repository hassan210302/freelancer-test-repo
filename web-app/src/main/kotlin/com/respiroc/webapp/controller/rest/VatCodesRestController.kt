package com.respiroc.webapp.controller.rest

import com.respiroc.ledger.application.VatService
import com.respiroc.ledger.domain.model.VatCode
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class VatCodesRestController(
    private val vatService: VatService
) {
    @GetMapping("/api/vat-codes")
    fun allVatCodes(): List<VatCode> {
        return vatService.findAllVatCodes().sortedBy { it.code }.toList()
    }
}