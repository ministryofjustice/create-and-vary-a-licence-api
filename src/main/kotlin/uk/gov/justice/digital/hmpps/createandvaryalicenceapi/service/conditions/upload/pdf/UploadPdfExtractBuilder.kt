package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.upload.pdf

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import org.slf4j.LoggerFactory
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class UploadPdfExtractBuilder(private var pdfDoc: PDDocument, private val v2ImageCroppingEnabled: Boolean) {

  fun build(): UploadPdfExtract {
    val fullSizeImage = fullSizeImage()
    val thumbnailImage = thumbnailImage(fullSizeImage)

    return UploadPdfExtract(description(), fullSizeImage, thumbnailImage)
  }

  private fun description(): String = runCatching {
    val info = pdfDoc.documentInformation

    if (info.metadataKeys.contains("Description")) {
      log.info("Extracting description from PDF metadata")
      return info.getCustomMetadataValue("Description")
    }

    with(BodyTextStripper()) {
      sortByPosition = true
      startPage = 2
      getText(pdfDoc)
    }
  }.getOrElse { throw UploadPdfExtractionException("Unable to extract description", it) }

  // Ignores the page header, "Notes" heading and copyright footer, keeping only the description body text
  private class BodyTextStripper : PDFTextStripper() {
    override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
      val first = textPositions.firstOrNull() ?: return
      val isHeaderOrFooter = first.yDirAdj < HEADER_BOTTOM_Y || first.yDirAdj > first.pageHeight - FOOTER_HEIGHT
      if (isHeaderOrFooter || text.trim() == NOTES_HEADING) return
      super.writeString(text, textPositions)
    }
  }

  private fun fullSizeImage(): ByteArray = runCatching {
    with(PDFRenderer(pdfDoc)) {
      val firstPageAsImage = renderImage(0)
      val mapImage = firstPageAsImage.getSubimage(
        IMAGE_LOCATION_X,
        IMAGE_LOCATION_Y,
        firstPageAsImage.width - IMAGE_PADDING,
        firstPageAsImage.height - IMAGE_PADDING,
      )
      val finalMapImage = if (v2ImageCroppingEnabled) trimWhiteBorder(mapImage) else mapImage

      with(ByteArrayOutputStream()) {
        ImageIO.write(finalMapImage, "png", this)
        val bytes = toByteArray()
        if (bytes.isEmpty()) error("Full size image was empty")
        bytes
      }
    }
  }.getOrElse { throw UploadPdfExtractionException("Unable to extract full size image details", it) }

  private fun trimWhiteBorder(image: BufferedImage): BufferedImage {
    var left = image.width
    var top = image.height
    var right = -1
    var bottom = -1

    for (y in 0 until image.height) {
      for (x in 0 until image.width) {
        val colour = image.getRGB(x, y)
        // Shift the colour value to get the 0-255 value of each the red, green and blue components
        val red = colour shr RED_SHIFT and EIGHT_BIT_MASK
        val green = colour shr GREEN_SHIFT and EIGHT_BIT_MASK
        val blue = colour shr BLUE_SHIFT and EIGHT_BIT_MASK
        if (red < WHITE_THRESHOLD || green < WHITE_THRESHOLD || blue < WHITE_THRESHOLD) {
          left = minOf(left, x)
          top = minOf(top, y)
          right = maxOf(right, x)
          bottom = maxOf(bottom, y)
        }
      }
    }

    val cropLeft = (left - IMAGE_TRIM_PADDING).coerceAtLeast(0)
    val cropTop = (top - IMAGE_TRIM_PADDING).coerceAtLeast(0)
    val cropRight = (right + IMAGE_TRIM_PADDING).coerceAtMost(image.width - 1)
    val cropBottom = (bottom + IMAGE_TRIM_PADDING).coerceAtMost(image.height - 1)

    return image.getSubimage(cropLeft, cropTop, cropRight - cropLeft + 1, cropBottom - cropTop + 1)
  }

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
    const val IMAGE_LOCATION_Y = 55
    const val IMAGE_PADDING = 100
    const val IMAGE_TRIM_PADDING = 2
    const val WHITE_THRESHOLD = 245
    const val HEADER_BOTTOM_Y = 60f
    const val FOOTER_HEIGHT = 50f
    const val NOTES_HEADING = "Notes"
    const val THUMBNAIL_WIDTH = 150
    const val THUMBNAIL_HEIGHT = 200
    const val RED_SHIFT = 16
    const val GREEN_SHIFT = 8
    const val BLUE_SHIFT = 0
    const val EIGHT_BIT_MASK = 0xFF
  }
}
