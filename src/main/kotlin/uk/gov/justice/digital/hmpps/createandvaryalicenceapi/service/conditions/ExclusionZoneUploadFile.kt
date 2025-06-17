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

class ExclusionZoneUploadFile(private val file: MultipartFile) {

  var thumbnailImage: ByteArray? = null
  var fullSizeImage: ByteArray? = null
  var description: String? = null

  fun status(): String = "(file=${if (file.isEmpty()) "empty" else "missing"}, thumbnail=${thumbnailImage?.size}"

  init {
    try {
      if (!file.isEmpty) {
        // Process the MapMaker PDF file to ...
        // get the fullSizeImage from page 1, descriptive text on page 2 and generate a thumbnail
        Loader.loadPDF(RandomAccessReadBuffer(file.inputStream)).use { pdfDoc ->
          initDescription(pdfDoc)
          initFullSizeImage(pdfDoc)
        }
        initThumbnail()
      }
    } catch (io: IOException) {
      log.error("IO error ${io.message}")
    } catch (ipe: InvalidPasswordException) {
      log.error("Invalid password ${ipe.message}")
    } catch (iae: IllegalArgumentException) {
      log.error("Illegal Argument ${iae.message}")
    }
  }

  private fun initDescription(pdfDoc: PDDocument) {
    val stripper = PDFTextStripper()
    stripper.sortByPosition = true
    stripper.startPage = 2
    description = stripper.getText(pdfDoc)
  }

  private fun initFullSizeImage(pdfDoc: PDDocument) {
    val renderer = PDFRenderer(pdfDoc)
    val firstImage = renderer.renderImage(0)
    val croppedImage = firstImage.getSubimage(
      IMAGE_LOCATION_X,
      IMAGE_LOCATION_Y,
      firstImage.width - IMAGE_PADDING,
      firstImage.height - IMAGE_PADDING,
    )

    with(ByteArrayOutputStream()) {
      ImageIO.write(croppedImage, "png", this)
      fullSizeImage = toByteArray()
    }
  }

  private fun initThumbnail() {
    if (fullSizeImage?.isEmpty() == true) {
      log.error("Creating thumbnail image - full size image was empty.")
      thumbnailImage = ByteArray(0)
      return
    }

    val inputStream = fullSizeImage!!.inputStream()
    val original = ImageIO.read(inputStream)
    val scaled = original.getScaledInstance(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, Image.SCALE_DEFAULT)
    val outputImage = BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB)
    val ready = outputImage.graphics.drawImage(scaled, 0, 0, null)

    if (!ready) {
      // Not seen it get here, but just in case.
      log.info("Initial image response not ready - waiting 500ms")
      Thread.sleep(WAIT_FOR_IMAGE_READY_MS)
    }

    with(ByteArrayOutputStream()) {
      ImageIO.write(outputImage, "jpg", this)
      thumbnailImage = toByteArray()
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
