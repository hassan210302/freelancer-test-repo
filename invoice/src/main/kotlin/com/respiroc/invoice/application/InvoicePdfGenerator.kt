package com.respiroc.invoice.application

import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceGray
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
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter

class InvoicePdfGenerator {

    private val numberFormat = DecimalFormat("#,##0.00")
    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA)
    private val boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)

    fun generate(invoice: Invoice): ByteArray {
        val output = ByteArrayOutputStream()
        val pdf = PdfDocument(PdfWriter(output))
        val document = Document(pdf, PageSize.A4).apply {
            setMargins(40f, 40f, 40f, 40f)
        }

        val subtotal = invoice.lines.fold(BigDecimal.ZERO) { acc, line -> acc + line.subTotal }
        val totalDiscount = invoice.lines.fold(BigDecimal.ZERO) { acc, line -> acc + line.discountAmount }
        val totalVat = invoice.lines.fold(BigDecimal.ZERO) { acc, line -> acc + line.sats }

        addHeader(document, invoice)
        addPartyInfo(document, invoice)
        addInvoiceDetails(document, invoice)
        addInvoiceLines(document, invoice)
        addSummaryRows(
            document,
            subtotal.subtract(totalDiscount),
            totalVat,
            invoice.totalAmount,
            currencyCode = invoice.currencyCode
        )
        document.close()
        return output.toByteArray()
    }

    private fun addInvoiceLines(
        document: Document,
        invoice: Invoice,
    ) {
        val columnWidths = floatArrayOf(3f, 1f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f)  // narrower columns
        val table = Table(UnitValue.createPercentArray(columnWidths))
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorderBottom(SolidBorder(0.25f))// reduce overall width to 80%
            .setMarginTop(50f)

        fun headerCell(text: String): Cell {
            return Cell()
                .add(Paragraph(text).setFont(boldFont).setFontSize(9f))  // smaller font size
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(3f)  // reduce padding
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(SolidBorder(0.25f))
        }

        fun dataCell(text: String, align: TextAlignment = TextAlignment.LEFT): Cell {
            return Cell()
                .add(Paragraph(text).setFont(regularFont).setFontSize(9f))  // smaller font size
                .setTextAlignment(align)
                .setPadding(1f)  // reduce padding
                .setBorder(Border.NO_BORDER)
        }

        // Add header row
        table.addHeaderCell(headerCell("Description").setTextAlignment(TextAlignment.LEFT))
        table.addHeaderCell(headerCell("Quantity").setTextAlignment(TextAlignment.RIGHT))
        table.addHeaderCell(headerCell("Unit Price\n(excl. VAT)").setTextAlignment(TextAlignment.RIGHT))
        table.addHeaderCell(headerCell("Discount").setTextAlignment(TextAlignment.RIGHT))
        table.addHeaderCell(headerCell("VAT Rate").setTextAlignment(TextAlignment.RIGHT))
        table.addHeaderCell(headerCell("Amount\n(excl. VAT)").setTextAlignment(TextAlignment.RIGHT))
        table.addHeaderCell(headerCell("VAT").setTextAlignment(TextAlignment.RIGHT))
        table.addHeaderCell(headerCell("Amount\n(incl. VAT)").setTextAlignment(TextAlignment.RIGHT))

        invoice.lines.forEachIndexed { index, line ->
            table.addCell(dataCell(line.itemName, TextAlignment.LEFT))
            table.addCell(dataCell(line.quantity.toString(), TextAlignment.RIGHT))
            table.addCell(dataCell(formatMoney(line.unitPrice), TextAlignment.RIGHT))
            table.addCell(dataCell("${line.discount} %", TextAlignment.RIGHT))
            table.addCell(dataCell("${line.vatRate} %", TextAlignment.RIGHT))
            table.addCell(dataCell(formatMoney(line.subTotal.subtract(line.discountAmount)), TextAlignment.RIGHT))
            table.addCell(dataCell(formatMoney(line.sats), TextAlignment.RIGHT))
            table.addCell(dataCell(formatMoney(line.totalAmount), TextAlignment.RIGHT))
        }
        document.add(table)
    }


    fun formatMoney(amount: BigDecimal?): String {
        return amount?.let { "%,.2f".format(it) } ?: ""
    }

    fun addInvoiceDetails(document: Document, invoice: Invoice) {
        val table = Table(UnitValue.createPointArray(floatArrayOf(100f, 120f)))
            .setBorder(Border.NO_BORDER)
            .setWidth(UnitValue.createPercentValue(30f))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setMargin(0f)
            .setPadding(0f)

        fun labelCell(text: String): Cell {
            return Cell().add(Paragraph(text))
                .setFont(regularFont)
                .setFontSize(10f)
                .setTextAlignment(TextAlignment.LEFT)
                .setBorder(Border.NO_BORDER)
                .setPadding(0f)
        }

        fun valueCell(text: String): Cell {
            return Cell().add(Paragraph(text))
                .setFont(regularFont)
                .setFontSize(10f)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(Border.NO_BORDER)
                .setPadding(0f)
        }
        table.addCell(
            Cell(1, 2)
                .add(Paragraph("Invoice").setFont(boldFont).setFontSize(18f))
                .setTextAlignment(TextAlignment.LEFT)
                .setBorder(Border.NO_BORDER)
                .setPaddingBottom(2f)
        )

        table.addCell(labelCell("Invoice No.:"))
        table.addCell(valueCell(invoice.number))

        table.addCell(labelCell("Invoice Date:"))
        table.addCell(valueCell(invoice.issueDate.toString()))

        table.addCell(
            Cell(1, 2)
                .add(Paragraph(" "))
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(4f)
        )

        table.addCell(
            Cell(1, 2)
                .add(Paragraph("Payment Information").setFont(boldFont))
                .setTextAlignment(TextAlignment.LEFT)
                .setBorder(Border.NO_BORDER)
                .setPaddingBottom(2f)
        )

        table.addCell(labelCell("Due Date:"))
        table.addCell(valueCell(invoice.dueDate.toString()))

        document.add(table)
    }

    private fun addSummaryRows(
        document: Document,
        subtotal: BigDecimal,
        totalVat: BigDecimal,
        totalAmount: BigDecimal,
        currencyCode: String
    ) {
        val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(4f, 1f, 2f, 2f, 2f, 2f, 2f, 2f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginTop(5f)

        summaryTable.addCell(
            Cell(1, 5)
                .add(Paragraph("Sum").setFont(boldFont).setFontSize(9f))
                .setTextAlignment(TextAlignment.LEFT)
                .setBorder(Border.NO_BORDER)
        )
        summaryTable.addCell(
            Cell().add(Paragraph(formatMoney(subtotal)).setFont(regularFont).setFontSize(9f))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(Border.NO_BORDER)
        )
        summaryTable.addCell(
            Cell().add(Paragraph(formatMoney(totalVat)).setFont(regularFont).setFontSize(9f))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(Border.NO_BORDER)
        )
        summaryTable.addCell(
            Cell().add(Paragraph(formatMoney(totalAmount)).setFont(regularFont).setFontSize(9f))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(Border.NO_BORDER)
        )
        summaryTable.addCell(
            Cell(1, 8)
                .setBorder(Border.NO_BORDER)
                .setBorderTop(SolidBorder(ColorConstants.BLACK, 0.25f))
                .setHeight(0.25f)
        )

        // Row 2: Payment info
        summaryTable.addCell(
            Cell(1, 6)
                .add(
                    Paragraph("Payable to account xxxx-xxxx-xxxx-xxxx")
                        .setFont(boldFont)
                        .setFontSize(9f)
                )
                .setTextAlignment(TextAlignment.LEFT)
                .setBorder(Border.NO_BORDER)
        )
        summaryTable.addCell(
            Cell(1, 2)
                .add(Paragraph("$currencyCode ${formatMoney(totalAmount)}").setFont(boldFont).setFontSize(9f))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(Border.NO_BORDER)
        )

        summaryTable.addCell(
            Cell(1, 8)
                .setBorder(Border.NO_BORDER)
                .setBorderTop(SolidBorder(ColorConstants.BLACK, 2f))
                .setHeight(1f)
        )
        document.add(summaryTable)
    }


    private fun addHeader(document: Document, invoice: Invoice) {
        val company = invoice.tenant.company
        val address = company.address

        val addressLineParts = listOfNotNull(
            address?.addressPart1,
            address?.addressPart2,
            address?.postalCode
        )

        val addressLine = addressLineParts.joinToString(", ") + ". " + "Org No: ${company.organizationNumber}"

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
                .addCell(
                    Cell().apply {
                        setBorder(Border.NO_BORDER)
                        setBorderTop(SolidBorder(ColorConstants.BLACK, 2f))
                        setHeight(1f)
                    }
                )
        )

        val addressWidth = regularFont.getWidth(addressLine, 10f)
        document.add(
            Paragraph(address?.countryIsoCode?.uppercase())
                .setFont(regularFont)
                .setFontSize(10f)
                .setWidth(addressWidth)
                .setTextAlignment(TextAlignment.LEFT)
                .setHorizontalAlignment(HorizontalAlignment.RIGHT)
                .setMarginBottom(0f)
        )

        document.add(
            Paragraph(addressLine)
                .setFont(regularFont)
                .setFontSize(10f)
                .setWidth(addressWidth)
                .setTextAlignment(TextAlignment.LEFT)
                .setHorizontalAlignment(HorizontalAlignment.RIGHT)
                .setMarginBottom(5f)
        )


    }

    private fun addPartyInfo(document: Document, invoice: Invoice) {
        val address = if (invoice.customer.isPrivateCustomer()) invoice.customer.person!!.address
        else invoice.customer.company!!.address

        val detailLines = mutableListOf("${invoice.customer.getName()}\n")
        if (address?.addressPart1!!.isNotBlank()) detailLines.add("${address.addressPart1}\n")
        if (address.addressPart2 != null) detailLines.add("${address.addressPart2}\n")
        if (address.postalCode != null) detailLines.add("${address.postalCode}\n")
        if (invoice.customer.isPrivateCustomer()
                .not()
        ) detailLines.add("${invoice.customer.company!!.organizationNumber}\n")
        detailLines.add(address.countryIsoCode)

        val lines = Paragraph()
            .setFont(regularFont)
            .setFontSize(10f)
            .setMargin(0f)
            .setPadding(0f)
            .setMultipliedLeading(1f)

        for (detailLine in detailLines) {
            lines.add(detailLine)
        }

        document.add(lines)
    }

    private fun addTableHeader(table: Table, headers: List<String>) {
        headers.forEach { header ->
            table.addHeaderCell(
                Cell().apply {
                    add(Paragraph(header).setFont(boldFont))
                    setBackgroundColor(DeviceGray.makeLighter(DeviceGray.GRAY))
                    setBorder(Border.NO_BORDER)
                    setPadding(5f)
                }
            )
        }
    }

    private fun addTableRow(table: Table, values: List<String>) {
        values.forEach { value ->
            table.addCell(
                Cell().apply {
                    add(Paragraph(value).setFont(regularFont))
                    setBorder(Border.NO_BORDER)
                    setPadding(5f)
                }
            )
        }
    }

    private fun format(amount: BigDecimal): String = numberFormat.format(amount)
}