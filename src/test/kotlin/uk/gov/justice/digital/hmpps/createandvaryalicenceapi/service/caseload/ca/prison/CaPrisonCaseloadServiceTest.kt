package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecordService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.caCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ReleaseDateLabelFactory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison.CaPrisonCaseloadService.GroupedByCom
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.workingDays.WorkingDaysService
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class CaPrisonCaseloadServiceTest {
  private val caseloadService = mock<CaseloadService>()
  private val licenceService = mock<LicenceService>()
  private val hdcService = mock<HdcService>()
  private val deliusApiClient = mock<DeliusApiClient>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val workingDaysService = mock<WorkingDaysService>()
  private val releaseDateLabelFactory = ReleaseDateLabelFactory(workingDaysService)
  private val cvlRecordService = mock<CvlRecordService>()

  private val service = CaPrisonCaseloadService(
    licenceService = licenceService,
    deliusApiClient = deliusApiClient,
    existingCasesCaseloadService = ExistingCasesCaseloadService(
      caseloadService,
      clock,
      releaseDateService,
      releaseDateLabelFactory,
    ),
    notStartedCaseloadService = NotStartedCaseloadService(
      hdcService,
      clock,
      deliusApiClient,
      prisonerSearchApiClient,
      releaseDateLabelFactory,
      cvlRecordService,
    ),
  )

  @BeforeEach
  fun reset() {
    reset(
      caseloadService,
      licenceService,
      hdcService,
      deliusApiClient,
      prisonerSearchApiClient,
      releaseDateService,
      cvlRecordService,
    )
  }

  @Nested
  inner class `split Cases By Com Details` {
    val caseWithComUsername = caCase().copy(
      probationPractitioner = ProbationPractitioner(
        staffUsername = "ABC123",
      ),
    )
    val caseWithComCode = caCase().copy(probationPractitioner = aProbationPractitioner)
    val caseWithNoComId = caCase().copy(probationPractitioner = ProbationPractitioner())

    @Test
    fun `initialises params to empty arrays if there are no relevant cases`() {
      assertThat(service.splitCasesByComDetails(listOf(caseWithComUsername))).isEqualTo(
        GroupedByCom(
          withStaffCode = emptyList(),
          withStaffUsername = listOf(caseWithComUsername),
          withNoComId = emptyList(),
        ),
      )
      assertThat(service.splitCasesByComDetails(listOf(caseWithComCode))).isEqualTo(
        GroupedByCom(
          withStaffCode = listOf(caseWithComCode),
          withStaffUsername = emptyList(),
          withNoComId = emptyList(),
        ),
      )
      assertThat(service.splitCasesByComDetails(listOf(caseWithNoComId))).isEqualTo(
        GroupedByCom(
          withStaffCode = emptyList(),
          withStaffUsername = emptyList(),
          withNoComId = listOf(caseWithNoComId),
        ),
      )
    }
  }

  private companion object {
    private fun createClock(timestamp: String) = Clock.fixed(Instant.parse(timestamp), ZoneId.systemDefault())

    val dateTime: LocalDateTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 13, 39))
    val instant: Instant = dateTime.atZone(ZoneId.systemDefault()).toInstant()
    val clock: Clock = createClock(instant.toString())

    val aProbationPractitioner = ProbationPractitioner(staffCode = "DEF456")
  }
}
