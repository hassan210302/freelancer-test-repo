package com.respiroc.invoice.application

import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.ColorConstants
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
import java.time.format.DateTimeFormatter

class InvoicePdfGenerator {

    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA)
    private val boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)

    fun generate(invoice: Invoice): ByteArray {
        val output = ByteArrayOutputStream()
        val pdf = PdfDocument(PdfWriter(output))
        val document = Document(pdf, PageSize.A4).apply {
            setMargins(40f, 40f, 40f, 40f)
        }

        val subtotal = invoice.lines.sumOf { it.subTotal }
        val totalDiscount = invoice.lines.sumOf { it.discountAmount }
        val totalVat = invoice.lines.sumOf { it.sats }

        addHeader(document, invoice)
        addPartyInfo(document, invoice)
        addInvoiceDetails(document, invoice)
        addInvoiceLines(document, invoice)
        addSummaryRows(document, subtotal - totalDiscount, totalVat, invoice.totalAmount, invoice.currencyCode)

        document.close()
        return output.toByteArray()
    }

    private fun addInvoiceLines(document: Document, invoice: Invoice) {
        val columnWidths = floatArrayOf(3f, 1f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f)
        val table = Table(UnitValue.createPercentArray(columnWidths))
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorderBottom(SolidBorder(0.25f))
            .setMarginTop(50f)

        val headers = listOf(
            "Description", "Quantity", "Unit Price\n(excl. VAT)", "Discount",
            "VAT Rate", "Amount\n(excl. VAT)", "VAT", "Amount\n(incl. VAT)"
        )

        headers.forEachIndexed { index, text ->
            table.addHeaderCell(
                createCell(text, boldFont, 9f, TextAlignment.CENTER)
                    .setBorderTop(Border.NO_BORDER)
                    .setBorderLeft(Border.NO_BORDER)
                    .setBorderRight(Border.NO_BORDER)
                    .setBorderBottom(SolidBorder(0.25f))
                    .setPadding(3f)
                    .setTextAlignment(if (index == 0) TextAlignment.LEFT else TextAlignment.RIGHT)
            )
        }

        invoice.lines.forEach { line ->
            table.addCell(createCell(line.itemName, regularFont, 9f, TextAlignment.LEFT))
            table.addCell(createCell(line.quantity.toString(), regularFont, 9f, TextAlignment.RIGHT))
            table.addCell(createCell(formatMoney(line.unitPrice), regularFont, 9f, TextAlignment.RIGHT))
            table.addCell(createCell("${line.discount} %", regularFont, 9f, TextAlignment.RIGHT))
            table.addCell(createCell("${line.vatRate} %", regularFont, 9f, TextAlignment.RIGHT))
            table.addCell(
                createCell(
                    formatMoney(line.subTotal - line.discountAmount),
                    regularFont,
                    9f,
                    TextAlignment.RIGHT
                )
            )
            table.addCell(createCell(formatMoney(line.sats), regularFont, 9f, TextAlignment.RIGHT))
            table.addCell(createCell(formatMoney(line.totalAmount), regularFont, 9f, TextAlignment.RIGHT))
        }

        document.add(table)
    }

    private fun addInvoiceDetails(document: Document, invoice: Invoice) {
        val table = Table(UnitValue.createPointArray(floatArrayOf(100f, 120f)))
            .setWidth(UnitValue.createPercentValue(30f))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setBorder(Border.NO_BORDER)

        fun label(text: String) = createCell(text, regularFont, 10f, TextAlignment.LEFT)
        fun value(text: String) = createCell(text, regularFont, 10f, TextAlignment.RIGHT)

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

    private fun addSummaryRows(
        document: Document,
        subtotal: BigDecimal,
        totalVat: BigDecimal,
        totalAmount: BigDecimal,
        currencyCode: String
    ) {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(4f, 1f, 2f, 2f, 2f, 2f, 2f, 2f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginTop(5f)

        table.addCell(createCell("Sum", boldFont, 9f, TextAlignment.LEFT, colspan = 5, border = Border.NO_BORDER))
        table.addCell(createCell(formatMoney(subtotal), regularFont, 9f, TextAlignment.RIGHT))
        table.addCell(createCell(formatMoney(totalVat), regularFont, 9f, TextAlignment.RIGHT))
        table.addCell(createCell(formatMoney(totalAmount), regularFont, 9f, TextAlignment.RIGHT))
        table.addCell(emptyCell(8).setBorderTop(SolidBorder(ColorConstants.BLACK, 0.25f)))

        table.addCell(
            createCell("Payable", boldFont, 9f, TextAlignment.LEFT, colspan = 6)
        )
        table.addCell(
            createCell("$currencyCode ${formatMoney(totalAmount)}", boldFont, 9f, TextAlignment.RIGHT, colspan = 2)
        )
        table.addCell(emptyCell(8).setBorderTop(SolidBorder(ColorConstants.BLACK, 2f)).setHeight(1f))

        document.add(table)
    }

    private fun addHeader(document: Document, invoice: Invoice) {
        val company = invoice.tenant.company
        val address = company.address!!

        val addressLine = listOfNotNull(
            address.addressPart1, address.addressPart2, address.postalCode
        ).joinToString(", ") + ". Org No: ${company.organizationNumber}"

        document.add(
            Paragraph(company.name)
                .setFont(boldFont)
                .setFontSize(14f)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(2f)
        )

        document.add(
            Table(1)
                .setWidth(UnitValue.createPercentValue(100f))
                .setBorder(Border.NO_BORDER)
                .addCell(emptyCell().setBorderTop(SolidBorder(ColorConstants.BLACK, 2f)).setHeight(1f))
        )

        val addressWidth = regularFont.getWidth(addressLine, 10f)

        listOf(
            address.countryIsoCode.uppercase(),
            addressLine
        ).forEach {
            document.add(
                Paragraph(it)
                    .setFont(regularFont)
                    .setFontSize(10f)
                    .setWidth(addressWidth)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setHorizontalAlignment(HorizontalAlignment.RIGHT)
                    .setMarginBottom(0f)
            )
        }
    }

    private fun addPartyInfo(document: Document, invoice: Invoice) {
        val address = invoice.customer.let {
            if (it.isPrivateCustomer()) it.person!!.address else it.company!!.address
        }

        val details = buildList {
            add(invoice.customer.getName())
            address?.addressPart1?.takeIf { it.isNotBlank() }?.let { add(it) }
            address?.addressPart2?.let { add(it) }
            address?.postalCode?.let { add(it) }
            if (!invoice.customer.isPrivateCustomer()) {
                add(invoice.customer.company!!.organizationNumber)
            }
            add(address?.countryIsoCode.orEmpty())
        }

        val paragraph = Paragraph()
            .setFont(regularFont)
            .setFontSize(10f)
            .setMultipliedLeading(1f)

        details.forEach { paragraph.add("$it\n") }

        document.add(paragraph)
    }

    private fun createCell(
        text: String,
        font: com.itextpdf.kernel.font.PdfFont,
        fontSize: Float,
        alignment: TextAlignment,
        colspan: Int = 1,
        border: Border? = null
    ): Cell {
        return Cell(1, colspan).apply {
            add(Paragraph(text).setFont(font).setFontSize(fontSize))
            setTextAlignment(alignment)
            setBorder(border ?: Border.NO_BORDER) // setBorder method doesn't work with default value.
            setPadding(1f)
        }
    }

    private fun emptyCell(colspan: Int = 1): Cell {
        return Cell(1, colspan).setBorder(Border.NO_BORDER)
    }

    private fun formatMoney(amount: BigDecimal?): String = amount?.let { "%,.2f".format(it) } ?: ""
}