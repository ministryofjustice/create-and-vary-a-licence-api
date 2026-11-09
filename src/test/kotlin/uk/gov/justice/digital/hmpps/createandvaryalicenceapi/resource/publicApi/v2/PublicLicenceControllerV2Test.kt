package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.json.JsonCompareMode.STRICT
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.PublicLicenceController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aModelHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aModelLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi.PublicLicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi.v2.PublicLicenceServiceV2
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToPublicLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.createTestMapper
import java.time.DayOfWeek
import java.time.LocalTime

class PublicLicenceControllerV2Test {
  private val licenceService = mock<LicenceService>()
  private val mvc = MockMvcBuilders.standaloneSetup(
    PublicLicenceController(PublicLicenceService(mock(), licenceService, mock())),
    PublicLicenceControllerV2(PublicLicenceServiceV2(licenceService)),
  ).setControllerAdvice(ControllerAdvice()).build()

  @Test
  fun `both versions return the original non HDC licence response`() {
    // Given
    val licence = aModelLicence().copy(
      version = "2.1",
      additionalLicenceConditions = emptyList(),
      additionalPssConditions = emptyList(),
    )
    whenever(licenceService.getLicenceById(1)).thenReturn(licence)
    val expectedJson = createTestMapper().writeValueAsString(licence.transformToPublicLicence())

    listOf("/public", "/public/v2").forEach { apiPath ->
      // When
      val response = mvc.perform(get("$apiPath/licences/id/1").accept(APPLICATION_JSON))

      // Then
      response
        .andExpect(status().isOk)
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json(expectedJson, STRICT))
    }
  }

  @Test
  fun `HDC curfew fields are only exposed by v2`() {
    // Given
    val licence = aModelHdcLicence().copy(
      version = "2.1",
      firstNightCurfewTimes = CurfewTimes(
        fromTime = LocalTime.of(18, 0),
        untilTime = LocalTime.of(8, 0),
        createdTimestamp = null,
      ),
      weeklyCurfewTimes = listOf(
        CurfewTimes(
          curfewTimesSequence = 1,
          fromDay = DayOfWeek.MONDAY,
          fromTime = LocalTime.of(19, 0),
          untilDay = DayOfWeek.TUESDAY,
          untilTime = LocalTime.of(7, 0),
          createdTimestamp = null,
        ),
      ),
    )
    whenever(licenceService.getLicenceById(1)).thenReturn(licence)

    // When
    val originalResponse = mvc.perform(get("/public/licences/id/1").accept(APPLICATION_JSON))
    val v2Response = mvc.perform(get("/public/v2/licences/id/1").accept(APPLICATION_JSON))

    // Then
    originalResponse
      .andExpect(status().isOk)
      .andExpect(content().json(createTestMapper().writeValueAsString(licence.transformToPublicLicence()), STRICT))
      .andExpect(jsonPath("$.curfewAddress").doesNotExist())
      .andExpect(jsonPath("$.firstNightCurfewTimes").doesNotExist())
      .andExpect(jsonPath("$.curfewTimes").doesNotExist())

    v2Response
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.kind").value("HDC"))
      .andExpect(jsonPath("$.firstNightCurfewTimes.fromTime").value("18:00:00"))
      .andExpect(jsonPath("$.firstNightCurfewTimes.untilTime").value("08:00:00"))
      .andExpect(jsonPath("$.curfewTimes[0].fromDay").value("MONDAY"))
      .andExpect(jsonPath("$.curfewTimes[0].fromTime").value("19:00:00"))
      .andExpect(jsonPath("$.curfewTimes[0].untilDay").value("TUESDAY"))
      .andExpect(jsonPath("$.curfewTimes[0].untilTime").value("07:00:00"))
  }

  @ParameterizedTest
  @ValueSource(strings = ["/public", "/public/v2"])
  fun `both versions return not found for missing licences`(apiPath: String) {
    // Given
    whenever(licenceService.getLicenceById(99)).thenThrow(EntityNotFoundException("Licence not found"))

    // When
    val response = mvc.perform(get("$apiPath/licences/id/99").accept(APPLICATION_JSON))

    // Then
    response
      .andExpect(status().isNotFound)
  }
}
