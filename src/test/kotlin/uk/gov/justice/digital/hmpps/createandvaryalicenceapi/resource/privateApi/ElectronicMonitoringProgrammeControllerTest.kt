package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.NotSecuredWebMvcTest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateElectronicMonitoringProgrammeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.ElectronicMonitoringProgrammeService

@NotSecuredWebMvcTest(controllers = [ElectronicMonitoringProgrammeController::class])
class ElectronicMonitoringProgrammeControllerTest {

  @MockitoBean
  private lateinit var electronicMonitoringProgrammeService: ElectronicMonitoringProgrammeService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(electronicMonitoringProgrammeService)

    mvc = MockMvcBuilders
      .standaloneSetup(ElectronicMonitoringProgrammeController(electronicMonitoringProgrammeService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `Updates the electronic monitoring programme details`() {
    val request = UpdateElectronicMonitoringProgrammeRequest(isToBeTaggedForProgramme = true, programmeName = "Test Programme")
    mvc.perform(
      post("/licence/id/1/electronic-monitoring-programmes")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(request)),
    )
      .andExpect(status().isOk)

    verify(electronicMonitoringProgrammeService, times(1)).updateElectronicMonitoringProgramme(1, request)
  }
}
