package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import org.apache.pdfbox.Loader
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import org.springframework.web.multipart.MultipartFile
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO

data class ExclusionZonePdfExtract(
  var thumbnailImage: ByteArray,
  var fullSizeImage: ByteArray,
  var description: String,
) {
  companion object {
    fun fromMultipartFile(file: MultipartFile): ExclusionZonePdfExtract? = Loader.loadPDF(RandomAccessReadBuffer(file.inputStream)).use { pdfDoc ->
      return ExclusionZonePdfExtractBuilder(pdfDoc).build()
    }
  }
}

class ExclusionZonePdfExtractBuilder(private var pdfDoc: PDDocument) {
  fun build(): ExclusionZonePdfExtract? {
    try {
      val description = description()
      val fullSizeImage = fullSizeImage()
      val thumbnailImage = thumbnailImage(fullSizeImage)

      if (!listOf(description, thumbnailImage, fullSizeImage).contains(null)) {
        return ExclusionZonePdfExtract(
          description = description!!,
          fullSizeImage = fullSizeImage,
          thumbnailImage = thumbnailImage,
        )
      }
    } catch (io: IOException) {
      log.error("IO error ${io.message}")
    } catch (ipe: InvalidPasswordException) {
      log.error("Invalid password ${ipe.message}")
    } catch (iae: IllegalArgumentException) {
      log.error("Illegal Argument ${iae.message}")
    }

    return null
  }

  private fun description(): String? = with(PDFTextStripper()) {
    sortByPosition = true
    startPage = 2
    getText(pdfDoc)
  }

  private fun fullSizeImage(): ByteArray = with(PDFRenderer(pdfDoc)) {
    val firstImage = renderImage(0)
    val croppedImage = firstImage
      .getSubimage(
        IMAGE_LOCATION_X,
        IMAGE_LOCATION_Y,
        firstImage.width - IMAGE_PADDING,
        firstImage.height - IMAGE_PADDING,
      )

    with(ByteArrayOutputStream()) {
      ImageIO.write(croppedImage, "png", this)
      return toByteArray()
    }
  }

  private fun thumbnailImage(fullSizeImage: ByteArray?): ByteArray {
    if (fullSizeImage?.isEmpty() == true) {
      log.error("Creating thumbnail image - full size image was empty.")
      return ByteArray(0)
    }

    val inputStream = fullSizeImage!!.inputStream()
    val original = ImageIO.read(inputStream)
    val scaled = original.getScaledInstance(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, Image.SCALE_DEFAULT)
    val outputImage = BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB)
    outputImage.graphics.drawImage(scaled, 0, 0, null)

    with(ByteArrayOutputStream()) {
      ImageIO.write(outputImage, "jpg", this)
      return toByteArray()
    }
  }

  companion object {
    val log = LoggerFactory.getLogger(this::class.java)
    const val IMAGE_LOCATION_X = 50
    const val IMAGE_LOCATION_Y = 50
    const val IMAGE_PADDING = 100
    const val THUMBNAIL_WIDTH = 150
    const val THUMBNAIL_HEIGHT = 200
    const val WAIT_FOR_IMAGE_READY_MS: Long = 500
  }
}
