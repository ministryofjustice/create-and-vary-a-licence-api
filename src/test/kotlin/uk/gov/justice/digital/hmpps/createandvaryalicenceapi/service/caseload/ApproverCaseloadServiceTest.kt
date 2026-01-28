package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceApproverCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonApproverService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManagerWithoutUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Detail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.TeamDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate
import java.time.LocalDateTime

class ApproverCaseloadServiceTest {
  private val prisonApproverService = mock<PrisonApproverService>()
  private val deliusApiClient = mock<DeliusApiClient>()
  private val releaseDateService = mock<ReleaseDateService>()

  private val service = ApproverCaseloadService(prisonApproverService, deliusApiClient, releaseDateService)

  @BeforeEach
  fun reset() {
    reset(prisonApproverService, deliusApiClient, releaseDateService)
  }

  @Nested
  inner class `Build approval needed caseload` {
    @Test
    fun `It builds the approval needed caseload successfully with multiple cases`() {
      val nomisIds = listOf("A1234AA", "B1234BB", "C1234CC")

      whenever(prisonApproverService.getLicenceCasesReadyForApproval(aListOfPrisonCodes)).thenReturn(
        listOf(
          aLicenceApproverCase(),
          aLicenceApproverCase(
            licenceId = 2L,
            prisonNumber = "B1234BB",
            forename = "John",
            surname = "Doe",
            prisonCode = "ABC",
            prisonDescription = "ABC (HMP)",
            comUsername = "jdoe",
          ),
          aLicenceApproverCase(
            licenceId = 3L,
            prisonNumber = "C1234CC",
            forename = "Jane",
            surname = "Doe",
            prisonCode = "MDI",
            comUsername = "jdoe",
          ),
        ),
      )
      whenever(deliusApiClient.getOffenderManagersWithoutUser(nomisIds)).thenReturn(
        listOf(
          aCommunityManager(),
          aCommunityManager(
            case = aProbationCaseResult(
              crn = "Y12345",
              croNumber = "DE01/234567F",
              pncNumber = null,
              nomisId = "B1234BB",
            ),
          ),
        ),
      )

      val approvalCases = service.getApprovalNeeded(aListOfPrisonCodes)

      assertThat(approvalCases).hasSize(2)

      with(approvalCases[0]) {
        assertThat(licenceId).isEqualTo(2L)
        assertThat(name).isEqualTo("John Doe")
        assertThat(prisonerNumber).isEqualTo("B1234BB")
        assertThat(submittedByFullName).isEqualTo("X Y")
        assertThat(releaseDate).isEqualTo((LocalDate.of(2021, 10, 22)))
        assertThat(urgentApproval).isFalse()
        assertThat(approvedBy).isEqualTo("jim smith")
        assertThat(approvedOn).isEqualTo((LocalDateTime.of(2023, 9, 19, 16, 38, 42)))
        with(probationPractitioner) {
          assertThat(staffCode).isEqualTo("AB012C")
          assertThat(name).isEqualTo("Test Test")
          assertThat(allocated).isEqualTo(true)
        }
        assertThat(kind).isEqualTo(LicenceKind.CRD)
        assertThat(prisonCode).isEqualTo("ABC")
        assertThat(prisonDescription).isEqualTo("ABC (HMP)")
      }

      with(approvalCases[1]) {
        assertThat(licenceId).isEqualTo(1L)
        assertThat(name).isEqualTo("Person One")
        assertThat(prisonerNumber).isEqualTo("A1234AA")
        assertThat(submittedByFullName).isEqualTo("X Y")
        assertThat(releaseDate).isEqualTo((LocalDate.of(2021, 10, 22)))
        assertThat(urgentApproval).isFalse()
        assertThat(approvedBy).isEqualTo("jim smith")
        assertThat(approvedOn).isEqualTo((LocalDateTime.of(2023, 9, 19, 16, 38, 42)))
        with(probationPractitioner) {
          assertThat(staffCode).isEqualTo("AB012C")
          assertThat(name).isEqualTo("Test Test")
          assertThat(allocated).isEqualTo(true)
        }
        assertThat(kind).isEqualTo(LicenceKind.CRD)
        assertThat(prisonCode).isEqualTo("MDI")
        assertThat(prisonDescription).isEqualTo("Moorland (HMP)")
      }

      verify(prisonApproverService, times(1)).getLicenceCasesReadyForApproval(aListOfPrisonCodes)
      verify(deliusApiClient, times(1)).getOffenderManagersWithoutUser(nomisIds)
    }

    @Test
    fun `It builds the approval needed caseload successfully with an unallocated case`() {
      val nomisIds = listOf("A1234UU")

      whenever(prisonApproverService.getLicenceCasesReadyForApproval(aListOfPrisonCodes)).thenReturn(
        listOf(
          aLicenceApproverCase(
            licenceId = 2L,
            prisonNumber = "A1234UU",
            forename = "Person",
            surname = "Two",
            prisonCode = "ABC",
            prisonDescription = "ABC (HMP)",
            comUsername = "jdoe",
          ),
        ),
      )
      whenever(deliusApiClient.getOffenderManagersWithoutUser(nomisIds)).thenReturn(
        listOf(
          anUnallocatedCommunityManager(),
        ),
      )

      val approvalCases = service.getApprovalNeeded(aListOfPrisonCodes)

      assertThat(approvalCases).hasSize(1)

      with(approvalCases[0]) {
        assertThat(licenceId).isEqualTo(2L)
        assertThat(name).isEqualTo("Person Two")
        assertThat(prisonerNumber).isEqualTo("A1234UU")
        assertThat(submittedByFullName).isEqualTo("X Y")
        assertThat(releaseDate).isEqualTo((LocalDate.of(2021, 10, 22)))
        assertThat(urgentApproval).isFalse()
        assertThat(approvedBy).isEqualTo("jim smith")
        assertThat(approvedOn).isEqualTo((LocalDateTime.of(2023, 9, 19, 16, 38, 42)))
        with(probationPractitioner) {
          assertThat(staffCode).isEqualTo("A01B02C")
          assertThat(name).isEqualTo("Not allocated")
          assertThat(allocated).isEqualTo(false)
        }
        assertThat(kind).isEqualTo(LicenceKind.CRD)
        assertThat(prisonCode).isEqualTo("ABC")
        assertThat(prisonDescription).isEqualTo("ABC (HMP)")
      }

      verify(prisonApproverService, times(1)).getLicenceCasesReadyForApproval(aListOfPrisonCodes)
      verify(deliusApiClient, times(1)).getOffenderManagersWithoutUser(nomisIds)
    }

    @Test
    fun `the approval needed caseload is sorted correctly`() {
      val nomisIds = listOf("A1234AA", "B1234BB", "C1234CC")

      whenever(prisonApproverService.getLicenceCasesReadyForApproval(aListOfPrisonCodes)).thenReturn(
        listOf(
          aLicenceApproverCase(
            licenceStartDate = LocalDate.of(2024, 6, 21),
          ),
          aLicenceApproverCase(
            licenceId = 2L,
            prisonNumber = "B1234BB",
            forename = "John",
            surname = "Doe",
            prisonCode = "ABC",
            prisonDescription = "ABC (HMP)",
            comUsername = "jdoe",
            licenceStartDate = LocalDate.of(2024, 6, 20),
          ),
          aLicenceApproverCase(
            licenceId = 3L,
            prisonNumber = "C1234CC",
            forename = "Jane",
            surname = "Doe",
            prisonCode = "MDI",
            comUsername = "jdoe",
          ),
        ),
      )
      whenever(deliusApiClient.getOffenderManagersWithoutUser(nomisIds)).thenReturn(
        listOf(
          aCommunityManager(),
          aCommunityManager(
            case = aProbationCaseResult(
              crn = "Y12345",
              croNumber = "DE01/234567F",
              pncNumber = null,
              nomisId = "B1234BB",
            ),
          ),
        ),
      )

      val approvalCases = service.getApprovalNeeded(aListOfPrisonCodes)

      verify(prisonApproverService, times(1)).getLicenceCasesReadyForApproval(aListOfPrisonCodes)
      verify(deliusApiClient, times(1)).getOffenderManagersWithoutUser(nomisIds)

      assertThat(approvalCases).hasSize(2)
      assertThat(approvalCases).extracting<Long> { it.licenceId }.containsExactly(2, 1)
    }

    @Test
    fun `null release dates are surfaced above other release dates for the approval needed caseload`() {
      val nomisIds = listOf("A1234AA", "B1234BB", "C1234CC")

      whenever(prisonApproverService.getLicenceCasesReadyForApproval(aListOfPrisonCodes)).thenReturn(
        listOf(
          aLicenceApproverCase(
            licenceStartDate = LocalDate.of(2024, 6, 21),
          ),
          aLicenceApproverCase(
            licenceId = 2L,
            prisonNumber = "B1234BB",
            forename = "John",
            surname = "Doe",
            prisonCode = "ABC",
            prisonDescription = "ABC (HMP)",
            comUsername = "jdoe",
            licenceStatus = LicenceStatus.APPROVED,
            licenceStartDate = LocalDate.of(2024, 6, 20),
          ),
          aLicenceApproverCase(
            licenceId = 3L,
            prisonNumber = "C1234CC",
            forename = "Jane",
            surname = "Doe",
            prisonCode = "MDI",
            comUsername = "tcom",
            licenceStatus = LicenceStatus.ACTIVE,
            licenceStartDate = null,
          ),
        ),
      )
      whenever(deliusApiClient.getOffenderManagersWithoutUser(nomisIds)).thenReturn(
        listOf(
          aCommunityManager(),
          aCommunityManager(
            case = aProbationCaseResult(
              crn = "Y12345",
              croNumber = "DE01/234567F",
              pncNumber = null,
              nomisId = "B1234BB",
            ),
          ),
          aCommunityManager(
            case = aProbationCaseResult(
              crn = "Z12345",
              croNumber = "GH01/234567I",
              pncNumber = null,
              nomisId = "C1234CC",
            ),
          ),
        ),
      )

      val approvalCases = service.getApprovalNeeded(aListOfPrisonCodes)

      verify(prisonApproverService, times(1)).getLicenceCasesReadyForApproval(aListOfPrisonCodes)
      verify(deliusApiClient, times(1)).getOffenderManagersWithoutUser(nomisIds)

      assertThat(approvalCases).hasSize(3)
      assertThat(approvalCases).extracting<Long> { it.licenceId }.containsExactly(3, 2, 1)
    }

    @Test
    fun `CADM prison caseload is filtered out`() {
      val aListOfPrisonCodes = listOf("ABC", "DEF", "CADM")
      service.getApprovalNeeded(aListOfPrisonCodes)
      verify(prisonApproverService, times(1)).getLicenceCasesReadyForApproval(listOf("ABC", "DEF"))
    }

    @Test
    fun `Missing NOMIS ID on licence returns an empty caseload`() {
      whenever(prisonApproverService.getLicenceCasesReadyForApproval(aListOfPrisonCodes)).thenReturn(
        listOf(
          aLicenceApproverCase(
            prisonNumber = null,
          ),
        ),
      )

      val caseload = service.getApprovalNeeded(aListOfPrisonCodes)

      verify(prisonApproverService, times(1)).getLicenceCasesReadyForApproval(aListOfPrisonCodes)
      verify(deliusApiClient, times(1)).getOffenderManagersWithoutUser(emptyList())
      assertThat(caseload).isEmpty()
    }

    @Test
    fun `It sets the urgent approval flag for a time served licence`() {
      val nomisIds = listOf("A1234AA")

      whenever(prisonApproverService.getLicenceCasesReadyForApproval(aListOfPrisonCodes)).thenReturn(
        listOf(
          aLicenceApproverCase(kind = LicenceKind.TIME_SERVED),
        ),
      )
      whenever(deliusApiClient.getOffenderManagersWithoutUser(nomisIds)).thenReturn(
        listOf(aCommunityManager()),
      )

      val approvalCases = service.getApprovalNeeded(aListOfPrisonCodes)

      assertThat(approvalCases).hasSize(1)

      with(approvalCases[0]) {
        assertThat(licenceId).isEqualTo(1L)
        assertThat(name).isEqualTo("Person One")
        assertThat(prisonerNumber).isEqualTo("A1234AA")
        assertThat(submittedByFullName).isEqualTo("X Y")
        assertThat(releaseDate).isEqualTo((LocalDate.of(2021, 10, 22)))
        assertThat(urgentApproval).isTrue()
        assertThat(approvedBy).isEqualTo("jim smith")
        assertThat(approvedOn).isEqualTo((LocalDateTime.of(2023, 9, 19, 16, 38, 42)))
        with(probationPractitioner) {
          assertThat(staffCode).isEqualTo("AB012C")
          assertThat(name).isEqualTo("Test Test")
          assertThat(allocated).isEqualTo(true)
        }
        assertThat(kind).isEqualTo(LicenceKind.TIME_SERVED)
        assertThat(prisonCode).isEqualTo("MDI")
        assertThat(prisonDescription).isEqualTo("Moorland (HMP)")
      }
    }
  }

