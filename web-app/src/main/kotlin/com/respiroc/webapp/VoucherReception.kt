package com.respiroc.webapp

import com.respiroc.tenant.application.TenantService
import com.respiroc.tenant.domain.model.Tenant
import com.respiroc.util.repository.CustomJpaRepository
import com.respiroc.webapp.controller.BaseController
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.*

@RestController
class VoucherReceptionController(
    private val voucherReceptionService: VoucherReceptionService,
    private val tenantService: TenantService
) {

    private val logger = LoggerFactory.getLogger(VoucherReceptionController::class.java)

    data class EmailDocumentRequest(
        val filename: String,
        val mimeType: String,
        val fileData: String, // base64 encoded
        val senderEmail: String
    )

    // Cloudflare worker in index.js calls this
    @PostMapping("/api/voucher-reception")
    fun receiveDocumentFromEmail(
        @RequestHeader("X-Tenant-Slug") tenantSlug: String,
        @RequestBody request: EmailDocumentRequest
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Receiving Voucher Receipt for $tenantSlug")
        val tenant = tenantService.findTenantBySlug(tenantSlug)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Company not found"))

        val fileData = try {
            Base64.getDecoder().decode(request.fileData)
        } catch (_: Exception) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Invalid base64 data"))
        }

        val saved = voucherReceptionService.saveDocument(
            fileData = fileData,
            filename = request.filename,
            mimeType = request.mimeType,
            senderEmail = request.senderEmail,
            tenant = tenant
        )

        return ResponseEntity.ok(
            mapOf(
                "id" to (saved.id ?: 0),
                "filename" to saved.attachment.filename,
                "status" to "received"
            )
        )
    }
}

@Service
@Transactional
class VoucherReceptionService(
    private val voucherDocumentRepository: VoucherReceptionDocumentRepository,
    private val attachmentRepository: AttachmentRepository,
    private val attachmentService: AttachmentService
) {

    fun saveDocument(
        fileData: ByteArray,
        filename: String,
        mimeType: String,
        senderEmail: String,
        tenant: Tenant
    ): VoucherReceptionDocument {

        val (pdfBytes, pdfName, pdfMime) =
            attachmentService.convertToPdf(fileData, filename, mimeType)

        val attachment = Attachment().apply {
            this.fileData = pdfBytes
            this.filename = pdfName
            this.mimetype = pdfMime
        }
        val savedAttachment = attachmentRepository.save(attachment)

        val document = VoucherReceptionDocument().apply {
            this.attachment = savedAttachment
            this.senderEmail = senderEmail
            this.tenant = tenant
        }
        return voucherDocumentRepository.save(document)
    }
}

@Repository
interface VoucherReceptionDocumentRepository : CustomJpaRepository<VoucherReceptionDocument, Long>

@Entity
@Table(name = "voucher_reception_documents")
class VoucherReceptionDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id", nullable = false)
    lateinit var attachment: Attachment

    @CreationTimestamp
    @Column(name = "received_at", nullable = false)
    var receivedAt: Instant? = null

    @Column(name = "sender_email")
    var senderEmail: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    lateinit var tenant: Tenant
}

@Controller
@RequestMapping("/voucher-reception")
class VoucherReceptionWebController(
    private val voucherReceptionDocumentRepository: VoucherReceptionDocumentRepository,
    private val voucherReceptionService: VoucherReceptionService,
    private val tenantService: TenantService
) : BaseController() {

    @GetMapping(value = ["", "/"])
    fun overview(model: Model): String {
        val currentUser = springUser()
        val documents = voucherReceptionDocumentRepository.findAll()

        val pdfDataMap = documents.associate { doc ->
            doc.id to Base64.getEncoder().encodeToString(doc.attachment.fileData)
        }

        addCommonAttributesForCurrentTenant(model, "Voucher Reception")
        model.addAttribute("documents", documents)
        model.addAttribute("pdfDataMap", pdfDataMap)
        model.addAttribute("tenantSlug", currentUser.ctx.currentTenant?.tenantSlug)
        return "voucher-reception/overview"
    }

    @GetMapping("/document/{id}/pdf")
    fun getDocumentData(@PathVariable id: Long): ResponseEntity<String> {
        val document = voucherReceptionDocumentRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found") }

        val base64Data = Base64.getEncoder().encodeToString(document.attachment.fileData)
        val dataUrl = "data:application/pdf;base64,$base64Data"
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML)
            .body("""<embed id="pdf-embed" type="application/pdf" src="$dataUrl" style="width: 100%; height: 100%; border: none;"/>""");
    }

    @PostMapping("/upload")
    @HxRequest
    fun uploadFiles(
        @RequestParam("files") files: List<MultipartFile>,
        @RequestParam("tenantSlug") tenantSlug: String,
        model: Model): String {
        val tenant = tenantService.findTenantBySlug(tenantSlug) ?: return "Tenant not found."
        val currentUser = springUser()

        try {
            files.forEach { file ->
                val fileData = file.bytes
                val filename = file.originalFilename ?: "unnamed"
                val mimeType = file.contentType ?: "application/octet-stream"

                voucherReceptionService.saveDocument(
                    fileData = fileData,
                    filename = filename,
                    mimeType = mimeType,
                    senderEmail = currentUser.username,
                    tenant = tenant
                )
            }

            val updatedDocuments = voucherReceptionDocumentRepository.findAll()
            model.addAttribute("documents", updatedDocuments)

            return "voucher-reception/overview :: documentTableBody"
        } catch (e: Exception) {
            return "Error saving files"
        }
    }




}
