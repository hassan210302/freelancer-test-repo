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
import com.respiroc.ledger.domain.model.ExpenseStatus
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest

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
        @RequestParam(name = "status", required = false)
        status: ExpenseStatus?,
        model: Model
    ): String {
        val effectiveStartDate = startDate ?: LocalDate.now().withDayOfMonth(1)
        val effectiveEndDate = endDate ?: LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth())
        val expenses = expenseService.findExpensesWithFilters(
            startDate = effectiveStartDate,
            endDate = effectiveEndDate,
            status = status
        )

        addCommonAttributesForCurrentTenant(model, "Expenses")
        model.addAttribute("expenses", expenses)
        model.addAttribute("startDate", effectiveStartDate)
        model.addAttribute("endDate", effectiveEndDate)
        model.addAttribute("selectedStatus", status?.name?.lowercase()?.replaceFirstChar { it.uppercase() })
        model.addAttribute("expenseStatuses", ExpenseStatus.entries)
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
        val attachments = expenseService.findExpenseAttachments(id)

        addCommonAttributesForCurrentTenant(model, "Edit Expense")
        model.addAttribute("categories", categories)
        model.addAttribute("expense", expense)
        model.addAttribute("expenseDate", expense.expenseDate)
        model.addAttribute("paymentTypes", PaymentType.entries)
        model.addAttribute("attachments", attachments)
        
        return "expenses/edit"
    }

    @PostMapping("/create", consumes = ["multipart/form-data"])
    fun createExpense(
        @RequestParam title: String,
        @RequestParam description: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) expenseDate: LocalDate,
        @RequestParam categoryId: Long,
        @RequestParam(name = "costTitles", required = false) costTitles: Array<String>?,
        @RequestParam(name = "costAmounts", required = false) costAmounts: Array<String>?,
        @RequestParam(name = "costVats", required = false) costVats: Array<String>?,
        @RequestParam(name = "costPaymentTypes", required = false) costPaymentTypes: Array<String>?,
        @RequestParam(name = "costChargeables", required = false) costChargeables: Array<String>?,
        @RequestParam(name = "attachments", required = false) attachments: List<MultipartFile>?,
        model: Model
    ): String {
        return try {
            val costs = buildCostsFromArrays(
                costTitles, costAmounts, costVats, costPaymentTypes, costChargeables, expenseDate
            )
            
            val payload = CreateExpensePayload(
                title = title,
                description = description,
                expenseDate = expenseDate,
                categoryId = categoryId,
                costs = costs
            )

            val created = expenseService.createExpense(payload, springUser().username ?: "system")

            if (!attachments.isNullOrEmpty()) {
                val fileData = attachments.map { it.bytes }
                val filenames = attachments.map { it.originalFilename ?: "attachment_${System.currentTimeMillis()}.pdf" }
                expenseService.addAttachmentsToExpense(created.id, fileData, filenames)
            }
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

    @PostMapping("/update/{id}", consumes = ["multipart/form-data"])
    fun updateExpense(
        @PathVariable id: Long,
        @RequestParam title: String,
        @RequestParam description: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) expenseDate: LocalDate,
        @RequestParam categoryId: Long,
        @RequestParam(name = "costTitles", required = false) costTitles: Array<String>?,
        @RequestParam(name = "costAmounts", required = false) costAmounts: Array<String>?,
        @RequestParam(name = "costVats", required = false) costVats: Array<String>?,
        @RequestParam(name = "costPaymentTypes", required = false) costPaymentTypes: Array<String>?,
        @RequestParam(name = "costChargeables", required = false) costChargeables: Array<String>?,
        @RequestParam(name = "attachments", required = false) attachments: List<MultipartFile>?,
        model: Model
    ): String {
        return try {
            val costs = buildCostsFromArrays(
                costTitles, costAmounts, costVats, costPaymentTypes, costChargeables, expenseDate
            )
            
            val payload = CreateExpensePayload(
                title = title,
                description = description,
                expenseDate = expenseDate,
                categoryId = categoryId,
                costs = costs
            )

            expenseService.updateExpense(id, payload)

            processAttachments(id, attachments)
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
                amount = BigDecimal.ZERO,
                costs = emptyList(),
                createdBy = "",
                attachmentCount = 0
            )
            
            model.addAttribute("expense", expenseForForm)
            model.addAttribute("expenseDate", expenseDate)
            
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

    @PostMapping("/update-status/{id}")
    fun updateExpenseStatus(@PathVariable id: Long): String {
        expenseService.updateExpenseStatus(id)
        return "redirect:/expenses/edit/$id"
    }

    private fun processAttachments(expenseId: Long, attachments: List<MultipartFile>?) {
        if (!attachments.isNullOrEmpty() && attachments.any { !it.isEmpty && it.size > 0 }) {
            val validAttachments = attachments.filter { !it.isEmpty && it.size > 0 }
            val fileData = validAttachments.map { it.bytes }
            val filenames = validAttachments.map { it.originalFilename ?: "attachment_${System.currentTimeMillis()}.pdf" }
            expenseService.addAttachmentsToExpense(expenseId, fileData, filenames)
        }
    }

    private fun buildCostsFromArrays(
        costTitles: Array<String>?,
        costAmounts: Array<String>?,
        costVats: Array<String>?,
        costPaymentTypes: Array<String>?,
        costChargeables: Array<String>?,
        expenseDate: LocalDate
    ): List<CreateCostPayload> {
        if (costTitles.isNullOrEmpty()) return emptyList()
        
        return costTitles.mapIndexedNotNull { index, title ->
            if (title.isBlank()) return@mapIndexedNotNull null
            
            val amountStr = costAmounts?.getOrNull(index) ?: "0"
            val vatStr = costVats?.getOrNull(index) ?: "0"
            val paymentTypeStr = costPaymentTypes?.getOrNull(index) ?: ""
            val chargeableStr = costChargeables?.getOrNull(index) ?: "false"
            
            try {
                val amount = BigDecimal(amountStr)
                val vat = vatStr.toIntOrNull() ?: 0
                val paymentType = PaymentType.valueOf(paymentTypeStr)
                val chargeable = chargeableStr.toBoolean()
                
                CreateCostPayload(
                    title = title,
                    date = expenseDate,
                    amount = amount,
                    vat = vat,
                    currency = "NOK",
                    paymentType = paymentType,
                    chargeable = chargeable
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
@Controller
@RequestMapping("htmx/expense")
class ExpenseHTMXController(
    private val expenseService: ExpenseService
) : BaseController() {
    @GetMapping("/overview")
    @HxRequest
    fun overviewHTMX(
        @RequestParam(name = "startDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startDate: LocalDate?,
        @RequestParam(name = "endDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        endDate: LocalDate?,
        @RequestParam(name = "currentStatus", required = false)
        currentStatus: String?,
        model: Model
    ): String {
        println("DEBUG: HTMX request received")
        println("DEBUG: startDate = $startDate")
        println("DEBUG: endDate = $endDate")
        println("DEBUG: currentStatus = $currentStatus")
        
        val effectiveStartDate = startDate ?: LocalDate.now().withDayOfMonth(1)
        val effectiveEndDate = endDate ?: LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth())

        var statusEnum: ExpenseStatus? = null
        when (currentStatus) {
            "Open" -> statusEnum = ExpenseStatus.OPEN
            "Delivered" -> statusEnum = ExpenseStatus.DELIVERED
            "Approved" -> statusEnum = ExpenseStatus.APPROVED
            "NULL" -> statusEnum = null
            null -> statusEnum = null
        }
        
        println("DEBUG: effectiveStartDate = $effectiveStartDate")
        println("DEBUG: effectiveEndDate = $effectiveEndDate")
        println("DEBUG: statusEnum = $statusEnum")

        val expenses = expenseService.findExpensesWithFilters(
            startDate = effectiveStartDate,
            endDate = effectiveEndDate,
            status = statusEnum
        )
        
        println("DEBUG: found ${expenses.size} expenses")

        model.addAttribute("expenses", expenses)
        model.addAttribute("startDate", effectiveStartDate)
        model.addAttribute("endDate", effectiveEndDate)
        model.addAttribute("selectedStatus", currentStatus)
        model.addAttribute("expenseStatuses", ExpenseStatus.entries)
        return "expenses/overview :: tableContent"
    }

    @PostMapping("/upload")
    fun uploadFiles(
        @RequestParam("expenseId") expenseId: Long,
        model: Model
    ): String {
        return try {
            val attachments = listOf(ByteArray(1024))
            val filenames = listOf("dummy_file.pdf")
            expenseService.addAttachmentsToExpense(expenseId, attachments, filenames)
            
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