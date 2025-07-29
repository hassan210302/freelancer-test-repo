package com.respiroc.invoice.application

import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.respiroc.invoice.domain.entity.Invoice
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*

class InvoicePdfGenerator {

    companion object {
        private const val FONT_SIZE_NORMAL = 10f
        private const val FONT_SIZE_SMALL = 9f
        private const val FONT_SIZE_HEADER = 14f

        private val LINE_HEADERS = listOf(
            "Description", "Quantity", "Unit Price\n(excl. VAT)", "Discount",
            "VAT Rate", "Amount\n(excl. VAT)", "VAT", "Amount\n(incl. VAT)"
        )
    }

    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val regularFont: PdfFont by lazy { PdfFontFactory.createFont(StandardFonts.HELVETICA) }
    private val boldFont: PdfFont by lazy { PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD) }

    private val moneyFormatter: NumberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }

    fun generate(invoice: Invoice): ByteArray {
        val output = ByteArrayOutputStream()
        val pdf = PdfDocument(PdfWriter(output))
        val document = Document(pdf, PageSize.A4).apply {
            setMargins(40f, 40f, 40f, 40f)
        }

        pdf.documentInfo
            .setTitle("Invoice ${invoice.number}")
            .author = invoice.tenant.company.name

        addHeader(document, invoice)
        addCustomerInfo(document, invoice)
        addInvoiceDetails(document, invoice)
        addInvoiceLines(document, invoice)
        addSummaryRows(document, invoice)

        document.close()
        return output.toByteArray()
    }

    private fun addHeader(document: Document, invoice: Invoice) {
        val company = invoice.tenant.company
        val address = company.address!!

        val addressLine = listOfNotNull(
            address.addressPart1,
            address.addressPart2,
            address.postalCode,
            address.city,
            getCountryNameFromCode(address.countryIsoCode)
        ).joinToString(", ")

        document.add(
            Paragraph(company.name)
                .setFont(boldFont)
                .setFontSize(FONT_SIZE_HEADER)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(2f)
        )

        document.add(Table(1).apply {
            width = UnitValue.createPercentValue(100f)
            setBorder(Border.NO_BORDER)
            addCell(emptyCell().setBorderTop(SolidBorder(ColorConstants.BLACK, 2f)).setHeight(1f))
        })

        document.add(
            Paragraph(addressLine)
                .setFont(regularFont)
                .setFontSize(FONT_SIZE_NORMAL)
                .setTextAlignment(TextAlignment.RIGHT)
                .setHorizontalAlignment(HorizontalAlignment.RIGHT)
                .setMarginBottom(0f)
        )

        val labels = listOf("Company Number:")
        val values = listOf("${company.countryCode} ${company.organizationNumber.chunked(3).joinToString(" ")} MVA")

        val fontSize = FONT_SIZE_NORMAL
        val maxLabelWidth = maxTextWidth(labels, boldFont, fontSize)
        val maxValueWidth = maxTextWidth(values, regularFont, fontSize)
        val padding = 8f

        val table = Table(
            UnitValue.createPointArray(
                floatArrayOf(maxLabelWidth + padding, maxValueWidth + padding)
            )
        ).apply {
            setWidth(maxLabelWidth + maxValueWidth + (padding * 2))
            setMarginTop(10f)
            setBorder(Border.NO_BORDER)
            setHorizontalAlignment(HorizontalAlignment.RIGHT)
        }

        table.addCell(createCell(labels[0], boldFont, fontSize, TextAlignment.LEFT))
        table.addCell(createCell(values[0], regularFont, fontSize, TextAlignment.LEFT))
        document.add(table)
    }

    private fun addCustomerInfo(document: Document, invoice: Invoice) {
        val address = invoice.customer.let {
            if (it.isPrivateCustomer()) it.person!!.address else it.company!!.address
        }

        val details = buildList {
            add(invoice.customer.getName())
            address?.addressPart1?.takeIf { it.isNotBlank() }?.let { add(it) }
            address?.addressPart2?.takeIf { it.isNotBlank() }?.let { add(it) }
            add("${address?.postalCode ?: ""} ${address?.city}".trim())
        }

        details.forEach {
            document.add(
                Paragraph(it)
                    .setFont(regularFont)
                    .setFontSize(FONT_SIZE_NORMAL)
                    .setMarginBottom(0.2f)
                    .setMultipliedLeading(1f)
            )
        }
    }

    private fun addInvoiceDetails(document: Document, invoice: Invoice) {
        val table = Table(UnitValue.createPointArray(floatArrayOf(100f, 120f)))
            .setWidth(UnitValue.createPercentValue(30f))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setBorder(Border.NO_BORDER)

        fun label(text: String) = createCell(text, regularFont, FONT_SIZE_NORMAL, TextAlignment.LEFT)
        fun value(text: String) = createCell(text, regularFont, FONT_SIZE_NORMAL, TextAlignment.RIGHT)

        table.addCell(
            Cell(1, 2)
                .add(Paragraph("Invoice").setFont(boldFont).setFontSize(18f))
                .setTextAlignment(TextAlignment.LEFT)
                .setBorder(Border.NO_BORDER)
                .setPaddingBottom(2f)
        )

        table.addCell(label("Invoice No.:"))
        table.addCell(value(invoice.number))
        table.addCell(label("Invoice Date:"))
        table.addCell(value(invoice.issueDate.format(dateFormat)))
        table.addCell(emptyCell(2).setPaddingTop(4f))
        table.addCell(
            Cell(1, 2)
                .add(Paragraph("Payment Information").setFont(boldFont))
                .setBorder(Border.NO_BORDER)
                .setPaddingBottom(2f)
        )
        table.addCell(label("Due Date:"))
        table.addCell(value(invoice.dueDate?.format(dateFormat) ?: ""))

        document.add(table)
    }

    private fun addInvoiceLines(document: Document, invoice: Invoice) {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(3f, 1f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorderBottom(SolidBorder(0.25f))
            .setMarginTop(50f)

        LINE_HEADERS.forEachIndexed { index, text ->
            table.addHeaderCell(
                createCell(text, boldFont, FONT_SIZE_SMALL, TextAlignment.CENTER)
                    .setBorderTop(Border.NO_BORDER)
                    .setBorderLeft(Border.NO_BORDER)
                    .setBorderRight(Border.NO_BORDER)
                    .setBorderBottom(SolidBorder(0.25f))
                    .setPadding(3f)
                    .setTextAlignment(if (index == 0) TextAlignment.LEFT else TextAlignment.RIGHT)
            )
        }

        invoice.lines.forEach { line ->
            table.addCell(createCell(line.itemName, regularFont, FONT_SIZE_SMALL, TextAlignment.LEFT))
            table.addCell(createCell(line.quantity.toString(), regularFont, FONT_SIZE_SMALL, TextAlignment.RIGHT))
            table.addCell(createCell(formatMoney(line.unitPrice), regularFont, FONT_SIZE_SMALL, TextAlignment.RIGHT))
            table.addCell(createCell("${line.discount} %", regularFont, FONT_SIZE_SMALL, TextAlignment.RIGHT))
            table.addCell(createCell("${line.vatRate} %", regularFont, FONT_SIZE_SMALL, TextAlignment.RIGHT))
            table.addCell(
                createCell(
                    formatMoney(line.subTotal - line.discountAmount),
                    regularFont,
                    FONT_SIZE_SMALL,
                    TextAlignment.RIGHT
                )
            )
            table.addCell(createCell(formatMoney(line.vst), regularFont, FONT_SIZE_SMALL, TextAlignment.RIGHT))
            table.addCell(createCell(formatMoney(line.totalAmount), regularFont, FONT_SIZE_SMALL, TextAlignment.RIGHT))
        }

        document.add(table)
    }

    private fun addSummaryRows(document: Document, invoice: Invoice) {
        val totals = calculateTotals(invoice)

        val table = Table(UnitValue.createPercentArray(floatArrayOf(4f, 1f, 2f, 2f, 2f, 2f, 2f, 2f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginTop(5f)

        if (invoice.lines.size > 1) {
            table.addCell(createCell("Sum", boldFont, FONT_SIZE_SMALL, TextAlignment.LEFT, colspan = 5))
            table.addCell(
                createCell(
                    formatMoney(totals.subtotal - totals.discount),
                    regularFont,
                    FONT_SIZE_SMALL,
                    TextAlignment.RIGHT
                )
            )
            table.addCell(createCell(formatMoney(totals.vat), regularFont, FONT_SIZE_SMALL, TextAlignment.RIGHT))
            table.addCell(createCell(formatMoney(totals.total), regularFont, FONT_SIZE_SMALL, TextAlignment.RIGHT))
            table.addCell(emptyCell(8).setBorderTop(SolidBorder(ColorConstants.BLACK, 0.25f)))
        }

        table.addCell(createCell("Payable", boldFont, FONT_SIZE_SMALL, TextAlignment.LEFT, colspan = 6))
        table.addCell(
            createCell(
                "${invoice.currencyCode} ${formatMoney(totals.total)}",
                boldFont,
                FONT_SIZE_SMALL,
                TextAlignment.RIGHT,
                colspan = 2
            )
        )
        table.addCell(emptyCell(8).setBorderTop(SolidBorder(ColorConstants.BLACK, 2f)).setHeight(1f))

        document.add(table)
    }

    private fun calculateTotals(invoice: Invoice): InvoiceTotals {
        val subtotal = invoice.lines.sumOf { it.subTotal }
        val discount = invoice.lines.sumOf { it.discountAmount }
        val vat = invoice.lines.sumOf { it.vst }
        val total = invoice.totalAmount
        return InvoiceTotals(subtotal, discount, vat, total)
    }

    private data class InvoiceTotals(
        val subtotal: BigDecimal,
        val discount: BigDecimal,
        val vat: BigDecimal,
        val total: BigDecimal
    )

    private fun createCell(
        text: String,
        font: PdfFont,
        fontSize: Float,
        alignment: TextAlignment,
        colspan: Int = 1,
        border: Border? = null
    ): Cell {
        return Cell(1, colspan).apply {
            add(Paragraph(text).setFont(font).setFontSize(fontSize))
            setTextAlignment(alignment)
            setBorder(border ?: Border.NO_BORDER)
            setPadding(1f)
        }
    }

    private fun emptyCell(colspan: Int = 1): Cell {
        return Cell(1, colspan).setBorder(Border.NO_BORDER)
    }

    private fun formatMoney(amount: BigDecimal?): String =
        amount?.let { moneyFormatter.format(it) } ?: ""

    fun getCountryNameFromCode(countryCode: String, displayLocale: Locale = Locale.ENGLISH): String {
        val locale = Locale.Builder().setRegion(countryCode).build()
        return locale.getDisplayCountry(displayLocale)
    }

    fun maxTextWidth(texts: List<String>, font: PdfFont, fontSize: Float): Float {
        return texts.maxOfOrNull { font.getWidth(it, fontSize) } ?: 0f
    }
}