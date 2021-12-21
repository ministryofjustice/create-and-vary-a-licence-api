package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource

import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ExclusionZoneService

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [ExclusionZoneController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [ExclusionZoneController::class])
@WebAppConfiguration
class ExclusionZoneControllerTest {
  @MockBean
  private lateinit var exclusionZoneService: ExclusionZoneService

  @Autowired
  private lateinit var mvc: MockMvc

  @BeforeEach
  fun reset() {
    Mockito.reset(exclusionZoneService)

    mvc = MockMvcBuilders
      .standaloneSetup(ExclusionZoneController(exclusionZoneService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `upload an exclusion zone PDF file associated with an additional condition`() {
    val fileResource = ClassPathResource("Test_map_2021-12-06_112550.pdf")
    AssertionsForClassTypes.assertThat(fileResource).isNotNull

    val fileToUpload = MockMultipartFile(
      "file",
      fileResource.filename,
      MediaType.MULTIPART_FORM_DATA_VALUE,
      fileResource.file.inputStream(),
    )
    AssertionsForClassTypes.assertThat(fileToUpload).isNotNull

    mvc
      .perform(
        MockMvcRequestBuilders.multipart("/exclusion-zone/id/4/condition/id/1/file-upload").file(fileToUpload)
      )
      .andExpect(MockMvcResultMatchers.status().isOk)

    verify(exclusionZoneService, times(1)).uploadExclusionZoneFile(4, 1, fileToUpload)
  }

  @Test
  fun `remove an exclusion zone upload associated with an additional condition`() {
    mvc.perform(
      MockMvcRequestBuilders.put("/exclusion-zone/id/4/condition/id/1/remove-upload")
    )
      .andExpect(MockMvcResultMatchers.status().isOk)

    verify(exclusionZoneService, times(1)).removeExclusionZoneFile(4, 1)
  }

  @Test
  fun `get a full-size image for an exclusion zone`() {
    mvc.perform(
      MockMvcRequestBuilders.get("/exclusion-zone/id/4/condition/id/1/full-size-image")
    )
      .andExpect(MockMvcResultMatchers.status().isOk)

    verify(exclusionZoneService, times(1)).getExclusionZoneImage(4, 1)
  }
}
