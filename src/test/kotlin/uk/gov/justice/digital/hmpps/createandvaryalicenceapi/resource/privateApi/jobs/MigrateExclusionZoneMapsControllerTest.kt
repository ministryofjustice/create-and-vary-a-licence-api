package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi.jobs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.ExclusionZoneUploadsMigration

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [MigrateExclusionZoneMapsController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [MigrateExclusionZoneMapsController::class])
@WebAppConfiguration
class MigrateExclusionZoneMapsControllerTest {
  @MockitoBean
  private lateinit var exclusionZoneUploadsMigration: ExclusionZoneUploadsMigration

  @Autowired
  private lateinit var mvc: MockMvc

  @BeforeEach
  fun reset() {
    org.mockito.kotlin.reset(exclusionZoneUploadsMigration)

    mvc = MockMvcBuilders
      .standaloneSetup(MigrateExclusionZoneMapsController(exclusionZoneUploadsMigration))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @ParameterizedTest
  @ValueSource(ints = [1, 2, 3, 99])
  fun `performs migration of exclusion zone maps with the given batch size`(batchSize: Int) {
    triggerRequest(withBatchSizeOf = batchSize).andExpect(MockMvcResultMatchers.status().isOk)
    verify(exclusionZoneUploadsMigration, times(1)).perform(batchSize)
  }

  @ParameterizedTest
  @ValueSource(ints = [-1, 0])
  fun `will not perform migration with a non positive batch size`(batchSize: Int) {
    triggerRequest(withBatchSizeOf = batchSize).andExpect(MockMvcResultMatchers.status().is4xxClientError)
    verify(exclusionZoneUploadsMigration, never()).perform(anyOrNull())
  }

  @Test
  fun `defaults to batch size of 1 when parameter omitted`() {
    triggerRequest(withBatchSizeOf = null).andExpect(MockMvcResultMatchers.status().isOk)
    verify(exclusionZoneUploadsMigration, times(1)).perform(1)
  }

  private fun triggerRequest(withBatchSizeOf: Int?): ResultActions = mvc.perform(
    MockMvcRequestBuilders.post("/jobs/migrate-exclusion-zone-maps".plus(withBatchSizeOf?.let { "?batchSize=$it" } ?: ""))
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON),
  )
}
