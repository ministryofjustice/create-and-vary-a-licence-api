import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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

}
