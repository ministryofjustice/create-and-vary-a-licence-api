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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Detail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.TeamDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.StaffNameResponse
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
    reset(prisonApproverService, deliusApiClient)
  }

  @Nested
  inner class `Build getApprovalNeeded caseload` {
    @Nested
    inner class `Build approval caseload` {
      @Test
      fun `It builds the approval needed caseload successfully`() {
        val nomisId = "A1234AA"
        val comUsernames = listOf("tcom")

        whenever(prisonApproverService.getLicenceCasesReadyForApproval(aListOfPrisonCodes)).thenReturn(
          listOf(
            aLicenceApproverCase(),
          ),
        )
        whenever(deliusApiClient.getOffenderManagers(listOf(nomisId))).thenReturn(listOf(aCommunityManager()))
        whenever(deliusApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(listOf(aStaffNameResponse()))

        val approvalCases = service.getSortedApprovalNeededCases(aListOfPrisonCodes)

        assertThat(approvalCases).hasSize(1)

        with(approvalCases.first()) {
          assertThat(licenceId).isEqualTo(1L)
          assertThat(name).isEqualTo("Person One")
          assertThat(prisonerNumber).isEqualTo("A1234AA")
          assertThat(submittedByFullName).isEqualTo("X Y")
          assertThat(releaseDate).isEqualTo((LocalDate.of(2021, 10, 22)))
          assertThat(urgentApproval).isFalse()
          assertThat(approvedBy).isEqualTo("jim smith")
          assertThat(approvedOn).isEqualTo((LocalDateTime.of(2023, 9, 19, 16, 38, 42)))
          with(probationPractitioner!!) {
            assertThat(staffCode).isEqualTo("AB012C")
            assertThat(name).isEqualTo("Test Test")
          }
          assertThat(kind).isEqualTo(LicenceKind.CRD)
          assertThat(prisonCode).isEqualTo("MDI")
          assertThat(prisonDescription).isEqualTo("Moorland (HMP)")
        }

        verify(prisonApproverService, times(1)).getLicenceCasesReadyForApproval(aListOfPrisonCodes)
        verify(deliusApiClient, times(1)).getOffenderManagers(listOf(nomisId))
        verify(deliusApiClient, times(1)).getStaffDetailsByUsername(comUsernames)
      }

      @Test
      fun `It builds the approval needed caseload successfully with multiple cases`() {
        val nomisIds = listOf("A1234AA", "B1234BB", "C1234CC")
        val comUsernames = listOf("tcom", "jdoe")

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
        whenever(deliusApiClient.getOffenderManagers(nomisIds)).thenReturn(
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
        whenever(deliusApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(
          listOf(
            aStaffNameResponse(),
            aStaffNameResponse(
              id = 3000,
              username = "jdoe",
              forename = "Test2",
              surname = "Test2",
              code = "DE012F",
            ),
          ),
        )

        val approvalCases = service.getSortedApprovalNeededCases(aListOfPrisonCodes)

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
          with(probationPractitioner!!) {
            assertThat(staffCode).isEqualTo("DE012F")
            assertThat(name).isEqualTo("Test2 Test2")
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
          with(probationPractitioner!!) {
            assertThat(staffCode).isEqualTo("AB012C")
            assertThat(name).isEqualTo("Test Test")
          }
          assertThat(kind).isEqualTo(LicenceKind.CRD)
          assertThat(prisonCode).isEqualTo("MDI")
          assertThat(prisonDescription).isEqualTo("Moorland (HMP)")
        }

        verify(prisonApproverService, times(1)).getLicenceCasesReadyForApproval(aListOfPrisonCodes)
        verify(deliusApiClient, times(1)).getOffenderManagers(nomisIds)
        verify(deliusApiClient, times(1)).getStaffDetailsByUsername(comUsernames)
      }

      @Test
      fun `the approval needed caseload is sorted correctly`() {
        val nomisIds = listOf("A1234AA", "B1234BB", "C1234CC")
        val comUsernames = listOf("tcom", "jdoe")

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
        whenever(deliusApiClient.getOffenderManagers(nomisIds)).thenReturn(
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
        whenever(deliusApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(
          listOf(
            aStaffNameResponse(),
            aStaffNameResponse(
              id = 3000,
              username = "jdoe",
              forename = "Test2",
              surname = "Test2",
              code = "DE012F",
            ),
          ),
        )

        val approvalCases = service.getSortedApprovalNeededCases(aListOfPrisonCodes)

        verify(prisonApproverService, times(1)).getLicenceCasesReadyForApproval(aListOfPrisonCodes)
        verify(deliusApiClient, times(1)).getOffenderManagers(nomisIds)
        verify(deliusApiClient, times(1)).getStaffDetailsByUsername(comUsernames)

        assertThat(approvalCases).hasSize(2)
        assertThat(approvalCases).extracting<Long> { it.licenceId }.containsExactly(2, 1)
      }

      @Test
      fun `null release dates are surfaced above other release dates for the approval needed caseload`() {
        val nomisIds = listOf("A1234AA", "B1234BB", "C1234CC")
        val comUsernames = listOf("tcom", "jdoe", "tcom")

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
        whenever(deliusApiClient.getOffenderManagers(nomisIds)).thenReturn(
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
        whenever(deliusApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(
          listOf(
            aStaffNameResponse(),
            aStaffNameResponse(
              id = 3000,
              username = "jdoe",
              forename = "Test2",
              surname = "Test2",
              code = "DE012F",
            ),
            aStaffNameResponse(),
          ),
        )

        val approvalCases = service.getSortedApprovalNeededCases(aListOfPrisonCodes)

        verify(prisonApproverService, times(1)).getLicenceCasesReadyForApproval(aListOfPrisonCodes)
        verify(deliusApiClient, times(1)).getOffenderManagers(nomisIds)
        verify(deliusApiClient, times(1)).getStaffDetailsByUsername(comUsernames)

        assertThat(approvalCases).hasSize(3)
        assertThat(approvalCases).extracting<Long> { it.licenceId }.containsExactly(3, 2, 1)
      }
    }

    @Test
    fun `CADM prison caseload is filtered out`() {
      val aListOfPrisonCodes = listOf("ABC", "DEF", "CADM")
      service.getSortedApprovalNeededCases(aListOfPrisonCodes)
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

      val caseload = service.getSortedApprovalNeededCases(aListOfPrisonCodes)

      verify(prisonApproverService, times(1)).getLicenceCasesReadyForApproval(aListOfPrisonCodes)
      verify(deliusApiClient, times(1)).getOffenderManagers(emptyList())
      verify(deliusApiClient, times(1)).getStaffDetailsByUsername(emptyList())
      assertThat(caseload).isEmpty()
    }

    @Test
    fun `LSD is selected for releaseDate`() {
      val nomisId = "A1234AA"
      val comUsernames = listOf("tcom")

      whenever(prisonApproverService.getLicenceCasesReadyForApproval(aListOfPrisonCodes)).thenReturn(
        listOf(
          aLicenceApproverCase(
            actualReleaseDate = LocalDate.of(2024, 6, 20),
            conditionalReleaseDate = LocalDate.of(2024, 6, 19),
            licenceStartDate = LocalDate.of(2024, 6, 18),
          ),
        ),
      )
      whenever(deliusApiClient.getOffenderManagers(listOf(nomisId))).thenReturn(listOf(aCommunityManager()))
      whenever(deliusApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(listOf(aStaffNameResponse()))

      val approvalCases = service.getSortedApprovalNeededCases(aListOfPrisonCodes)

      assertThat(approvalCases).hasSize(1)

      with(approvalCases.first()) {
        assertThat(releaseDate).isEqualTo((LocalDate.of(2024, 6, 18)))
      }

      verify(prisonApproverService, times(1)).getLicenceCasesReadyForApproval(aListOfPrisonCodes)
      verify(deliusApiClient, times(1)).getOffenderManagers(listOf(nomisId))
      verify(deliusApiClient, times(1)).getStaffDetailsByUsername(comUsernames)
    }
  }

  @Nested
  inner class `Build getRecentlyApproved caseload` {

    @Nested
    inner class `Build recently approved caseload` {
      @Test
      fun `It builds the recently approved caseload successfully`() {
        val nomisId = "A1234AA"
        val comUsernames = listOf("tcom")

        val aLicenceSummaryApproverView = aLicenceApproverCase(
          licenceStatus = LicenceStatus.APPROVED,
          licenceStartDate = LocalDate.now().minusDays(14),
        )

        whenever(prisonApproverService.findRecentlyApprovedLicenceCases(aListOfPrisonCodes)).thenReturn(
          listOf(
            aLicenceSummaryApproverView,
          ),
        )
        whenever(deliusApiClient.getOffenderManagers(listOf(nomisId))).thenReturn(listOf(aCommunityManager()))
        whenever(deliusApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(listOf(aStaffNameResponse()))

        val approvalCases = service.getSortedRecentlyApprovedCases(aListOfPrisonCodes)

        assertThat(approvalCases).hasSize(1)

        with(approvalCases.first()) {
          assertThat(licenceId).isEqualTo(1L)
          assertThat(name).isEqualTo("Person One")
          assertThat(prisonerNumber).isEqualTo("A1234AA")
          assertThat(submittedByFullName).isEqualTo("X Y")
          assertThat(releaseDate).isEqualTo((LocalDate.now().minusDays(14)))
          assertThat(urgentApproval).isFalse()
          assertThat(approvedBy).isEqualTo("jim smith")
          assertThat(approvedOn).isEqualTo((LocalDateTime.of(2023, 9, 19, 16, 38, 42)))
          with(probationPractitioner!!) {
            assertThat(staffCode).isEqualTo("AB012C")
            assertThat(name).isEqualTo("Test Test")
          }
          assertThat(kind).isEqualTo(LicenceKind.CRD)
          assertThat(prisonCode).isEqualTo("MDI")
          assertThat(prisonDescription).isEqualTo("Moorland (HMP)")
        }

        verify(prisonApproverService, times(1)).findRecentlyApprovedLicenceCases(aListOfPrisonCodes)
        verify(deliusApiClient, times(1)).getOffenderManagers(listOf(nomisId))
        verify(deliusApiClient, times(1)).getStaffDetailsByUsername(comUsernames)
      }

      @Test
      fun `It builds the recently approved caseload successfully with multiple cases`() {
        val nomisIds = listOf("B1234BB", "C1234CC")
        val comUsernames = listOf("jdoe", "tcom")

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
        whenever(deliusApiClient.getOffenderManagers(nomisIds)).thenReturn(
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

        whenever(deliusApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(
          listOf(
            aStaffNameResponse(
              id = 3000,
              username = "jdoe",
              forename = "Test2",
              surname = "Test2",
              code = "DE012F",
            ),
            aStaffNameResponse(),
          ),
        )

        val caseload = service.getSortedRecentlyApprovedCases(aListOfPrisonCodes)

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
          with(probationPractitioner!!) {
            assertThat(staffCode).isEqualTo("AB012C")
            assertThat(name).isEqualTo("Test Test")
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
          with(probationPractitioner!!) {
            assertThat(staffCode).isEqualTo("DE012F")
            assertThat(name).isEqualTo("Test2 Test2")
          }
          assertThat(kind).isEqualTo(LicenceKind.CRD)
          assertThat(prisonCode).isEqualTo("ABC")
          assertThat(prisonDescription).isEqualTo("ABC (HMP)")
        }

        verify(prisonApproverService, times(1)).findRecentlyApprovedLicenceCases(aListOfPrisonCodes)
        verify(deliusApiClient, times(1)).getOffenderManagers(nomisIds)
        verify(deliusApiClient, times(1)).getStaffDetailsByUsername(comUsernames)
      }

      @Test
      fun `the recently approved caseload is sorted correctly`() {
        val nomisIds = listOf("B1234BB", "C1234CC")
        val comUsernames = listOf("jdoe", "tcom")

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
        whenever(deliusApiClient.getOffenderManagers(nomisIds)).thenReturn(
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
        whenever(deliusApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(
          listOf(
            aStaffNameResponse(
              id = 3000,
              username = "jdoe",
              forename = "Test2",
              surname = "Test2",
              code = "DE012F",
            ),
            aStaffNameResponse(),
          ),
        )

        val caseload = service.getSortedRecentlyApprovedCases(aListOfPrisonCodes)

        verify(prisonApproverService, times(1)).findRecentlyApprovedLicenceCases(aListOfPrisonCodes)
        verify(deliusApiClient, times(1)).getOffenderManagers(nomisIds)
        verify(deliusApiClient, times(1)).getStaffDetailsByUsername(comUsernames)

        assertThat(caseload).hasSize(2)
        assertThat(caseload).extracting<Long> { it.licenceId }.containsExactly(2, 3)
      }
    }

    @Test
    fun `CADM prison code is filtered out`() {
      val aListOfPrisonCodes = listOf("ABC", "DEF", "CADM")
      service.getSortedRecentlyApprovedCases(aListOfPrisonCodes)
      verify(prisonApproverService, times(1)).findRecentlyApprovedLicenceCases(listOf("ABC", "DEF"))
    }

    @Test
    fun `a null release date is returned when LSD is not set`() {
      val nomisId = "A1234AA"
      val comUsernames = listOf("tcom")

      whenever(prisonApproverService.findRecentlyApprovedLicenceCases(aListOfPrisonCodes)).thenReturn(
        listOf(
          aLicenceApproverCase(
            licenceStartDate = null,
          ),
        ),
      )
      whenever(deliusApiClient.getOffenderManagers(listOf(nomisId))).thenReturn(listOf(aCommunityManager()))
      whenever(deliusApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(listOf(aStaffNameResponse()))

      val approvalCases = service.getSortedRecentlyApprovedCases(aListOfPrisonCodes)

      assertThat(approvalCases).hasSize(1)

      with(approvalCases.first()) {
        assertThat(releaseDate).isNull()
      }

      verify(prisonApproverService, times(1)).findRecentlyApprovedLicenceCases(aListOfPrisonCodes)
      verify(deliusApiClient, times(1)).getOffenderManagers(listOf(nomisId))
      verify(deliusApiClient, times(1)).getStaffDetailsByUsername(comUsernames)
    }

    @Test
    fun `It derives if urgent approval is needed`() {
      val nomisId = "A1234AA"
      val comUsernames = listOf("tcom")

      whenever(releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(any())).thenReturn(
        true,
      )

      whenever(prisonApproverService.findRecentlyApprovedLicenceCases(aListOfPrisonCodes)).thenReturn(
        listOf(
          aLicenceApproverCase(),
        ),
      )
      whenever(deliusApiClient.getOffenderManagers(listOf(nomisId))).thenReturn(listOf(aCommunityManager()))
      whenever(deliusApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(listOf(aStaffNameResponse()))

      val approvalCases = service.getSortedRecentlyApprovedCases(aListOfPrisonCodes)

      assertThat(approvalCases).hasSize(1)

      with(approvalCases.first()) {
        assertThat(urgentApproval).isTrue()
      }

      verify(prisonApproverService, times(1)).findRecentlyApprovedLicenceCases(aListOfPrisonCodes)
      verify(deliusApiClient, times(1)).getOffenderManagers(listOf(nomisId))
      verify(deliusApiClient, times(1)).getStaffDetailsByUsername(comUsernames)
    }
  }

  @Nested
  inner class `Retrieve the correct probation practitioner` {
    @Test
    fun `The correct com is retrieved`() {
      val users = listOf(
        aStaffNameResponse(),
        aStaffNameResponse(
          id = 1000,
          username = "jdoe",
          forename = "Test2",
          surname = "Test2",
          code = "CD012E",
        ),
      )

      val com = service.findProbationPractitioner(aLicenceApproverCase().comUsername, users, aCommunityManager())

      assertThat(com?.staffCode).isEqualTo("AB012C")
      assertThat(com?.name).isEqualTo("Test Test")
    }

    @Test
    fun `If the com is not found in the list of coms, the delius record details are used instead`() {
      val users = listOf(
        aStaffNameResponse(
          username = "test1",
          code = "test1",
          forename = "Test1",
          surname = "Test1",
        ),
        aStaffNameResponse(
          username = "test2",
          code = "test2",
          forename = "Test2",
          surname = "Test2",
        ),
      )

      val com = service.findProbationPractitioner(aLicenceApproverCase().comUsername, users, aCommunityManager())

      assertThat(com?.staffCode).isEqualTo("AB012C")
      assertThat(com?.name).isEqualTo("Test Test")
    }

    @Test
    fun `return null if com is unallocated`() {
      val users = listOf(
        aStaffNameResponse(
          username = "test1",
          code = "test1",
          forename = "Test1",
          surname = "Test1",
        ),
        aStaffNameResponse(
          username = "test2",
          code = "test2",
          forename = "Test2",
          surname = "Test2",
        ),
      )
      val unallocatedCom = aCommunityManager(unallocated = true)

      val com = service.findProbationPractitioner(aLicenceApproverCase().comUsername, users, unallocatedCom)

      assertThat(com).isNull()
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

  private fun aStaffNameResponse(
    id: Long = 2000,
    username: String = "tcom",
    forename: String = "Test",
    surname: String = "Test",
    code: String = "AB012C",
  ) = StaffNameResponse(
    id = id,
    username = username,
    name = Name(forename, surname = surname),
    code = code,
  )

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
  ) = CommunityManager(
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
