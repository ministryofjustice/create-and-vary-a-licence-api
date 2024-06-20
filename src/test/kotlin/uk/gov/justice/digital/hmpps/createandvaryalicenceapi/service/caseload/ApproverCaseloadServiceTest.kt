package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummaryApproverView
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonApproverService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OffenderDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OtherIds
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffHuman
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.User
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

class ApproverCaseloadServiceTest {
  private val prisonApproverService = mock<PrisonApproverService>()
  private val probationSearchApiClient = mock<ProbationSearchApiClient>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val communityApiClient = mock<CommunityApiClient>()

  private val service = ApproverCaseloadService(prisonApproverService, probationSearchApiClient, prisonerSearchApiClient, communityApiClient)

  @BeforeEach
  fun reset() {
    reset(prisonApproverService, probationSearchApiClient, prisonerSearchApiClient, communityApiClient)
  }

  @Test
  fun `CADM prison caseload is filtered out`() {
    val aListOfPrisonCodes = listOf("ABC", "DEF", "CADM")
    service.getApprovalNeeded(aListOfPrisonCodes)
    verify(prisonApproverService, times(1)).getLicencesForApproval(listOf("ABC", "DEF"))
  }

  @Test
  fun `It builds the approval needed caseload`() {
    val nomisId = "A1234AA"
    val comUsernames = listOf("smills")

    whenever(prisonApproverService.getLicencesForApproval(aListOfPrisonCodes)).thenReturn(listOf(aLicenceSummaryApproverView))
    whenever(probationSearchApiClient.searchForPeopleByNomsNumber(listOf(nomisId))).thenReturn(
      listOf(anOffenderDetailResult),
    )
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(nomisId))).thenReturn(listOf(aPrisonerSearchResult))
    whenever(communityApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(listOf(aUser))

    val approvalCases = service.getApprovalNeeded(aListOfPrisonCodes)

    assertThat(approvalCases).hasSize(1)

    with(approvalCases.first()) {
      assertThat(licenceId).isEqualTo(1L)
      assertThat(name).isEqualTo("Bob Mortimar")
      assertThat(prisonerNumber).isEqualTo("A1234AA")
      assertThat(submittedByFullName).isEqualTo("X Y")
      assertThat(releaseDate).isEqualTo((LocalDate.of(2021, 10, 22)))
      assertThat(urgentApproval).isFalse()
      assertThat(approvedBy).isEqualTo("jim smith")
      assertThat(approvedOn).isEqualTo((LocalDateTime.of(2023, 9, 19, 16, 38, 42)))
      assertThat(isDueForEarlyRelease).isFalse()
      with(probationPractitioner!!) {
        assertThat(staffCode).isEqualTo("AB012C")
        assertThat(name).isEqualTo("Test Test")
      }
    }

    verify(prisonApproverService, times(1)).getLicencesForApproval(aListOfPrisonCodes)
    verify(probationSearchApiClient, times(1)).searchForPeopleByNomsNumber(listOf(nomisId))
    verify(prisonerSearchApiClient, times(1)).searchPrisonersByNomisIds(listOf(nomisId))
    verify(communityApiClient, times(1)).getStaffDetailsByUsername(comUsernames)
  }

  private companion object {
    val aListOfPrisonCodes = listOf("MDI", "ABC")

    val aLicenceSummaryApproverView = LicenceSummaryApproverView(
      licenceId = 1,
      forename = "Bob",
      surname = "Mortimer",
      dateOfBirth = LocalDate.of(1985, 12, 28),
      licenceStatus = LicenceStatus.SUBMITTED,
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
      comUsername = "smills",
      conditionalReleaseDate = LocalDate.of(2021, 10, 22),
      actualReleaseDate = LocalDate.of(2021, 10, 22),
      sentenceStartDate = LocalDate.of(2018, 10, 22),
      sentenceEndDate = LocalDate.of(2021, 10, 22),
      licenceStartDate = LocalDate.of(2021, 10, 22),
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
      topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
      dateCreated = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
      submittedDate = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
      approvedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
      approvedByName = "jim smith",
      licenceVersion = "1.0",
      versionOf = null,
      isReviewNeeded = false,
      updatedByFullName = "X Y",
      submittedByFullName = "X Y",
    )

    val anOffenderDetailResult = OffenderDetail(
      offenderId = 1L,
      otherIds = OtherIds(
        crn = "X12345",
        croNumber = "AB01/234567C",
        pncNumber = null,
        nomsNumber = "A1234AA",
      ),
      offenderManagers = listOf(
        OffenderManager(
          staffDetail = StaffDetail(
            code = "AB012C",
            forenames = "Test",
            surname = "Test",
            unallocated = false,
          ),
          active = true,
        ),
      ),
    )

    val aPrisonerSearchResult = PrisonerSearchPrisoner(
      prisonerNumber = "A1234AA",
      bookingId = "54321",
      status = "ACTIVE IN",
      mostSeriousOffence = "Robbery",
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
      homeDetentionCurfewEligibilityDate = null,
      releaseDate = LocalDate.of(2021, 10, 22),
      confirmedReleaseDate = LocalDate.of(2021, 10, 22),
      conditionalReleaseDate = LocalDate.of(2021, 10, 22),
      paroleEligibilityDate = null,
      actualParoleDate = null,
      postRecallReleaseDate = null,
      legalStatus = "SENTENCED",
      indeterminateSentence = false,
      recall = false,
      prisonId = "MDI",
      locationDescription = "HMP Moorland",
      bookNumber = "12345A",
      firstName = "Bob",
      middleNames = null,
      lastName = "Mortimar",
      dateOfBirth = LocalDate.of(1985, 12, 28),
      conditionalReleaseDateOverrideDate = null,
      sentenceStartDate = LocalDate.of(2018, 10, 22),
      sentenceExpiryDate = LocalDate.of(2021, 10, 22),
      topupSupervisionStartDate = null,
      croNumber = null,
    )

    val aUser = User(
      staffIdentifier = 2000,
      username = "smills",
      email = "testemail@probation.gov.uk",
      staff = StaffHuman(
        forenames = "Test",
        surname = "Test",
      ),
      teams = emptyList(),
      staffCode = "AB012C",
    )
  }
}
