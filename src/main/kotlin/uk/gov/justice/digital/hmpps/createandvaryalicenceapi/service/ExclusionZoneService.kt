package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
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
import java.io.IOException
import java.io.InputStream
import java.lang.IllegalArgumentException
import javax.imageio.ImageIO
import javax.persistence.EntityNotFoundException
import javax.validation.ValidationException
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
    // There can only be one uploaded exclusion map on this licence/condition
    additionalCondition.additionalConditionUploadSummary.map { it.uploadDetailId }.forEach {
      additionalConditionUploadDetailRepository.findById(it).ifPresent { detail ->
        additionalConditionUploadDetailRepository.delete(detail)
      }
    }

    log.info("uploadExclusionZoneFile:  Name ${file.name} Type ${file.contentType} Original ${file.originalFilename}, Size ${file.size}")

    if (file.isEmpty) {
      log.error("uploadExclusion:  Empty file uploaded, Name ${file.name} Type ${file.contentType} Orig. Name ${file.originalFilename}, Size ${file.size}")
      throw ValidationException("Exclusion zone - file was empty.")
    }

    // Process the MapMaker PDF file to get the fullSizeImage from page 1, descriptive text on page 2 and generate a thumbnail
    val fullSizeImage = extractFullSizeImageJpeg(file.inputStream)
    val description = extractDescription(file.inputStream)
    val thumbnailImage = createThumbnailImageJpeg(fullSizeImage)

    // Validate that we were able to extract meaningful data from the uploaded file
    if (fullSizeImage == null || thumbnailImage == null) {
      log.error("uploadExclusion:  Could not extract images from file, Name ${file.name} Type ${file.contentType} Orig. Name ${file.originalFilename}, Size ${file.size}")
      throw ValidationException("Exclusion zone - failed to extract the expected image map")
    }

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

    // Remove uploadDetail rows manually - intentionally not linked to the additionalCondition entity
    additionalCondition.additionalConditionUploadSummary.map { it.uploadDetailId }.forEach {
      additionalConditionUploadDetailRepository.findById(it).ifPresent { detail ->
        additionalConditionUploadDetailRepository.delete(detail)
      }
    }

    // Remove the additionalConditionData item for 'outOfBoundFilename'
    val updatedAdditionalConditionData = additionalCondition
      .additionalConditionData
      .filter { !it.dataField.equals("outOfBoundFilename") }

    // Update summary and data via the additionalCondition lists
    val updatedAdditionalCondition = additionalCondition.copy(
      additionalConditionData = updatedAdditionalConditionData,
      additionalConditionUploadSummary = emptyList(),
    )

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
    if (uploadIds.isEmpty()) {
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
      pdfDoc = PDDocument.load(fileStream)
      val renderer = PDFRenderer(pdfDoc)
      val firstImage = renderer.renderImage(0)
      val croppedImage = firstImage.getSubimage(50, 50, firstImage.width - 100, firstImage.height - 100)
      val baos = ByteArrayOutputStream()
      ImageIO.write(croppedImage, "jpg", baos)

      // Scale it back up to original size without the borders ?

      return baos.toByteArray()
    } catch (e: IOException) {
      log.error("Extracting full size image - IO error ${e.message}")
    } catch (ipe: InvalidPasswordException) {
      log.error("Extracting full size image - encrypted error ${ipe.message}")
    } finally {
      pdfDoc?.close()
      fileStream.close()
    }
    return null
  }

  fun extractDescription(fileStream: InputStream): String? {
    var pdfDoc: PDDocument? = null
    try {
      pdfDoc = PDDocument.load(fileStream)
      val stripper = PDFTextStripper()
      stripper.sortByPosition = true
      stripper.startPage = 2
      return stripper.getText(pdfDoc)
    } catch (e: IOException) {
      log.error("Extracting exclusion zone description - IO error ${e.message}")
    } catch (ipe: InvalidPasswordException) {
      log.error("Extracting exclusion zone description - encrypted error ${ipe.message}")
    } finally {
      pdfDoc?.close()
      fileStream.close()
    }
    return null
  }

  fun createThumbnailImageJpeg(fullSizeImage: ByteArray?, width: Int = 150, height: Int = 200): ByteArray? {
    if (fullSizeImage?.isEmpty() == true) {
      log.error("Creating thumbnail image - full size image was empty.")
      return ByteArray(0)
    }
    try {
      val inputStream = fullSizeImage!!.inputStream()
      val original = ImageIO.read(inputStream)
      val scaled = original.getScaledInstance(width, height, Image.SCALE_DEFAULT)
      val outputImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
      val ready = outputImage.graphics.drawImage(scaled, 0, 0, null)
      if (!ready) {
        // Not seen it get here, but just in case.
        log.info("Initial image response not ready - waiting 500ms")
        Thread.sleep(500)
      }
      val baos = ByteArrayOutputStream()
      ImageIO.write(outputImage, "jpg", baos)
      return baos.toByteArray()
    } catch (io: IOException) {
      log.error("Creating thumbnail image - IO error ${io.message}")
    } catch (iae: IllegalArgumentException) {
      log.error("Creating thumbnail image (null image) - error ${iae.message}")
    }
    return null
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
