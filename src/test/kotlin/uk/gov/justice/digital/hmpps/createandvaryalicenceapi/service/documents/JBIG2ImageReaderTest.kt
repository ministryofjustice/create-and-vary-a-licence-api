package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

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
