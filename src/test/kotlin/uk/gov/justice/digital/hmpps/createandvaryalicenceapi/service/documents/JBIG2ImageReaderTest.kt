import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.File

class JBIG2ImageReaderTest {

  @Test
  fun jbig2PluginShouldBeOnClasspath() {
    // Given
    val className = "org.apache.pdfbox.jbig2.JBIG2ImageReader"

    // When
    val clazz = Class.forName(className)

    // Then
    assertThat(clazz).isNotNull
  }

  /**
   * This test is to ensure that the PDFBox JBIG2 plugin is working correctly.
   *
   *  Generated a simple PDF and verifies JBIG2 compression - see ticket CVSL-4022 for full script!
   * Uses ImageMagick (image creation), Ghostscript (PDF generation), and Poppler (JBIG2 verification)
   *
   */
  @Test
  fun shouldRenderPdfWithoutThrowing() {
    // Given
    val url =
      requireNotNull(
        javaClass.getResource("/test_data/jbig2.pdf"),
      ) { "Test PDF not found on classpath" }

    val file = File(url.toURI())

    // When
    Loader.loadPDF(file).use { document ->
      val renderer = PDFRenderer(document)
      val image: BufferedImage = renderer.renderImageWithDPI(0, 150f)

      // Then
      assertThat(image.width).isGreaterThan(0)
      assertThat(image.height).isGreaterThan(0)
    }
  }
}
