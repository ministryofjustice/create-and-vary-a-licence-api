package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.upload.pdf

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
class UploadPdfExtractBuilder(private var pdfDoc: PDDocument) {

  fun build(): UploadPdfExtract {
    val fullSizeImage = fullSizeImage()
    val thumbnailImage = thumbnailImage(fullSizeImage)

    return UploadPdfExtract(description(), fullSizeImage, thumbnailImage)
  }

  private fun description(): String = runCatching {
    with(PDFTextStripper()) {
      sortByPosition = true
      startPage = 2
      getText(pdfDoc)
    }
  }.getOrElse { throw UploadPdfExtractionException("Unable to extract description", it) }

  private fun fullSizeImage(): ByteArray = runCatching {
    with(PDFRenderer(pdfDoc)) {
      val firstPageAsImage = renderImage(0)
      val mapImage = firstPageAsImage.getSubimage(
        IMAGE_LOCATION_X,
        IMAGE_LOCATION_Y,
        firstPageAsImage.width - IMAGE_PADDING,
        firstPageAsImage.height - IMAGE_PADDING,
      )

      with(ByteArrayOutputStream()) {
        ImageIO.write(mapImage, "png", this)
        val bytes = toByteArray()
        if (bytes.isEmpty()) error("Full size image was empty")
        bytes
      }
    }
  }.getOrElse { throw UploadPdfExtractionException("Unable to extract full size image details", it) }

  private fun thumbnailImage(fullSizeImage: ByteArray): ByteArray = runCatching {
    val inputStream = fullSizeImage.inputStream()
    val original = ImageIO.read(inputStream)
    val scaled = original.getScaledInstance(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, Image.SCALE_DEFAULT)
    val outputImage = BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB)
    outputImage.graphics.drawImage(scaled, 0, 0, null)

    with(ByteArrayOutputStream()) {
      ImageIO.write(outputImage, "jpg", this)
      toByteArray()
    }
  }.getOrElse { throw UploadPdfExtractionException("Unable to extract thumbnail image details", it) }

  companion object {
    val log = LoggerFactory.getLogger(this::class.java)
    const val IMAGE_LOCATION_X = 50
    const val IMAGE_LOCATION_Y = 50
    const val IMAGE_PADDING = 100
    const val THUMBNAIL_WIDTH = 150
    const val THUMBNAIL_HEIGHT = 200
  }
}