  @Nested
  inner class `Build recently approved caseload` {
    @Test
    fun `It builds the recently approved caseload successfully with multiple cases`() {
      val nomisIds = listOf("B1234BB", "C1234CC")

      whenever(prisonApproverService.findRecentlyApprovedLicenceCases(aListOfPrisonCodes)).thenReturn(
        listOf(
          aLicenceApproverCase(
            licenceId = 2L,
            prisonNumber = "B1234BB",
            forename = "John",
            surname = "Doe",
            prisonCode = "ABC",
            prisonDescription = "ABC (HMP)",
            comUsername = "jdoe",
            licenceStatus = LicenceStatus.APPROVED,
            licenceStartDate = LocalDate.now().minusDays(14),
            approvedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
          ),
          aLicenceApproverCase(
            licenceId = 3L,
            prisonNumber = "C1234CC",
            forename = "Jane",
            surname = "Doe",
            prisonCode = "MDI",
            comUsername = "tcom",
            licenceStatus = LicenceStatus.ACTIVE,
            licenceStartDate = LocalDate.now().minusDays(14),
            approvedDate = LocalDateTime.of(2023, 9, 29, 16, 38, 42),
          ),
        ),
      )
      whenever(deliusApiClient.getOffenderManagersWithoutUser(nomisIds)).thenReturn(
        listOf(
          aCommunityManager(
            case = aProbationCaseResult(
              crn = "Y12345",
              croNumber = "DE01/234567F",
              pncNumber = null,
              nomisId = "B1234BB",
            ),
          ),
          aCommunityManager(
            case = aProbationCaseResult(
              crn = "Z12345",
              croNumber = "GH01/234567I",
              pncNumber = null,
              nomisId = "C1234CC",
            ),
          ),
        ),
      )

      val caseload = service.getRecentlyApproved(aListOfPrisonCodes)

      assertThat(caseload).hasSize(2)

      with(caseload[0]) {
        assertThat(licenceId).isEqualTo(3L)
        assertThat(name).isEqualTo("Jane Doe")
        assertThat(prisonerNumber).isEqualTo("C1234CC")
        assertThat(submittedByFullName).isEqualTo("X Y")
        assertThat(releaseDate).isEqualTo(LocalDate.now().minusDays(14))
        assertThat(urgentApproval).isFalse()
        assertThat(approvedBy).isEqualTo("jim smith")
        assertThat(approvedOn).isEqualTo(LocalDateTime.of(2023, 9, 29, 16, 38, 42))
        with(probationPractitioner) {
          assertThat(staffCode).isEqualTo("AB012C")
          assertThat(name).isEqualTo("Test Test")
          assertThat(allocated).isEqualTo(true)
        }
        assertThat(kind).isEqualTo(LicenceKind.CRD)
        assertThat(prisonCode).isEqualTo("MDI")
        assertThat(prisonDescription).isEqualTo("Moorland (HMP)")
      }

      with(caseload[1]) {
        assertThat(licenceId).isEqualTo(2L)
        assertThat(name).isEqualTo("John Doe")
        assertThat(prisonerNumber).isEqualTo("B1234BB")
        assertThat(submittedByFullName).isEqualTo("X Y")
        assertThat(releaseDate).isEqualTo(LocalDate.now().minusDays(14))
        assertThat(urgentApproval).isFalse()
        assertThat(approvedBy).isEqualTo("jim smith")
        assertThat(approvedOn).isEqualTo((LocalDateTime.of(2023, 9, 19, 16, 38, 42)))
        with(probationPractitioner) {
          assertThat(staffCode).isEqualTo("AB012C")
          assertThat(name).isEqualTo("Test Test")
          assertThat(allocated).isEqualTo(true)
        }
        assertThat(kind).isEqualTo(LicenceKind.CRD)
        assertThat(prisonCode).isEqualTo("ABC")
        assertThat(prisonDescription).isEqualTo("ABC (HMP)")
      }

      verify(prisonApproverService, times(1)).findRecentlyApprovedLicenceCases(aListOfPrisonCodes)
      verify(deliusApiClient, times(1)).getOffenderManagersWithoutUser(nomisIds)
    }

    @Test
    fun `the recently approved caseload is sorted correctly`() {
      val nomisIds = listOf("B1234BB", "C1234CC")

      whenever(prisonApproverService.findRecentlyApprovedLicenceCases(aListOfPrisonCodes)).thenReturn(
        listOf(
          aLicenceApproverCase(
            licenceId = 2L,
            prisonNumber = "B1234BB",
            forename = "John",
            surname = "Doe",
            prisonCode = "ABC",
            prisonDescription = "ABC (HMP)",
            comUsername = "jdoe",
            licenceStatus = LicenceStatus.APPROVED,
            licenceStartDate = LocalDate.now().minusDays(13),
            approvedDate = LocalDateTime.of(2023, 9, 29, 16, 38, 42),
          ),
          aLicenceApproverCase(
            licenceId = 3L,
            prisonNumber = "C1234CC",
            forename = "Jane",
            surname = "Doe",
            prisonCode = "MDI",
            comUsername = "tcom",
            licenceStatus = LicenceStatus.ACTIVE,
            licenceStartDate = LocalDate.now().minusDays(14),
            approvedDate = LocalDateTime.of(2023, 9, 28, 16, 38, 42),
          ),
        ),
      )
      whenever(deliusApiClient.getOffenderManagersWithoutUser(nomisIds)).thenReturn(
        listOf(
          aCommunityManager(
            case = aProbationCaseResult(
              crn = "Y12345",
              croNumber = "DE01/234567F",
              pncNumber = null,
              nomisId = "B1234BB",
            ),
          ),
          aCommunityManager(
            case = aProbationCaseResult(
              crn = "Z12345",
              croNumber = "GH01/234567I",
              pncNumber = null,
              nomisId = "C1234CC",
            ),
          ),
        ),
      )

      val caseload = service.getRecentlyApproved(aListOfPrisonCodes)

      verify(prisonApproverService, times(1)).findRecentlyApprovedLicenceCases(aListOfPrisonCodes)
      verify(deliusApiClient, times(1)).getOffenderManagersWithoutUser(nomisIds)

      assertThat(caseload).hasSize(2)
      assertThat(caseload).extracting<Long> { it.licenceId }.containsExactly(2, 3)
    }

    @Test
    fun `CADM prison code is filtered out`() {
      val aListOfPrisonCodes = listOf("ABC", "DEF", "CADM")
      service.getRecentlyApproved(aListOfPrisonCodes)
      verify(prisonApproverService, times(1)).findRecentlyApprovedLicenceCases(listOf("ABC", "DEF"))
    }

    @Test
    fun `a null release date is returned when LSD is not set`() {
      val nomisId = "A1234AA"
      whenever(prisonApproverService.findRecentlyApprovedLicenceCases(aListOfPrisonCodes)).thenReturn(
        listOf(
          aLicenceApproverCase(
            licenceStartDate = null,
          ),
        ),
      )
      whenever(deliusApiClient.getOffenderManagersWithoutUser(listOf(nomisId))).thenReturn(listOf(aCommunityManager()))

      val approvalCases = service.getRecentlyApproved(aListOfPrisonCodes)

      assertThat(approvalCases).hasSize(1)

      with(approvalCases.first()) {
        assertThat(releaseDate).isNull()
      }

      verify(prisonApproverService, times(1)).findRecentlyApprovedLicenceCases(aListOfPrisonCodes)
      verify(deliusApiClient, times(1)).getOffenderManagersWithoutUser(listOf(nomisId))
    }

    @Test
    fun `It derives if urgent approval is needed`() {
      val nomisId = "A1234AA"

      whenever(releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(any())).thenReturn(
        true,
      )

      whenever(prisonApproverService.findRecentlyApprovedLicenceCases(aListOfPrisonCodes)).thenReturn(
        listOf(
          aLicenceApproverCase(),
        ),
      )
      whenever(deliusApiClient.getOffenderManagersWithoutUser(listOf(nomisId))).thenReturn(listOf(aCommunityManager()))

      val approvalCases = service.getRecentlyApproved(aListOfPrisonCodes)

      assertThat(approvalCases).hasSize(1)

      with(approvalCases.first()) {
        assertThat(urgentApproval).isTrue()
      }

      verify(prisonApproverService, times(1)).findRecentlyApprovedLicenceCases(aListOfPrisonCodes)
      verify(deliusApiClient, times(1)).getOffenderManagersWithoutUser(listOf(nomisId))
    }
  }

