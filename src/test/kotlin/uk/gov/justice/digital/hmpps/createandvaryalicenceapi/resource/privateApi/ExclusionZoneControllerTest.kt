package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import jakarta.validation.ValidationException
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.ExclusionZoneService

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [ExclusionZoneController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [ExclusionZoneController::class])
@WebAppConfiguration
class ExclusionZoneControllerTest {
  @MockitoBean
  private lateinit var exclusionZoneService: ExclusionZoneService

  @Autowired
  private lateinit var mvc: MockMvc

  @BeforeEach
  fun reset() {
    reset(exclusionZoneService)

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
        MockMvcRequestBuilders.multipart("/exclusion-zone/id/4/condition/id/1/file-upload").file(fileToUpload),
      )
      .andExpect(MockMvcResultMatchers.status().isOk)

    verify(exclusionZoneService, times(1)).uploadExclusionZoneFile(4, 1, fileToUpload)
  }

  @Test
  fun `upload an invalid exclusion zone file and check validation error is returned`() {
    val fileResource = ClassPathResource("Test_map_2021-12-06_112550.pdf")
    AssertionsForClassTypes.assertThat(fileResource).isNotNull

    val fileToUpload = MockMultipartFile(
      "file",
      fileResource.filename,
      MediaType.MULTIPART_FORM_DATA_VALUE,
      fileResource.file.inputStream(),
    )
    AssertionsForClassTypes.assertThat(fileToUpload).isNotNull

    whenever(exclusionZoneService.uploadExclusionZoneFile(4L, 1L, fileToUpload))
      .thenThrow(ValidationException("Exclusion zone - failed to extract the expected image map"))

    mvc
      .perform(
        MockMvcRequestBuilders.multipart("/exclusion-zone/id/4/condition/id/1/file-upload").file(fileToUpload),
      )
      .andExpect(MockMvcResultMatchers.status().is4xxClientError)

    verify(exclusionZoneService, times(1)).uploadExclusionZoneFile(4, 1, fileToUpload)
  }

  @Test
  fun `get a full-size image for an exclusion zone`() {
    mvc.perform(
      MockMvcRequestBuilders.get("/exclusion-zone/id/4/condition/id/1/full-size-image"),
    )
      .andExpect(MockMvcResultMatchers.status().isOk)

    verify(exclusionZoneService, times(1)).getExclusionZoneImage(4, 1)
  }
}
