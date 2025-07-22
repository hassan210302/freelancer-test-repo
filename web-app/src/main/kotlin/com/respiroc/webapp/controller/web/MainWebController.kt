package com.respiroc.webapp.controller.web

import com.respiroc.ledger.application.ExpenseService
import com.respiroc.webapp.controller.BaseController
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class MainWebController(
    private val expenseService: ExpenseService  // Add this injection
) : BaseController() {

    @GetMapping("/")
    fun home(): String {
        return if (isUserLoggedIn()) {
            "redirect:/dashboard"
        } else {
            "redirect:/auth/login"
        }
    }

    @GetMapping("/dashboard")
    fun dashboard(model: Model): String {
        try {
            // Get expense data for dashboard widgets
            val totalExpenseAmount = expenseService.getTotalExpenseAmount()
            val allExpenses = expenseService.findAllExpensesByTenant()
            val recentExpenses = allExpenses.take(5) // Last 5 expenses
            val activeCategoryCount = expenseService.findAllActiveCategories().size

            // Add expense data to model
            model.addAttribute("totalExpenseAmount", totalExpenseAmount)
            model.addAttribute("recentExpenses", recentExpenses)
            model.addAttribute("recentExpenseCount", recentExpenses.size)
            model.addAttribute("activeCategoryCount", activeCategoryCount)

        } catch (e: Exception) {
            // Fallback values if expense service fails
            model.addAttribute("totalExpenseAmount", 0.00)
            model.addAttribute("recentExpenses", emptyList<Any>())
            model.addAttribute("recentExpenseCount", 0)
            model.addAttribute("activeCategoryCount", 6)
        }

        addCommonAttributesForCurrentTenant(model, "Dashboard")
        return "dashboard/index"
    }
}