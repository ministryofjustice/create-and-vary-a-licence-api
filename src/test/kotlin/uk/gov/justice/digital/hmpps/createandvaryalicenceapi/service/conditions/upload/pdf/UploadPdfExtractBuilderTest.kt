package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.upload.pdf

import org.apache.pdfbox.Loader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.core.io.ClassPathResource

class UploadPdfExtractBuilderTest {

  @ParameterizedTest
  @CsvSource(
    "old/portrait",
    "old/landscape",
    "new/landscape",
    "new/portrait-single-paragraph",
  )
  fun `extracts only the description`(fileName: String) {
    val bytes =
      ClassPathResource("pdfs/$fileName.pdf").inputStream.readAllBytes()
    Loader.loadPDF(bytes).use {
      val extract = UploadPdfExtractBuilder(it).build()
      assertThat(extract.description.normalise()).isEqualTo(EXPECTED_DESCRIPTION)
    }
  }

  // This is an example of a malformed PDF where we can't extract the data from
  @Disabled
  @ParameterizedTest
  @CsvSource(
    "new/portrait-multi-paragraph",
  )
  fun `extracts only the description from multiparagraph`(fileName: String) {
    val bytes =
      ClassPathResource("pdfs/$fileName.pdf").inputStream.readAllBytes()
    Loader.loadPDF(bytes).use {
      val extract = UploadPdfExtractBuilder(it).build()
      assertThat(extract.description.normalise()).isEqualTo(EXPECTED_DESCRIPTION)
    }
  }

  @Test
  fun `extracts description from the original pdf format`() {
    val bytes = ClassPathResource("Test_map_2021-12-06_112550.pdf").inputStream.readAllBytes()
    Loader.loadPDF(bytes).use {
      assertThat(UploadPdfExtractBuilder(it).build().description).contains("Description")
    }
  }

  companion object {
    private val EXPECTED_DESCRIPTION =
      """Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam dapibus, turpis non dignissim ullamcorper, tellus magna mattis 
  massa, id consequat nunc massa eget enim. Vivamus blandit cursus purus et interdum. Fusce sit amet dolor eros. Morbi in leo 
  sed ligula ultrices interdum quis sit amet quam. Vestibulum sapien eros, porttitor nec purus ac, maximus feugiat dolor. Aenean 
  sagittis varius erat at suscipit. Sed non ipsum sed lacus tristique pretium quis a dolor. Morbi non suscipit libero, nec venenatis 
  augue. Aenean vitae dolor imperdiet, pretium diam at, mattis turpis. Integer et molestie quam. Aliquam dui ligula, commodo at 
  tincidunt et, mollis sit amet lacus. Sed maximus, tellus eget aliquet lacinia, sem diam maximus justo, sed mattis est dui ut 
  massa. Donec dictum sit amet libero vitae egestas. Donec sit amet sem non ligula aliquet tempor. Nam accumsan eu mi quis 
  tristique. 
    
  Pellentesque rhoncus tempus velit, a vestibulum turpis efficitur quis. Vestibulum pellentesque magna at orci dapibus, quis 
  consectetur diam aliquet. Proin ac nisi dolor. Aliquam venenatis justo massa, quis posuere lectus maximus sit amet. Vivamus 
  non cursus turpis. Curabitur accumsan sollicitudin neque at tempus. Aenean hendrerit nisi ut est placerat mollis. Pellentesque 
  vel orci vitae quam venenatis rhoncus molestie quis lacus. Ut dictum interdum accumsan. Nulla lobortis vestibulum nunc sed 
  ullamcorper. Vestibulum sollicitudin condimentum enim. 
    
  Quisque dictum lacinia lectus, sed elementum diam commodo et. Suspendisse sodales non velit et congue. Vestibulum elit 
  sapien, malesuada vitae mi vel, convallis faucibus est. Nunc rhoncus leo velit, nec facilisis dolor hendrerit faucibus. Aliquam a 
  condimentum lacus. Vivamus cursus ex ac erat molestie consectetur. Praesent nec tempus elit. Vivamus imperdiet magna nec 
  lacinia tincidunt. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Suspendisse vitae 
  efficitur urna. Aliquam rhoncus cursus dui, ut congue dolor malesuada quis. Ut aliquam dui sed nisl venenatis, in fringilla odio 
  scelerisque.""".normalise()

    fun String.normalise() = // transform multiple whitespace characters and any nbsp into single space.
      this.replace("\u00A0", " ").replace(Regex("\\s+"), " ").trim()
  }
}
