package com.respiroc.util.attachment

import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.WriterProperties
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.respiroc.util.repository.CustomJpaRepository
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam


@Entity
@Table(name = "attachments")
class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "file_data", nullable = false, columnDefinition = "BYTEA")
    lateinit var fileData: ByteArray

    @Column(name = "filename", nullable = false)
    lateinit var filename: String

    @Column(name = "mimetype", nullable = false)
    lateinit var mimetype: String

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant? = null
}

@Repository
interface AttachmentRepository : CustomJpaRepository<Attachment, Long>


@Service
class AttachmentService {
    private val imageMimeTypes = setOf(
        "image/png", "image/jpeg", "image/jpg",
        "image/bmp", "image/gif"
    )

    fun convertToPdf(
        fileData: ByteArray,
        filename: String,
        mimeType: String
    ): Triple<ByteArray, String, String> {

        /* 1. short‑circuit if it is already a PDF ----------------- */
        if (mimeType.equals("application/pdf", true)) {
            return Triple(fileData, ensurePdfExt(filename), "application/pdf")
        }

        /* 2. only PNG/JPEG/BMP/GIF accepted ----------------------- */
        val lowerMime = mimeType.lowercase()
        require(lowerMime in imageMimeTypes) {
            "Unsupported MIME type “$mimeType”. Allowed: PNG, JPEG, BMP, GIF, or PDF."
        }

        /* 3. decode to BufferedImage ------------------------------ */
        val buffered = ImageIO.read(ByteArrayInputStream(fileData))
            ?: throw IllegalArgumentException("Unable to decode image data for $mimeType")

        /* 4. convert → JPEG bytes in standard sRGB ---------------- */
        val jpegBytes = bufferedToJpegBytes(buffered)

        /* 5. wrap JPEG in a single‑page PDF ----------------------- */
        val pdfBaos = ByteArrayOutputStream()
        val writer = PdfWriter(pdfBaos, WriterProperties().setCompressionLevel(9))
        val pdfDoc = PdfDocument(writer)

        val pageSize = PageSize.LETTER
        val document = Document(pdfDoc, pageSize)
            .apply { setMargins(0f, 0f, 0f, 0f) }   // ← **no margins**

        val imgData = ImageDataFactory.create(jpegBytes)
        val image = Image(imgData).apply {
            scaleToFit(pageSize.width, pageSize.height)
            /* centre on the page (optional but nice) */
            val x = (pageSize.width - imageScaledWidth) / 2
            val y = (pageSize.height - imageScaledHeight) / 2
            setFixedPosition(x, y)
        }

        document.add(image)
        document.close()                         // flush & close everything

        return Triple(pdfBaos.toByteArray(), ensurePdfExt(filename), "application/pdf")
    }

    private fun bufferedToJpegBytes(src: BufferedImage): ByteArray {
        // --- force sRGB without alpha ------------------------------------------
        val rgbImg = if (src.type == BufferedImage.TYPE_3BYTE_BGR ||
            src.type == BufferedImage.TYPE_INT_RGB
        ) {
            src
        } else {
            val converted = BufferedImage(
                src.width, src.height,
                BufferedImage.TYPE_3BYTE_BGR
            )
            val g = converted.createGraphics()
            g.drawImage(src, 0, 0, java.awt.Color.WHITE, null) // fills alpha w/ white
            g.dispose()
            converted
        }

        val quality = 0.7f
        ByteArrayOutputStream().use { baos ->
            val writer = ImageIO.getImageWritersByFormatName("jpg").next()
            ImageIO.createImageOutputStream(baos).use { ios ->
                writer.output = ios
                writer.defaultWriteParam.apply {
                    if (canWriteCompressed()) {
                        compressionMode = ImageWriteParam.MODE_EXPLICIT
                        compressionQuality = quality
                    }
                }.also { param ->
                    writer.write(null, IIOImage(rgbImg, null, null), param)
                }
                writer.dispose()
            }
            return baos.toByteArray()
        }
    }


    fun combineIntoPdf(attachments: List<Attachment>): ByteArray {
        val pdfBaos = ByteArrayOutputStream()
        val writer = PdfWriter(pdfBaos, WriterProperties().setCompressionLevel(9))
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc, PageSize.LETTER).apply { setMargins(0f, 0f, 0f, 0f) }

        attachments.forEach { attachment ->
            if (attachment.mimetype.equals("application/pdf", true)) {
                // Merge existing PDF
                val existingPdf = PdfDocument(com.itextpdf.kernel.pdf.PdfReader(ByteArrayInputStream(attachment.fileData)))
                existingPdf.copyPagesTo(1, existingPdf.numberOfPages, pdfDoc)
                existingPdf.close()
            } else if (attachment.mimetype.lowercase() in imageMimeTypes) {
                // Convert image to PDF page
                val buffered = ImageIO.read(ByteArrayInputStream(attachment.fileData))
                    ?: return@forEach
                
                val jpegBytes = bufferedToJpegBytes(buffered)
                val imgData = ImageDataFactory.create(jpegBytes)
                val image = Image(imgData).apply {
                    scaleToFit(PageSize.LETTER.width, PageSize.LETTER.height)
                    val x = (PageSize.LETTER.width - imageScaledWidth) / 2
                    val y = (PageSize.LETTER.height - imageScaledHeight) / 2
                    setFixedPosition(x, y)
                }
                
                document.add(image)
                if (attachment != attachments.last()) {
                    document.add(com.itextpdf.layout.element.AreaBreak())
                }
            }
        }

        document.close()
        return pdfBaos.toByteArray()
    }

    private fun ensurePdfExt(name: String): String =
        if (name.lowercase().endsWith(".pdf")) name else "${name.substringBeforeLast('.', name)}.pdf"
}