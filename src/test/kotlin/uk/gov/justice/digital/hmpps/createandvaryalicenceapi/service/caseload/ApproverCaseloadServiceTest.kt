package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummaryApproverView
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonApproverService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

class ApproverCaseloadServiceTest {
  private val prisonApproverService = mock<PrisonApproverService>()

  private val service = ApproverCaseloadService(prisonApproverService)

  @BeforeEach
  fun reset() {
    reset(prisonApproverService)
  }

  @Test
  fun `CADM prison caseload is filtered out`() {
    val aListOfPrisonCodes = listOf("ABC", "DEF", "CADM")
    service.getApprovalNeeded(aListOfPrisonCodes)
    verify(prisonApproverService, times(1)).getLicencesForApproval(listOf("ABC", "DEF"))
  }

  private companion object {
    val aLicenceSummaryApproverView = LicenceSummaryApproverView(
      licenceId = 1,
      forename = "Bob",
      surname = "Mortimer",
      dateOfBirth = LocalDate.of(1985, 12, 28),
      licenceStatus = LicenceStatus.IN_PROGRESS,
      kind = LicenceKind.CRD,
      licenceType = LicenceType.AP,
      nomisId = "A1234AA",
      crn = "X12345",
      bookingId = 54321,
      prisonCode = "MDI",
      prisonDescription = "Moorland (HMP)",
      probationAreaCode = "N01",
      probationAreaDescription = "Wales",
      probationPduCode = "N01A",
      probationPduDescription = "Cardiff",
      probationLauCode = "N01A2",
      probationLauDescription = "Cardiff South",
      probationTeamCode = "NA01A2-A",
      probationTeamDescription = "Cardiff South Team A",
      comUsername = "jsmith",
      conditionalReleaseDate = LocalDate.of(2022, 12, 28),
      actualReleaseDate = LocalDate.of(2022, 12, 30),
      sentenceStartDate = LocalDate.of(2018, 10, 22),
      sentenceEndDate = LocalDate.of(2021, 10, 22),
      licenceStartDate = LocalDate.of(2021, 10, 22),
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
      topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
      postRecallReleaseDate = LocalDate.of(2021, 10, 22),
      dateCreated = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
      submittedDate = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
      approvedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
      approvedByName = "Jim Smith",
      licenceVersion = "1.0",
      versionOf = null,
      isReviewNeeded = false,
      updatedByFullName = "Test Updater",
      submittedByFullName = "Test Submitter",
      hardStopDate = LocalDate.of(2020, 1, 6),
      hardStopWarningDate = LocalDate.of(2020, 1, 4),
      isInHardStopPeriod = true,
      isDueForEarlyRelease = true,
      isDueToBeReleasedInTheNextTwoWorkingDays = true,
    )
  }
}