  private fun aLicenceApproverCase(
    licenceId: Long = 1L,
    licenceStartDate: LocalDate? = LocalDate.of(2021, 10, 22),
    kind: LicenceKind = LicenceKind.CRD,
    versionOfId: Long? = null,
    licenceStatus: LicenceStatus = LicenceStatus.SUBMITTED,
    prisonNumber: String? = "A1234AA",
    surname: String? = "One",
    forename: String? = "Person",
    updatedByFirstName: String? = "X",
    updatedByLastName: String? = "Y",
    comUsername: String? = "tcom",
    sentenceStartDate: LocalDate? = LocalDate.of(2021, 10, 19),
    conditionalReleaseDate: LocalDate? = LocalDate.of(2021, 10, 22),
    actualReleaseDate: LocalDate? = LocalDate.of(2021, 10, 22),
    postRecallReleaseDate: LocalDate? = null,
    approvedByName: String? = "jim smith",
    approvedDate: LocalDateTime? = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
    submittedByFullName: String? = "X Y",
    prisonCode: String? = "MDI",
    prisonDescription: String? = "Moorland (HMP)",
    variationOfId: Long? = null,
  ): LicenceApproverCase {
    val licenceApproverCase = LicenceApproverCase(
      licenceStartDate = licenceStartDate,
      kind = kind,
      licenceId = licenceId,
      versionOfId = versionOfId,
      statusCode = licenceStatus,
      prisonNumber = prisonNumber,
      surname = surname,
      forename = forename,
      updatedByFirstName = updatedByFirstName,
      updatedByLastName = updatedByLastName,
      comUsername = comUsername,
      sentenceStartDate = sentenceStartDate,
      conditionalReleaseDate = conditionalReleaseDate,
      actualReleaseDate = actualReleaseDate,
      postRecallReleaseDate = postRecallReleaseDate,
      approvedByName = approvedByName,
      approvedDate = approvedDate,
      prisonCode = prisonCode,
      prisonDescription = prisonDescription,
      variationOfId = variationOfId,
    )
    licenceApproverCase.submittedByFullName = submittedByFullName
    return licenceApproverCase
  }

