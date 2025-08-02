package com.respiroc.webapp.controller.web

import com.respiroc.ledger.application.ExpenseService
import com.respiroc.ledger.application.payload.CreateExpensePayload
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
    fun overview(model: Model): String {
        val expenses = expenseService.findAllExpensesByTenant()

        addCommonAttributesForCurrentTenant(model, "Expenses")
        model.addAttribute("expenses", expenses)
        return "expenses/overview"
    }

    @GetMapping("/new")
    fun newExpense(model: Model): String {
        val categories = expenseService.findAllActiveCategories()

        addCommonAttributesForCurrentTenant(model, "New Expense")
        model.addAttribute("categories", categories)
        model.addAttribute("expenseDate", LocalDate.now())
        return "expenses/new"
    }

    @PostMapping("/create")
    fun createExpense(
        @RequestParam categoryId: Long,
        @RequestParam amount: BigDecimal,
        @RequestParam description: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) expenseDate: LocalDate,
        model: Model
    ): String {
        return try {
            val payload = CreateExpensePayload(
                categoryId = categoryId,
                amount = amount,
                description = description,
                expenseDate = expenseDate
            )

            expenseService.createExpense(payload, springUser().username ?: "system")
            "redirect:/expenses/overview"
        } catch (e: Exception) {
            val categories = expenseService.findAllActiveCategories()
            addCommonAttributesForCurrentTenant(model, "New Expense")
            model.addAttribute("categories", categories)
            model.addAttribute("error", "Failed to create expense: ${e.message}")
            "expenses/new"
        }
    }
}