package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStreamWriter
import javax.imageio.ImageIO
import javax.persistence.EntityNotFoundException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadDetail as EntityAdditionalConditionUploadDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionUploadSummary as EntityAdditionalConditionUploadSummary

@Service
class ExclusionZoneService(
  private val licenceRepository: LicenceRepository,
  private val additionalConditionRepository: AdditionalConditionRepository,
  private val additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository,
) {

  @Transactional
  fun uploadExclusionZoneFile(licenceId: Long, conditionId: Long, file: MultipartFile) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val additionalCondition = additionalConditionRepository
      .findById(conditionId)
      .orElseThrow { EntityNotFoundException("$conditionId") }

    // Remove any existing upload detail rows manually - intentionally not linked to the additionalCondition entity
    additionalCondition.additionalConditionUploadSummary.map { it.uploadDetailId }.forEach {
      additionalConditionUploadDetailRepository.findById(it).ifPresent { detail ->
        additionalConditionUploadDetailRepository.delete(detail)
      }
    }

    // Process the MapMaker PDF file to get the fullSizeImage, thumbnailImage and descriptive text
    val fullSizeImage = extractFullSizeImageJpeg(file.inputStream)
    val description = extractDescription(file.inputStream)
    val thumbnailImage = createThumbnailImageJpeg(fullSizeImage)

    val uploadDetail = EntityAdditionalConditionUploadDetail(
      licenceId = licenceEntity.id,
      additionalConditionId = additionalCondition.id,
      originalData = file.bytes,
      fullSizeImage = fullSizeImage,
    )

    val savedDetail = additionalConditionUploadDetailRepository.saveAndFlush(uploadDetail)

    val uploadSummary = EntityAdditionalConditionUploadSummary(
      additionalCondition = additionalCondition,
      filename = file.originalFilename,
      fileType = file.contentType,
      fileSize = file.size.toInt(),
      description = description,
      thumbnailImage = thumbnailImage,
      uploadDetailId = savedDetail.id,
    )

    val updatedAdditionalCondition = additionalCondition.copy(additionalConditionUploadSummary = listOf(uploadSummary))

    additionalConditionRepository.saveAndFlush(updatedAdditionalCondition)
  }

  @Transactional
  fun removeExclusionZoneFile(licenceId: Long, conditionId: Long) {
    licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val additionalCondition = additionalConditionRepository
      .findById(conditionId)
      .orElseThrow { EntityNotFoundException("$conditionId") }

    // Remove upload detail rows manually - it is intentionally not linked to the additionalCondition entity
    additionalCondition.additionalConditionUploadSummary.map { it.uploadDetailId }.forEach {
      additionalConditionUploadDetailRepository.findById(it).ifPresent { detail ->
        additionalConditionUploadDetailRepository.delete(detail)
      }
    }

    // Remove the uploadSummary via the additionalCondition uploadSummary list
    val updatedAdditionalCondition = additionalCondition.copy(additionalConditionUploadSummary = emptyList())
    additionalConditionRepository.saveAndFlush(updatedAdditionalCondition)
  }

  fun getExclusionZoneImage(licenceId: Long, conditionId: Long): ByteArray? {
    licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val additionalCondition = additionalConditionRepository
      .findById(conditionId)
      .orElseThrow { EntityNotFoundException("$conditionId") }

    val uploadIds = additionalCondition.additionalConditionUploadSummary.map { it.uploadDetailId }
    if (uploadIds.isNotEmpty()) {
      throw EntityNotFoundException("$conditionId")
    }

    val upload = additionalConditionUploadDetailRepository
      .findById(uploadIds.first())
      .orElseThrow { EntityNotFoundException("$conditionId") }

    return upload.fullSizeImage
  }

  fun extractFullSizeImageJpeg(fileStream: InputStream): ByteArray? {
    var pdfDoc: PDDocument? = null
    try {
      log.info("Reading PDF document from uploaded file stream")
      pdfDoc = PDDocument.load(fileStream)
      val renderer = PDFRenderer(pdfDoc)
      log.info("Rendering page 1 as an image")
      val firstImage = renderer.renderImage(0)
      val baos = ByteArrayOutputStream()
      log.info("Writing the JPG to the output stream")
      ImageIO.write(firstImage, "jpg", baos)
      log.info("Returning a full size image byte array of length ${baos.size()}")
      return baos.toByteArray()
    } catch (e: Exception) {
      log.info("Exception extracting full size image file ${e.message}")
    } finally {
      log.info("Closing PDF document")
      pdfDoc?.close()
      fileStream.close()
    }
    log.info("Returning zero-length ByteArray")
    return ByteArray(0)
  }

  class GetPdfWords : PDFTextStripper() {
    var words: List<String> = ArrayList()
  }

  fun extractDescription(fileStream: InputStream): String {
    var pdfDoc: PDDocument? = null
    try {
      log.info("ExtractDescription: Loading PDF from stream")
      pdfDoc = PDDocument.load(fileStream)
      val stripper = GetPdfWords()
      stripper.sortByPosition = true
      stripper.startPage = 1
      stripper.endPage = 1
      val writer = OutputStreamWriter(ByteArrayOutputStream())
      log.info("ExtractDescription: Writing text to stripper")
      stripper.writeText(pdfDoc, writer)
      log.info("Stripper words from page 2 are ${stripper.words}")
      return stripper.words.joinToString(" ")
    } catch (e: Exception) {
      log.error("Exception processing words from file ${e.message}")
    } finally {
      log.info("Closing PDF document")
      fileStream.close()
      pdfDoc?.close()
    }
    log.error("Return empty description from extractDescription")
    return ""
  }

  fun createThumbnailImageJpeg(fullSizeImage: ByteArray?, width: Int = 150, height: Int = 200): ByteArray? {
    if (fullSizeImage?.isEmpty() == true) {
      log.info("Full size image is empty or undefined")
      return ByteArray(0)
    }
    try {
      val inputStream = fullSizeImage!!.inputStream()
      val original = ImageIO.read(inputStream)
      val scaled = original.getScaledInstance(width, height, Image.SCALE_DEFAULT)
      val outputImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
      val ready = outputImage.graphics.drawImage(scaled, 0, 0, null)
      if (!ready) {
        log.info("Initial image response not ready - waiting 500ms")
        Thread.sleep(500)
      }
      val baos = ByteArrayOutputStream()
      ImageIO.write(outputImage, "jpg", baos)
      return baos.toByteArray()
    } catch (e: Exception) {
      log.error("Exception creating thumbnail image ${e.message}")
    }
    log.info("Returning zero-length ByteArray from createThumbnailImageJpeg")
    return ByteArray(0)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
