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
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.LocalDate
import com.respiroc.ledger.application.payload.ExpensePayload

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

    @GetMapping("/edit/{id}")
    fun editExpense(@PathVariable id: Long, model: Model): String {
        val expense = expenseService.findExpenseByIdForEdit(id)
            ?: return "redirect:/expenses/overview"
        
        val categories = expenseService.findAllActiveCategories()
        val firstCost = expense.costs.firstOrNull()

        addCommonAttributesForCurrentTenant(model, "Edit Expense")
        model.addAttribute("categories", categories)
        model.addAttribute("expense", expense)
        model.addAttribute("expenseDate", expense.expenseDate)
        model.addAttribute("paymentTypes", PaymentType.entries)
        
        if (firstCost != null) {
            model.addAttribute("costTitle", firstCost.title)
            model.addAttribute("costAmount", firstCost.amount)
            model.addAttribute("costVat", firstCost.vat)
            model.addAttribute("costPaymentType", firstCost.paymentType)
            model.addAttribute("costChargeable", firstCost.chargeable)
        } else {
            model.addAttribute("costTitle", "")
            model.addAttribute("costAmount", "")
            model.addAttribute("costVat", 0)
            model.addAttribute("costPaymentType", "")
            model.addAttribute("costChargeable", false)
        }
        
        return "expenses/edit"
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

    @PostMapping("/update/{id}")
    fun updateExpense(
        @PathVariable id: Long,
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

            expenseService.updateExpense(id, payload)
            "redirect:/expenses/overview"
        } catch (e: Exception) {
            val categories = expenseService.findAllActiveCategories()
            addCommonAttributesForCurrentTenant(model, "Edit Expense")
            model.addAttribute("categories", categories)
            model.addAttribute("paymentTypes", PaymentType.entries)
            model.addAttribute("error", "Failed to update expense: ${e.message}")
            
            val expenseForForm = ExpensePayload(
                id = id,
                title = title,
                description = description,
                expenseDate = expenseDate,
                status = com.respiroc.ledger.domain.model.ExpenseStatus.OPEN,
                category = "",
                amount = costAmount,
                costs = emptyList(),
                createdBy = "",
                attachmentCount = 0
            )
            
            model.addAttribute("expense", expenseForForm)
            model.addAttribute("expenseDate", expenseDate)
            model.addAttribute("costTitle", costTitle)
            model.addAttribute("costAmount", costAmount)
            model.addAttribute("costVat", costVat)
            model.addAttribute("costPaymentType", costPaymentType)
            model.addAttribute("costChargeable", costChargeable)
            
            "expenses/edit"
        }
    }

    @PostMapping("/delete/{id}")
    fun deleteExpense(@PathVariable id: Long): String {
        return try {
            expenseService.deleteExpense(id)
            "redirect:/expenses/overview"
        } catch (e: Exception) {
            "redirect:/expenses/overview?error=Failed to delete expense"
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

    @PostMapping("/upload")
    fun uploadFiles(
        @RequestParam("expenseId") expenseId: Long,
        model: Model
    ): String {
        return try {
            // Simulate file upload - create a dummy attachment to test the flow
            val attachments = listOf(ByteArray(1024)) // Dummy file data
            expenseService.addAttachmentsToExpense(expenseId, attachments)
            
            model.addAttribute("message", "File uploaded successfully")
            model.addAttribute("messageType", "success")
            "fragments/message :: messageFragment"
        } catch (e: Exception) {
            model.addAttribute("message", "Error uploading file: ${e.message}")
            model.addAttribute("messageType", "error")
            "fragments/message :: messageFragment"
        }
    }
}