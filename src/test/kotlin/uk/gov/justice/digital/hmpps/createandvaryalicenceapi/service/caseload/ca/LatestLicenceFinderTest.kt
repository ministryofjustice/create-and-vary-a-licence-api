package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison.LatestLicenceFinder.findLatestLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

class LatestLicenceFinderTest {

  @Test
  fun `should return the first element if the licences length is one`() {
    val licences = aLicenceSummary.copy(licenceStatus = LicenceStatus.APPROVED)
    assertThat(findLatestLicenceSummary(listOf(licences))).isEqualTo(licences)
  }

  @Test
  fun `should return the IN_PROGRESS licence if there are IN_PROGRESS and TIMED_OUT licences`() {
    val licences =
      listOf(
        aLicenceSummary.copy(licenceStatus = LicenceStatus.IN_PROGRESS),
        aLicenceSummary.copy(licenceStatus = LicenceStatus.TIMED_OUT),
      )
    assertThat(findLatestLicenceSummary(licences)).isEqualTo(licences.first())
  }

  @Test
  fun `should return the SUBMITTED licence if there are IN_PROGRESS and SUBMITTED licences`() {
    val licences =
      listOf(
        aLicenceSummary.copy(licenceStatus = LicenceStatus.SUBMITTED),
        aLicenceSummary.copy(licenceStatus = LicenceStatus.IN_PROGRESS),
      )
    assertThat(findLatestLicenceSummary(licences)).isEqualTo(licences.first())
  }

  val aLicenceSummary = LicenceSummary(
    kind = LicenceKind.CRD,
    licenceId = 1,
    licenceType = LicenceType.AP,
    licenceStatus = LicenceStatus.IN_PROGRESS,
    nomisId = "A1234AA",
    forename = "Person",
    surname = "One",
    crn = "X12345",
    dateOfBirth = LocalDate.of(1985, 12, 28),
    prisonCode = "BAI",
    prisonDescription = "Moorland (HMP)",
    probationAreaCode = "N01",
    probationAreaDescription = "Wales",
    probationPduCode = "N01A",
    probationPduDescription = "Cardiff",
    probationLauCode = "N01A2",
    probationLauDescription = "Cardiff South",
    probationTeamCode = "NA01A2-A",
    probationTeamDescription = "Cardiff South Team A",
    conditionalReleaseDate = LocalDate.of(2021, 10, 22),
    actualReleaseDate = LocalDate.of(2021, 10, 22),
    sentenceStartDate = LocalDate.of(2018, 10, 22),
    sentenceEndDate = LocalDate.of(2021, 10, 22),
    licenceStartDate = LocalDate.of(2021, 10, 22),
    licenceExpiryDate = LocalDate.of(2021, 10, 22),
    topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
    topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
    comUsername = "com-user",
    bookingId = 54321,
    dateCreated = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
    approvedByName = "Approver Name",
    approvedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
    licenceVersion = "1.0",
    isReviewNeeded = false,

    isInHardStopPeriod = false,
    isDueToBeReleasedInTheNextTwoWorkingDays = false,
    updatedByFullName = "X Y",
  )
}
