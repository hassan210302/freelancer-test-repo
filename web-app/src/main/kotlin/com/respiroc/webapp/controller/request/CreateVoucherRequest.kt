package com.respiroc.webapp.controller.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate

data class CreateVoucherRequest(
    @field:NotNull(message = "Voucher date is required")
    val voucherDate: LocalDate = LocalDate.now(),

    val voucherDescription: String? = null,

    @field:Valid
    val postingLines: List<PostingLine?> = emptyList()
) {
    fun getValidPostingLines(): List<PostingLine> {
        return postingLines.filterNotNull()
            .filter { it.amount != null && it.amount > BigDecimal.ZERO }
            .filter { it.getAccountNumber().isNotBlank() }
            .map { it.roundAmountToTwoDecimals() }
    }
}

// Single posting line - represents either debit or credit in a journal entry
data class PostingLine(
    val debitAccount: String = "",
    val creditAccount: String = "",
    val amount: BigDecimal? = null,
    val currency: String = "NOK",
    val postingDate: LocalDate = LocalDate.now(),
    val description: String? = null,
    val debitVatCode: String? = null,
    val creditVatCode: String? = null,
    val supplier: Long? = null,
    val rowNumber: Int = 0
) {
    fun getAccountNumber(): String {
        return debitAccount.ifBlank { creditAccount }
    }

    fun getAccountType(): String {
        return if (debitAccount.isNotBlank()) "debit" else "credit"
    }

    fun getSignedAmount(): BigDecimal {
        val amt = amount ?: BigDecimal.ZERO
        return if (getAccountType() == "credit") amt.negate() else amt
    }

    fun getVatCode(): String? {
        val vatCodeValue = if (debitAccount.isNotBlank()) debitVatCode else creditVatCode
        return extractActualVatCode(vatCodeValue)
    }

    private fun extractActualVatCode(vatCodeValue: String?): String? {
        if (vatCodeValue.isNullOrBlank()) return null

        val trimmed = vatCodeValue.trim()

        return if (trimmed.contains("(")) {
            trimmed.substringBefore("(").trim().takeIf { it.isNotBlank() }
        } else {
            trimmed.takeIf { it.isNotBlank() }
        }
    }

    fun roundAmountToTwoDecimals(): PostingLine {
        val roundedAmount = amount?.setScale(2, java.math.RoundingMode.HALF_UP)
        return this.copy(amount = roundedAmount)
    }
}