  private fun aProbationCaseResult(
    crn: String = "X12345",
    croNumber: String = "AB01/234567C",
    pncNumber: String? = null,
    nomisId: String = "A1234AA",
  ) = ProbationCase(
    crn = crn,
    croNumber = croNumber,
    pncNumber = pncNumber,
    nomisId = nomisId,
  )

  private fun aCommunityManager(
    code: String = "AB012C",
    id: Long = 2000L,
    team: TeamDetail = TeamDetail(
      code = "NA01A2-A",
      description = "Cardiff South Team A",
      borough = Detail("N01A", "Cardiff"),
      district = Detail("N01A2", "Cardiff South"),
      provider = Detail("N01", "Wales"),
    ),
    provider: Detail = Detail("N01", "Wales"),
    case: ProbationCase = aProbationCaseResult(),
    name: Name = Name("Test", null, "Test"),
    allocationDate: LocalDate = LocalDate.of(2000, 1, 1),
    unallocated: Boolean = false,
  ) = CommunityManagerWithoutUser(
    code = code,
    id = id,
    team = team,
    provider = provider,
    case = case,
    name = name,
    allocationDate = allocationDate,
    unallocated = unallocated,
  )

  private fun anUnallocatedCommunityManager(
    code: String = "A01B02C",
    id: Long = 2000L,
    team: TeamDetail = TeamDetail(
      code = "A01B02",
      description = "Test Team",
      borough = Detail("A01B02", "description"),
      district = Detail("A01B02", "description"),
      provider = Detail("probationArea-code-1", "probationArea-description-1"),
    ),
    provider: Detail = Detail("N01", "Wales"),
    case: ProbationCase = aProbationCaseResult(
      crn = "X12345",
      croNumber = "AB01/234567C",
      pncNumber = null,
      nomisId = "A1234UU",
    ),
    name: Name = Name("Staff", null, "Surname"),
    allocationDate: LocalDate = LocalDate.of(2000, 1, 1),
    unallocated: Boolean = true,
  ) = CommunityManagerWithoutUser(
    code = code,
    id = id,
    team = team,
    provider = provider,
    case = case,
    name = name,
    allocationDate = allocationDate,
    unallocated = unallocated,
  )

  private companion object {
    val aListOfPrisonCodes = listOf("MDI", "ABC")
  }
}
