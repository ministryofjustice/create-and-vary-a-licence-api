package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceStatistics
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceStatisticsService
import java.time.LocalDate

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@WebAppConfiguration
class LicenceStatisticsControllerTest {
  @MockBean
  private lateinit var licenceStatisticsService: LicenceStatisticsService
  private lateinit var mvc: MockMvc

  @BeforeEach
  fun reset() {
    org.mockito.kotlin.reset(licenceStatisticsService)

    mvc = MockMvcBuilders.standaloneSetup(LicenceStatisticsController(licenceStatisticsService)).build()
  }

  @Test
  fun `get statistics between start and end dates`() {
    val request = MockMvcRequestBuilders.get("/support/licence-statistics?startDate=2022-09-02&endDate=2022-10-23")
      .accept(MediaType.APPLICATION_JSON)

    whenever(
      licenceStatisticsService.getStatistics(
        startDate = LocalDate.of(2022, 9, 2),
        endDate = LocalDate.of(2022, 10, 23)
      )
    ).thenReturn(
      aListOfStatistics
    )

    mvc.perform(request)
      .andExpect(MockMvcResultMatchers.status().isOk)
      .andExpect { aListOfStatistics }

    verify(licenceStatisticsService, times(1))
      .getStatistics(startDate = LocalDate.of(2022, 9, 2), endDate = LocalDate.of(2022, 10, 23))
  }

  companion object {
    val aListOfStatistics = listOf(
      LicenceStatistics(
        prison = "MDI",
        licenceType = "AP",
        inProgress = 10
      )
    )
  }
}
