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
import com.itextpdf.layout.element.*
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
        addInvoiceInfo(document, invoice)
        addInvoiceSummary(document, subtotal, totalDiscount, totalVat, invoice.totalAmount)
        addInvoiceLinesTable(document, invoice)
        addTotalSection(document, invoice)

        document.close()
        return output.toByteArray()
    }

    private fun addHeader(document: Document, invoice: Invoice) {
        document.add(
            Paragraph(invoice.tenant.getCompanyName())
                .setFont(boldFont)
                .setFontSize(24f)
                .setMarginBottom(5f)
        )

        document.add(
            Table(1).setWidth(UnitValue.createPercentValue(100f)).setBorder(Border.NO_BORDER).addCell(
                Cell().apply {
                    setBorder(Border.NO_BORDER)
                    setBorderTop(SolidBorder(ColorConstants.BLACK, 2f))
                    setHeight(2f)
                }
            )
        )
    }

    private fun addPartyInfo(document: Document, invoice: Invoice) {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(Border.NO_BORDER)
            .setMarginTop(30f)

        table.addCell(
            Cell().setBorder(Border.NO_BORDER).apply {
                add(Paragraph("Bill To:").setFont(boldFont).setFontSize(11f))
                add(Paragraph(invoice.getPartyName()).setFont(regularFont).setFontSize(11f))
            }
        )

        table.addCell(
            Cell().setBorder(Border.NO_BORDER).apply {
                add(
                    Paragraph("Currency: ${invoice.currencyCode}")
                        .setFont(regularFont)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setFontSize(11f)
                )
            }
        )

        document.add(table)
    }

    private fun addInvoiceInfo(document: Document, invoice: Invoice) {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(Border.NO_BORDER)
            .setMarginTop(40f)
            .setMarginBottom(5f)

        table.addCell(
            Cell().setBorder(Border.NO_BORDER).apply {
                add(Paragraph("Invoice Number").setFont(regularFont).setFontSize(10f).setFontColor(DeviceGray.GRAY))
                add(Paragraph(invoice.number).setFont(boldFont).setFontSize(16f))
            }
        )

        table.addCell(
            Cell().setBorder(Border.NO_BORDER).apply {
                add(Paragraph("Issue Date").setFont(regularFont).setFontSize(10f).setFontColor(DeviceGray.GRAY))
                add(Paragraph(invoice.issueDate.format(dateFormat)).setFont(boldFont).setFontSize(16f))
                setTextAlignment(TextAlignment.CENTER)
            }
        )

        table.addCell(
            Cell().setBorder(Border.NO_BORDER).apply {
                add(Paragraph("Due Date").setFont(regularFont).setFontSize(10f).setFontColor(DeviceGray.GRAY))
                add(Paragraph(invoice.dueDate?.format(dateFormat) ?: "-").setFont(boldFont).setFontSize(16f))
                setTextAlignment(TextAlignment.RIGHT)
            }
        )

        document.add(table)
    }

    private fun addInvoiceSummary(
        document: Document,
        subtotal: BigDecimal,
        discount: BigDecimal,
        sats: BigDecimal,
        total: BigDecimal
    ) {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(2f, 2f, 2f, 2f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setFontSize(10f)
            .setMarginBottom(20f)

        addTableHeader(table, listOf("Subtotal", "Discount", "Sats", "Total"))
        addTableRow(table, listOf(subtotal, discount, sats, total).map { format(it) })

        document.add(table)
    }

    private fun addInvoiceLinesTable(document: Document, invoice: Invoice) {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(3f, 1f, 1f, 1f, 1f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setFontSize(10f)

        addTableHeader(table, listOf("Item", "Qty", "Unit Price", "Discount %", "VAT %", "Amount"))

        invoice.lines.forEach { line ->
            addTableRow(
                table,
                listOf(
                    line.itemName,
                    line.quantity.toString(),
                    format(line.unitPrice),
                    format(line.discount ?: BigDecimal.ZERO),
                    format(line.vat),
                    format(line.totalAmount)
                )
            )
        }

        table.addCell(
            Cell(1, 6)
                .setBorder(Border.NO_BORDER)
                .setBorderTop(SolidBorder(ColorConstants.BLACK, 1f))
                .setPaddingTop(5f)
                .setPaddingBottom(5f)
        )

        document.add(table)
    }

    private fun addTotalSection(document: Document, invoice: Invoice) {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(3f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setFontSize(10f)

        table.addCell(
            Cell().setBorder(Border.NO_BORDER)
                .add(Paragraph("Total Amount").setFont(boldFont))
        )

        table.addCell(
            Cell().setBorder(Border.NO_BORDER)
                .add(Paragraph(format(invoice.totalAmount)).setFont(boldFont))
                .setTextAlignment(TextAlignment.RIGHT)
        )

        document.add(table)
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