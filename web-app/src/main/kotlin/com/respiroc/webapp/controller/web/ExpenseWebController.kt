package com.respiroc.webapp.controller.web

import com.respiroc.ledger.application.ExpenseService
import com.respiroc.ledger.application.payload.CreateExpensePayload
import com.respiroc.ledger.application.payload.CreateCostPayload
import com.respiroc.ledger.domain.model.PaymentType
import com.respiroc.webapp.controller.BaseController
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate

@Controller
@RequestMapping("/expenses")
class ExpenseWebController(
    private val expenseService: ExpenseService
) : BaseController() {

    @GetMapping
    fun expenses(): String {
        return "redirect:/expenses/overview"
    }

    @GetMapping("/overview")
    fun overview(
        @RequestParam(name = "startDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startDate: LocalDate?,
        @RequestParam(name = "endDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        endDate: LocalDate?,
        model: Model
    ): String {
        val effectiveStartDate = startDate ?: LocalDate.now().withDayOfMonth(1)
        val effectiveEndDate = endDate ?: LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth())
        val expenses = expenseService.findExpensesByDate(startDate = effectiveStartDate, endDate = effectiveEndDate)

        addCommonAttributesForCurrentTenant(model, "Expenses")
        model.addAttribute("expenses", expenses)
        model.addAttribute("startDate", effectiveStartDate)
        model.addAttribute("endDate", effectiveEndDate)
        return "expenses/overview"
    }

    @GetMapping("/new")
    fun newExpense(model: Model): String {
        val categories = expenseService.findAllActiveCategories()

        addCommonAttributesForCurrentTenant(model, "New Expense")
        model.addAttribute("categories", categories)
        model.addAttribute("expenseDate", LocalDate.now())
        model.addAttribute("paymentTypes", PaymentType.entries)
        return "expenses/new"
    }

    @PostMapping("/create")
    fun createExpense(
        @RequestParam title: String,
        @RequestParam description: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) expenseDate: LocalDate,
        @RequestParam categoryId: Long,
        @RequestParam costTitle: String,
        @RequestParam costAmount: BigDecimal,
        @RequestParam costVat: Int,
        @RequestParam costPaymentType: PaymentType,
        @RequestParam(defaultValue = "false") costChargeable: Boolean,
        model: Model
    ): String {
        return try {
            val costPayload = CreateCostPayload(
                title = costTitle,
                date = expenseDate,
                amount = costAmount,
                vat = costVat,
                currency = "NOK",
                paymentType = costPaymentType,
                chargeable = costChargeable
            )

            val payload = CreateExpensePayload(
                title = title,
                description = description,
                expenseDate = expenseDate,
                categoryId = categoryId,
                costs = listOf(costPayload)
            )

            expenseService.createExpense(payload, springUser().username ?: "system")
            "redirect:/expenses/overview"
        } catch (e: Exception) {
            val categories = expenseService.findAllActiveCategories()
            addCommonAttributesForCurrentTenant(model, "New Expense")
            model.addAttribute("categories", categories)
            model.addAttribute("paymentTypes", PaymentType.entries)
            model.addAttribute("error", "Failed to create expense: ${e.message}")
            "expenses/new"
        }
    }
}

@Controller
@RequestMapping("htmx/expense")
class ExpenseHTMXController(
    private val expenseService: ExpenseService
) : BaseController() {
    @GetMapping("/overview")
    fun overviewHTMX(
        @RequestParam(name = "startDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startDate: LocalDate?,
        @RequestParam(name = "endDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        endDate: LocalDate?,
        model: Model
    ): String {
        val effectiveStartDate = startDate ?: LocalDate.now().withDayOfMonth(1)
        val effectiveEndDate = endDate ?: LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth())
        val expenses = expenseService.findExpensesByDate(startDate = effectiveStartDate, endDate = effectiveEndDate)

        addCommonAttributesForCurrentTenant(model, "Expenses")
        model.addAttribute("expenses", expenses)
        model.addAttribute("startDate", effectiveStartDate)
        model.addAttribute("endDate", effectiveEndDate)
        return "expenses/overview :: tableContent"
    }
}