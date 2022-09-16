package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceStatistics
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.Prison
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonRegisterApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

class LicenceStatisticsServiceTest {
  private val prisonRegisterApiClient = mock<PrisonRegisterApiClient>()
  private val licenceService = mock<LicenceService>()
  private val licenceStatisticsService = LicenceStatisticsService(prisonRegisterApiClient, licenceService)

  @BeforeEach
  fun reset() {
    reset(prisonRegisterApiClient)
    reset(licenceService)
  }

  @Test
  fun `matches and gets licence stats correctly`() {

    whenever(prisonRegisterApiClient.getPrisonIds()).thenReturn(listOfPrisons)
    whenever(licenceService.findLicencesMatchingCriteria(any())).thenReturn(listOfLicenceSummaries)

    val actualResult = licenceStatisticsService.getStatistics(LocalDate.now(), LocalDate.now().plusMonths(1))

    verify(prisonRegisterApiClient, times(1)).getPrisonIds()
    verify(licenceService, times(1)).findLicencesMatchingCriteria(any())
    Assertions.assertThat(actualResult).isEqualTo(expectedResult)
  }

  companion object {
    val listOfPrisons = listOf(Prison(prisonId = "MDI"), Prison(prisonId = "BMI"))
    val listOfLicenceSummaries = listOf(
      LicenceSummary(
        licenceId = 1,
        licenceType = LicenceType.AP,
        licenceStatus = LicenceStatus.SUBMITTED,
        nomisId = "A1234AA",
        surname = "Smith",
        forename = "Brian",
        prisonCode = "MDI",
        prisonDescription = "Moorland (HMP)",
        probationAreaCode = "N01",
        probationAreaDescription = "Wales",
        probationPduCode = "N01CA",
        probationPduDescription = "North Wales",
        probationLauCode = "NA01CA-02",
        probationLauDescription = "North Wales",
        probationTeamCode = "NA01CA-02-A",
        probationTeamDescription = "Cardiff South",
        conditionalReleaseDate = LocalDate.now().plusDays(8),
        actualReleaseDate = null,
        crn = "X12344",
        dateOfBirth = null,
        comUsername = "jsmith",
        bookingId = 773722,
        dateCreated = null
      )
    )
    val expectedResult = listOf(
      LicenceStatistics(
        prison = "MDI",
        licenceType = "AP",
        eligibleForCvl = null,
        inProgress = 0,
        submitted = 1,
        approved = 0,
        active = 0,
        inactiveTotal = null,
        inactiveNotApproved = null,
        inactiveApproved = null,
        inactiveHdcApproved = null,
        approvedNotPrinted = null
      ),
      LicenceStatistics(
        prison = "MDI",
        licenceType = "PSS",
        eligibleForCvl = null,
        inProgress = 0,
        submitted = 0,
        approved = 0,
        active = 0,
        inactiveTotal = null,
        inactiveNotApproved = null,
        inactiveApproved = null,
        inactiveHdcApproved = null,
        approvedNotPrinted = null
      ),
      LicenceStatistics(
        prison = "MDI",
        licenceType = "AP_PSS",
        eligibleForCvl = null,
        inProgress = 0,
        submitted = 0,
        approved = 0,
        active = 0,
        inactiveTotal = null,
        inactiveNotApproved = null,
        inactiveApproved = null,
        inactiveHdcApproved = null,
        approvedNotPrinted = null
      )
    )
  }
}
