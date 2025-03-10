package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummaryApproverView
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonApproverService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OffenderDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OtherIds
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchStaffDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.User
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

class ApproverCaseloadServiceTest {
  private val prisonApproverService = mock<PrisonApproverService>()
  private val probationSearchApiClient = mock<ProbationSearchApiClient>()
  private val deliusApiClient = mock<DeliusApiClient>()

  private val service = ApproverCaseloadService(prisonApproverService, probationSearchApiClient, deliusApiClient)

  @BeforeEach
  fun reset() {
    reset(prisonApproverService, probationSearchApiClient, deliusApiClient)
  }

  @Nested
  inner class `Build getApprovalNeeded caseload` {
    @Nested
    inner class `Build approval caseload` {
      @Test
      fun `It builds the approval needed caseload successfully`() {
        val nomisId = "A1234AA"
        val comUsernames = listOf("smills")

        whenever(prisonApproverService.getLicencesForApproval(aListOfPrisonCodes)).thenReturn(
          listOf(
            aLicenceSummaryApproverView,
          ),
        )
        whenever(probationSearchApiClient.searchForPeopleByNomsNumber(listOf(nomisId))).thenReturn(
          listOf(anOffenderDetailResult),
        )
        whenever(deliusApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(listOf(aUser))

        val approvalCases = service.getApprovalNeeded(aListOfPrisonCodes)

        assertThat(approvalCases).hasSize(1)

        with(approvalCases.first()) {
          assertThat(licenceId).isEqualTo(1L)
          assertThat(name).isEqualTo("Bob Mortimer")
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
          assertThat(kind).isEqualTo(LicenceKind.CRD)
        }

        verify(prisonApproverService, times(1)).getLicencesForApproval(aListOfPrisonCodes)
        verify(probationSearchApiClient, times(1)).searchForPeopleByNomsNumber(listOf(nomisId))
        verify(deliusApiClient, times(1)).getStaffDetailsByUsername(comUsernames)
      }

      @Test
      fun `It builds the approval needed caseload successfully with multiple cases`() {
        val nomisIds = listOf("A1234AA", "B1234BB", "C1234CC")
        val comUsernames = listOf("smills", "jdoe")

        whenever(prisonApproverService.getLicencesForApproval(aListOfPrisonCodes)).thenReturn(
          listOf(
            aLicenceSummaryApproverView,
            aLicenceSummaryApproverView.copy(
              licenceId = 2L,
              bookingId = 12345,
              nomisId = "B1234BB",
              crn = "Y12345",
              forename = "John",
              surname = "Doe",
              prisonCode = "ABC",
              prisonDescription = "ABC (HMP)",
              comUsername = "jdoe",
            ),
            aLicenceSummaryApproverView.copy(
              licenceId = 3L,
              bookingId = 67890,
              nomisId = "C1234CC",
              crn = "Z12345",
              forename = "Jane",
              surname = "Doe",
              prisonCode = "MDI",
              comUsername = "jdoe",
            ),
          ),
        )
        whenever(probationSearchApiClient.searchForPeopleByNomsNumber(nomisIds)).thenReturn(
          listOf(
            anOffenderDetailResult,
            anOffenderDetailResult.copy(
              otherIds = OtherIds(
                crn = "Y12345",
                croNumber = "DE01/234567F",
                pncNumber = null,
                nomsNumber = "B1234BB",
              ),
              offenderManagers = listOf(
                OffenderManager(
                  staffDetail = ProbationSearchStaffDetail(
                    code = "DE012F",
                    forenames = "Test2",
                    surname = "Test2",
                    unallocated = false,
                  ),
                  active = true,
                ),
              ),
            ),
          ),
        )
        whenever(deliusApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(
          listOf(
            aUser,
            aUser.copy(
              id = 3000,
              username = "jdoe",
              email = "testemail2@probation.gov.uk",
              name = Name(
                forename = "Test2",
                surname = "Test2",
              ),
              teams = emptyList(),
              code = "DE012F",
            ),
          ),
        )

        val approvalCases = service.getApprovalNeeded(aListOfPrisonCodes)

        assertThat(approvalCases).hasSize(2)

        with(approvalCases.first()) {
          assertThat(licenceId).isEqualTo(1L)
          assertThat(name).isEqualTo("Bob Mortimer")
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
          assertThat(kind).isEqualTo(LicenceKind.CRD)
        }

        with(approvalCases.last()) {
          assertThat(licenceId).isEqualTo(2L)
          assertThat(name).isEqualTo("John Doe")
          assertThat(prisonerNumber).isEqualTo("B1234BB")
          assertThat(submittedByFullName).isEqualTo("X Y")
          assertThat(releaseDate).isEqualTo((LocalDate.of(2021, 10, 22)))
          assertThat(urgentApproval).isFalse()
          assertThat(approvedBy).isEqualTo("jim smith")
          assertThat(approvedOn).isEqualTo((LocalDateTime.of(2023, 9, 19, 16, 38, 42)))
          assertThat(isDueForEarlyRelease).isFalse()
          with(probationPractitioner!!) {
            assertThat(staffCode).isEqualTo("DE012F")
            assertThat(name).isEqualTo("Test2 Test2")
          }
          assertThat(kind).isEqualTo(LicenceKind.CRD)
        }

        verify(prisonApproverService, times(1)).getLicencesForApproval(aListOfPrisonCodes)
        verify(probationSearchApiClient, times(1)).searchForPeopleByNomsNumber(nomisIds)
        verify(deliusApiClient, times(1)).getStaffDetailsByUsername(comUsernames)
      }

      @Test
      fun `the approval needed caseload is sorted correctly`() {
        val nomisIds = listOf("A1234AA", "B1234BB", "C1234CC")
        val comUsernames = listOf("smills", "jdoe")

        whenever(prisonApproverService.getLicencesForApproval(aListOfPrisonCodes)).thenReturn(
          listOf(
            aLicenceSummaryApproverView.copy(
              licenceStartDate = LocalDate.of(2024, 6, 21),
            ),
            aLicenceSummaryApproverView.copy(
              licenceId = 2L,
              bookingId = 12345,
              nomisId = "B1234BB",
              crn = "Y12345",
              forename = "John",
              surname = "Doe",
              prisonCode = "ABC",
              prisonDescription = "ABC (HMP)",
              comUsername = "jdoe",
              licenceStartDate = LocalDate.of(2024, 6, 20),
            ),
            aLicenceSummaryApproverView.copy(
              licenceId = 3L,
              bookingId = 67890,
              nomisId = "C1234CC",
              crn = "Z12345",
              forename = "Jane",
              surname = "Doe",
              prisonCode = "MDI",
              comUsername = "jdoe",
            ),
          ),
        )
        whenever(probationSearchApiClient.searchForPeopleByNomsNumber(nomisIds)).thenReturn(
          listOf(
            anOffenderDetailResult,
            anOffenderDetailResult.copy(
              otherIds = OtherIds(
                crn = "Y12345",
                croNumber = "DE01/234567F",
                pncNumber = null,
                nomsNumber = "B1234BB",
              ),
              offenderManagers = listOf(
                OffenderManager(
                  staffDetail = ProbationSearchStaffDetail(
                    code = "DE012F",
                    forenames = "Test2",
                    surname = "Test2",
                    unallocated = false,
                  ),
                  active = true,
                ),
              ),
            ),
          ),
        )
        whenever(deliusApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(
          listOf(
            aUser,
            aUser.copy(
              id = 3000,
              username = "jdoe",
              email = "testemail2@probation.gov.uk",
              name = Name(
                forename = "Test2",
                surname = "Test2",
              ),
              teams = emptyList(),
              code = "DE012F",
            ),
          ),
        )

        val approvalCases = service.getApprovalNeeded(aListOfPrisonCodes)

        verify(prisonApproverService, times(1)).getLicencesForApproval(aListOfPrisonCodes)
        verify(probationSearchApiClient, times(1)).searchForPeopleByNomsNumber(nomisIds)
        verify(deliusApiClient, times(1)).getStaffDetailsByUsername(comUsernames)

        assertThat(approvalCases).hasSize(2)
        assertThat(approvalCases).extracting<Long> { it.licenceId }.containsExactly(2, 1)
      }

      @Test
      fun `null release dates are surfaced above other release dates for the approval needed caseload`() {
        val nomisIds = listOf("A1234AA", "B1234BB", "C1234CC")
        val comUsernames = listOf("smills", "jdoe", "smills")

        whenever(prisonApproverService.getLicencesForApproval(aListOfPrisonCodes)).thenReturn(
          listOf(
            aLicenceSummaryApproverView.copy(
              licenceStartDate = LocalDate.of(2024, 6, 21),
            ),
            aLicenceSummaryApproverView.copy(
              licenceId = 2L,
              bookingId = 12345,
              nomisId = "B1234BB",
              crn = "Y12345",
              forename = "John",
              surname = "Doe",
              prisonCode = "ABC",
              prisonDescription = "ABC (HMP)",
              comUsername = "jdoe",
              licenceStatus = LicenceStatus.APPROVED,
              licenceStartDate = LocalDate.of(2024, 6, 20),
            ),
            aLicenceSummaryApproverView.copy(
              licenceId = 3L,
              bookingId = 67890,
              nomisId = "C1234CC",
              crn = "Z12345",
              forename = "Jane",
              surname = "Doe",
              prisonCode = "MDI",
              comUsername = "smills",
              licenceStatus = LicenceStatus.ACTIVE,
              licenceStartDate = null,
            ),
          ),
        )
        whenever(probationSearchApiClient.searchForPeopleByNomsNumber(nomisIds)).thenReturn(
          listOf(
            anOffenderDetailResult,
            anOffenderDetailResult.copy(
              otherIds = OtherIds(
                crn = "Y12345",
                croNumber = "DE01/234567F",
                pncNumber = null,
                nomsNumber = "B1234BB",
              ),
              offenderManagers = listOf(
                OffenderManager(
                  staffDetail = ProbationSearchStaffDetail(
                    code = "DE012F",
                    forenames = "Test2",
                    surname = "Test2",
                    unallocated = false,
                  ),
                  active = true,
                ),
              ),
            ),
            anOffenderDetailResult.copy(
              otherIds = OtherIds(
                crn = "Z12345",
                croNumber = "GH01/234567I",
                pncNumber = null,
                nomsNumber = "C1234CC",
              ),
              offenderManagers = listOf(
                OffenderManager(
                  staffDetail = ProbationSearchStaffDetail(
                    code = "GH012I",
                    forenames = "Test3",
                    surname = "Test3",
                    unallocated = false,
                  ),
                  active = true,
                ),
              ),
            ),
          ),
        )
        whenever(deliusApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(
          listOf(
            aUser,
            aUser.copy(
              id = 3000,
              username = "jdoe",
              email = "testemail2@probation.gov.uk",
              name = Name(
                forename = "Test2",
                surname = "Test2",
              ),
              teams = emptyList(),
              code = "DE012F",
            ),
            aUser,
          ),
        )

        val approvalCases = service.getApprovalNeeded(aListOfPrisonCodes)

        verify(prisonApproverService, times(1)).getLicencesForApproval(aListOfPrisonCodes)
        verify(probationSearchApiClient, times(1)).searchForPeopleByNomsNumber(nomisIds)
        verify(deliusApiClient, times(1)).getStaffDetailsByUsername(comUsernames)

        assertThat(approvalCases).hasSize(3)
        assertThat(approvalCases).extracting<Long> { it.licenceId }.containsExactly(3, 2, 1)
      }
    }

    @Test
    fun `CADM prison caseload is filtered out`() {
      val aListOfPrisonCodes = listOf("ABC", "DEF", "CADM")
      service.getApprovalNeeded(aListOfPrisonCodes)
      verify(prisonApproverService, times(1)).getLicencesForApproval(listOf("ABC", "DEF"))
    }

    @Test
    fun `Missing NOMIS ID on licence returns an empty caseload`() {
      whenever(prisonApproverService.getLicencesForApproval(aListOfPrisonCodes)).thenReturn(
        listOf(
          aLicenceSummaryApproverView.copy(
            nomisId = null,
          ),
        ),
      )

      val caseload = service.getApprovalNeeded(aListOfPrisonCodes)

      verify(prisonApproverService, times(1)).getLicencesForApproval(aListOfPrisonCodes)
      verify(probationSearchApiClient, times(1)).searchForPeopleByNomsNumber(emptyList())
      verify(deliusApiClient, times(1)).getStaffDetailsByUsername(emptyList())
      assertThat(caseload).isEmpty()
    }

    @Test
    fun `LSD is selected for releaseDate`() {
      val nomisId = "A1234AA"
      val comUsernames = listOf("smills")

      whenever(prisonApproverService.getLicencesForApproval(aListOfPrisonCodes)).thenReturn(
        listOf(
          aLicenceSummaryApproverView.copy(
            actualReleaseDate = LocalDate.of(2024, 6, 20),
            conditionalReleaseDate = LocalDate.of(2024, 6, 19),
            licenceStartDate = LocalDate.of(2024, 6, 18),
          ),
        ),
      )
      whenever(probationSearchApiClient.searchForPeopleByNomsNumber(listOf(nomisId))).thenReturn(
        listOf(anOffenderDetailResult),
      )
      whenever(deliusApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(listOf(aUser))

      val approvalCases = service.getApprovalNeeded(aListOfPrisonCodes)

      assertThat(approvalCases).hasSize(1)

      with(approvalCases.first()) {
        assertThat(releaseDate).isEqualTo((LocalDate.of(2024, 6, 18)))
      }

      verify(prisonApproverService, times(1)).getLicencesForApproval(aListOfPrisonCodes)
      verify(probationSearchApiClient, times(1)).searchForPeopleByNomsNumber(listOf(nomisId))
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
        val comUsernames = listOf("smills")

        val aLicenceSummaryApproverView = aLicenceSummaryApproverView.copy(
          licenceStatus = LicenceStatus.APPROVED,
          licenceStartDate = LocalDate.now().minusDays(14),
        )

        whenever(prisonApproverService.findRecentlyApprovedLicences(aListOfPrisonCodes)).thenReturn(
          listOf(
            aLicenceSummaryApproverView,
          ),
        )
        whenever(probationSearchApiClient.searchForPeopleByNomsNumber(listOf(nomisId))).thenReturn(
          listOf(anOffenderDetailResult),
        )
        whenever(deliusApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(listOf(aUser))

        val approvalCases = service.getRecentlyApproved(aListOfPrisonCodes)

        assertThat(approvalCases).hasSize(1)

        with(approvalCases.first()) {
          assertThat(licenceId).isEqualTo(1L)
          assertThat(name).isEqualTo("Bob Mortimer")
          assertThat(prisonerNumber).isEqualTo("A1234AA")
          assertThat(submittedByFullName).isEqualTo("X Y")
          assertThat(releaseDate).isEqualTo((LocalDate.now().minusDays(14)))
          assertThat(urgentApproval).isFalse()
          assertThat(approvedBy).isEqualTo("jim smith")
          assertThat(approvedOn).isEqualTo((LocalDateTime.of(2023, 9, 19, 16, 38, 42)))
          assertThat(isDueForEarlyRelease).isFalse()
          with(probationPractitioner!!) {
            assertThat(staffCode).isEqualTo("AB012C")
            assertThat(name).isEqualTo("Test Test")
          }
          assertThat(kind).isEqualTo(LicenceKind.CRD)
        }

        verify(prisonApproverService, times(1)).findRecentlyApprovedLicences(aListOfPrisonCodes)
        verify(probationSearchApiClient, times(1)).searchForPeopleByNomsNumber(listOf(nomisId))
        verify(deliusApiClient, times(1)).getStaffDetailsByUsername(comUsernames)
      }

      @Test
      fun `It builds the recently approved caseload successfully with multiple cases`() {
        val nomisIds = listOf("B1234BB", "C1234CC")
        val comUsernames = listOf("jdoe", "smills")

        whenever(prisonApproverService.findRecentlyApprovedLicences(aListOfPrisonCodes)).thenReturn(
          listOf(
            aLicenceSummaryApproverView.copy(
              licenceId = 2L,
              bookingId = 12345,
              nomisId = "B1234BB",
              crn = "Y12345",
              forename = "John",
              surname = "Doe",
              prisonCode = "ABC",
              prisonDescription = "ABC (HMP)",
              comUsername = "jdoe",
              licenceStatus = LicenceStatus.APPROVED,
              licenceStartDate = LocalDate.now().minusDays(14),
            ),
            aLicenceSummaryApproverView.copy(
              licenceId = 3L,
              bookingId = 67890,
              nomisId = "C1234CC",
              crn = "Z12345",
              forename = "Jane",
              surname = "Doe",
              prisonCode = "MDI",
              comUsername = "smills",
              licenceStatus = LicenceStatus.ACTIVE,
              licenceStartDate = LocalDate.now().minusDays(14),
            ),
          ),
        )
        whenever(probationSearchApiClient.searchForPeopleByNomsNumber(nomisIds)).thenReturn(
          listOf(
            anOffenderDetailResult.copy(
              otherIds = OtherIds(
                crn = "Y12345",
                croNumber = "DE01/234567F",
                pncNumber = null,
                nomsNumber = "B1234BB",
              ),
              offenderManagers = listOf(
                OffenderManager(
                  staffDetail = ProbationSearchStaffDetail(
                    code = "DE012F",
                    forenames = "Test2",
                    surname = "Test",
                    unallocated = false,
                  ),
                  active = true,
                ),
              ),
            ),
            anOffenderDetailResult.copy(
              otherIds = OtherIds(
                crn = "Z12345",
                croNumber = "GH01/234567I",
                pncNumber = null,
                nomsNumber = "C1234CC",
              ),
              offenderManagers = listOf(
                OffenderManager(
                  staffDetail = ProbationSearchStaffDetail(
                    code = "GH012I",
                    forenames = "Test3",
                    surname = "Test3",
                    unallocated = false,
                  ),
                  active = true,
                ),
              ),
            ),
          ),
        )

        whenever(deliusApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(
          listOf(
            aUser.copy(
              id = 3000,
              username = "jdoe",
              email = "testemail2@probation.gov.uk",
              name = Name(
                forename = "Test2",
                surname = "Test2",
              ),
              teams = emptyList(),
              code = "DE012F",
            ),
            aUser,
          ),
        )

        val caseload = service.getRecentlyApproved(aListOfPrisonCodes)

        assertThat(caseload).hasSize(2)

        with(caseload.first()) {
          assertThat(licenceId).isEqualTo(2L)
          assertThat(name).isEqualTo("John Doe")
          assertThat(prisonerNumber).isEqualTo("B1234BB")
          assertThat(submittedByFullName).isEqualTo("X Y")
          assertThat(releaseDate).isEqualTo(LocalDate.now().minusDays(14))
          assertThat(urgentApproval).isFalse()
          assertThat(approvedBy).isEqualTo("jim smith")
          assertThat(approvedOn).isEqualTo((LocalDateTime.of(2023, 9, 19, 16, 38, 42)))
          assertThat(isDueForEarlyRelease).isFalse()
          with(probationPractitioner!!) {
            assertThat(staffCode).isEqualTo("DE012F")
            assertThat(name).isEqualTo("Test2 Test2")
          }
          assertThat(kind).isEqualTo(LicenceKind.CRD)
        }

        with(caseload.last()) {
          assertThat(licenceId).isEqualTo(3L)
          assertThat(name).isEqualTo("Jane Doe")
          assertThat(prisonerNumber).isEqualTo("C1234CC")
          assertThat(submittedByFullName).isEqualTo("X Y")
          assertThat(releaseDate).isEqualTo(LocalDate.now().minusDays(14))
          assertThat(urgentApproval).isFalse()
          assertThat(approvedBy).isEqualTo("jim smith")
          assertThat(approvedOn).isEqualTo((LocalDateTime.of(2023, 9, 19, 16, 38, 42)))
          assertThat(isDueForEarlyRelease).isFalse()
          with(probationPractitioner!!) {
            assertThat(staffCode).isEqualTo("AB012C")
            assertThat(name).isEqualTo("Test Test")
          }
          assertThat(kind).isEqualTo(LicenceKind.CRD)
        }

        verify(prisonApproverService, times(1)).findRecentlyApprovedLicences(aListOfPrisonCodes)
        verify(probationSearchApiClient, times(1)).searchForPeopleByNomsNumber(nomisIds)
        verify(deliusApiClient, times(1)).getStaffDetailsByUsername(comUsernames)
      }

      @Test
      fun `the recently approved caseload is sorted correctly`() {
        val nomisIds = listOf("B1234BB", "C1234CC")
        val comUsernames = listOf("jdoe", "smills")

        whenever(prisonApproverService.findRecentlyApprovedLicences(aListOfPrisonCodes)).thenReturn(
          listOf(
            aLicenceSummaryApproverView.copy(
              licenceId = 2L,
              bookingId = 12345,
              nomisId = "B1234BB",
              crn = "Y12345",
              forename = "John",
              surname = "Doe",
              prisonCode = "ABC",
              prisonDescription = "ABC (HMP)",
              comUsername = "jdoe",
              licenceStatus = LicenceStatus.APPROVED,
              licenceStartDate = LocalDate.now().minusDays(13),
            ),
            aLicenceSummaryApproverView.copy(
              licenceId = 3L,
              bookingId = 67890,
              nomisId = "C1234CC",
              crn = "Z12345",
              forename = "Jane",
              surname = "Doe",
              prisonCode = "MDI",
              comUsername = "smills",
              licenceStatus = LicenceStatus.ACTIVE,
              licenceStartDate = LocalDate.now().minusDays(14),
            ),
          ),
        )
        whenever(probationSearchApiClient.searchForPeopleByNomsNumber(nomisIds)).thenReturn(
          listOf(
            anOffenderDetailResult.copy(
              otherIds = OtherIds(
                crn = "Y12345",
                croNumber = "DE01/234567F",
                pncNumber = null,
                nomsNumber = "B1234BB",
              ),
              offenderManagers = listOf(
                OffenderManager(
                  staffDetail = ProbationSearchStaffDetail(
                    code = "DE012F",
                    forenames = "Test2",
                    surname = "Test",
                    unallocated = false,
                  ),
                  active = true,
                ),
              ),
            ),
            anOffenderDetailResult.copy(
              otherIds = OtherIds(
                crn = "Z12345",
                croNumber = "GH01/234567I",
                pncNumber = null,
                nomsNumber = "C1234CC",
              ),
              offenderManagers = listOf(
                OffenderManager(
                  staffDetail = ProbationSearchStaffDetail(
                    code = "GH012I",
                    forenames = "Test3",
                    surname = "Test3",
                    unallocated = false,
                  ),
                  active = true,
                ),
              ),
            ),
          ),
        )
        whenever(deliusApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(
          listOf(
            aUser.copy(
              id = 3000,
              username = "jdoe",
              email = "testemail2@probation.gov.uk",
              name = Name(
                forename = "Test2",
                surname = "Test2",
              ),
              teams = emptyList(),
              code = "DE012F",
            ),
            aUser,
          ),
        )

        val caseload = service.getRecentlyApproved(aListOfPrisonCodes)

        verify(prisonApproverService, times(1)).findRecentlyApprovedLicences(aListOfPrisonCodes)
        verify(probationSearchApiClient, times(1)).searchForPeopleByNomsNumber(nomisIds)
        verify(deliusApiClient, times(1)).getStaffDetailsByUsername(comUsernames)

        assertThat(caseload).hasSize(2)
        assertThat(caseload).extracting<Long> { it.licenceId }.containsExactly(3, 2)
      }

      @Test
      fun `null release dates are surfaced above other release dates for the recently approved caseload`() {
        val nomisIds = listOf("A1234AA", "B1234BB", "C1234CC")
        val comUsernames = listOf("smills", "jdoe", "smills")

        whenever(prisonApproverService.findRecentlyApprovedLicences(aListOfPrisonCodes)).thenReturn(
          listOf(
            aLicenceSummaryApproverView.copy(
              licenceStartDate = LocalDate.of(2024, 6, 21),
            ),
            aLicenceSummaryApproverView.copy(
              licenceId = 2L,
              bookingId = 12345,
              nomisId = "B1234BB",
              crn = "Y12345",
              forename = "John",
              surname = "Doe",
              prisonCode = "ABC",
              prisonDescription = "ABC (HMP)",
              comUsername = "jdoe",
              licenceStatus = LicenceStatus.APPROVED,
              licenceStartDate = LocalDate.of(2024, 6, 20),
            ),
            aLicenceSummaryApproverView.copy(
              licenceId = 3L,
              bookingId = 67890,
              nomisId = "C1234CC",
              crn = "Z12345",
              forename = "Jane",
              surname = "Doe",
              prisonCode = "MDI",
              comUsername = "smills",
              licenceStatus = LicenceStatus.ACTIVE,
              licenceStartDate = null,
            ),
          ),
        )
        whenever(probationSearchApiClient.searchForPeopleByNomsNumber(nomisIds)).thenReturn(
          listOf(
            anOffenderDetailResult,
            anOffenderDetailResult.copy(
              otherIds = OtherIds(
                crn = "Y12345",
                croNumber = "DE01/234567F",
                pncNumber = null,
                nomsNumber = "B1234BB",
              ),
              offenderManagers = listOf(
                OffenderManager(
                  staffDetail = ProbationSearchStaffDetail(
                    code = "DE012F",
                    forenames = "Test2",
                    surname = "Test",
                    unallocated = false,
                  ),
                  active = true,
                ),
              ),
            ),
            anOffenderDetailResult.copy(
              otherIds = OtherIds(
                crn = "Z12345",
                croNumber = "GH01/234567I",
                pncNumber = null,
                nomsNumber = "C1234CC",
              ),
              offenderManagers = listOf(
                OffenderManager(
                  staffDetail = ProbationSearchStaffDetail(
                    code = "GH012I",
                    forenames = "Test3",
                    surname = "Test3",
                    unallocated = false,
                  ),
                  active = true,
                ),
              ),
            ),
          ),
        )
        whenever(deliusApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(
          listOf(
            aUser,
            aUser.copy(
              id = 3000,
              username = "jdoe",
              email = "testemail2@probation.gov.uk",
              name = Name(
                forename = "Test2",
                surname = "Test2",
              ),
              teams = emptyList(),
              code = "DE012F",
            ),
            aUser,
          ),
        )

        val approvalCases = service.getRecentlyApproved(aListOfPrisonCodes)

        verify(prisonApproverService, times(1)).findRecentlyApprovedLicences(aListOfPrisonCodes)
        verify(probationSearchApiClient, times(1)).searchForPeopleByNomsNumber(nomisIds)
        verify(deliusApiClient, times(1)).getStaffDetailsByUsername(comUsernames)

        assertThat(approvalCases).hasSize(3)
        assertThat(approvalCases).extracting<Long> { it.licenceId }.containsExactly(3, 2, 1)
      }
    }

    @Test
    fun `CADM prison code is filtered out`() {
      val aListOfPrisonCodes = listOf("ABC", "DEF", "CADM")
      service.getRecentlyApproved(aListOfPrisonCodes)
      verify(prisonApproverService, times(1)).findRecentlyApprovedLicences(listOf("ABC", "DEF"))
    }

    @Test
    fun `a null release date is returned when LSD is not set`() {
      val nomisId = "A1234AA"
      val comUsernames = listOf("smills")

      whenever(prisonApproverService.findRecentlyApprovedLicences(aListOfPrisonCodes)).thenReturn(
        listOf(
          aLicenceSummaryApproverView.copy(
            licenceStartDate = null,
          ),
        ),
      )
      whenever(probationSearchApiClient.searchForPeopleByNomsNumber(listOf(nomisId))).thenReturn(
        listOf(anOffenderDetailResult),
      )
      whenever(deliusApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(listOf(aUser))

      val approvalCases = service.getRecentlyApproved(aListOfPrisonCodes)

      assertThat(approvalCases).hasSize(1)

      with(approvalCases.first()) {
        assertThat(releaseDate).isNull()
      }

      verify(prisonApproverService, times(1)).findRecentlyApprovedLicences(aListOfPrisonCodes)
      verify(probationSearchApiClient, times(1)).searchForPeopleByNomsNumber(listOf(nomisId))
      verify(deliusApiClient, times(1)).getStaffDetailsByUsername(comUsernames)
    }

    @Test
    fun `It derives if urgent approval is needed`() {
      val nomisId = "A1234AA"
      val comUsernames = listOf("smills")

      whenever(prisonApproverService.findRecentlyApprovedLicences(aListOfPrisonCodes)).thenReturn(
        listOf(
          aLicenceSummaryApproverView.copy(
            isDueToBeReleasedInTheNextTwoWorkingDays = true,
          ),
        ),
      )
      whenever(probationSearchApiClient.searchForPeopleByNomsNumber(listOf(nomisId))).thenReturn(
        listOf(anOffenderDetailResult),
      )
      whenever(deliusApiClient.getStaffDetailsByUsername(comUsernames)).thenReturn(listOf(aUser))

      val approvalCases = service.getRecentlyApproved(aListOfPrisonCodes)

      assertThat(approvalCases).hasSize(1)

      with(approvalCases.first()) {
        assertThat(urgentApproval).isTrue()
      }

      verify(prisonApproverService, times(1)).findRecentlyApprovedLicences(aListOfPrisonCodes)
      verify(probationSearchApiClient, times(1)).searchForPeopleByNomsNumber(listOf(nomisId))
      verify(deliusApiClient, times(1)).getStaffDetailsByUsername(comUsernames)
    }
  }

  @Nested
  inner class `Retrieve the correct probation practitioner` {
    @Test
    fun `The correct com is retrieved`() {
      val coms = listOf(
        aUser,
        aUser.copy(
          id = 1000,
          username = "jdoe",
          email = "testemail2@probation.gov.uk",
          name = Name(
            forename = "Test2",
            surname = "Test2",
          ),
          teams = emptyList(),
          code = "CD012E",
        ),
      )

      val com = service.findProbationPractitioner(anOffenderDetailResult, aLicenceSummaryApproverView.comUsername, coms)

      assertThat(com?.staffCode).isEqualTo("AB012C")
      assertThat(com?.name).isEqualTo("Test Test")
    }

    @Test
    fun `If the com is not found in the list of coms, the delius record details are used instead`() {
      val coms = listOf(
        aUser.copy(
          username = "test1",
          code = "test1",
          name = Name(
            forename = "Test1",
            surname = "Test1",
          ),
        ),
        aUser.copy(
          username = "test2",
          code = "test2",
          name = Name(
            forename = "Test2",
            surname = "Test2",
          ),
        ),
      )

      val com = service.findProbationPractitioner(anOffenderDetailResult, aLicenceSummaryApproverView.comUsername, coms)

      assertThat(com?.staffCode).isEqualTo("AB012C")
      assertThat(com?.name).isEqualTo("Test Test")
    }

    @Test
    fun `return null if com is unallocated`() {
      val coms = listOf(
        aUser.copy(
          username = "test1",
          code = "test1",
          name = Name(
            forename = "Test1",
            surname = "Test1",
          ),
        ),
        aUser.copy(
          username = "test2",
          code = "test2",
          name = Name(
            forename = "Test2",
            surname = "Test2",
          ),
        ),
      )

      val anOffenderDetailResult = anOffenderDetailResult.copy(
        offenderManagers = listOf(
          OffenderManager(
            staffDetail = ProbationSearchStaffDetail(
              code = "AB012C",
              forenames = "Test",
              surname = "Test",
              unallocated = true,
            ),
            active = true,
          ),
        ),
      )

      val com = service.findProbationPractitioner(anOffenderDetailResult, aLicenceSummaryApproverView.comUsername, coms)

      assertThat(com).isNull()
    }
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
          staffDetail = ProbationSearchStaffDetail(
            code = "AB012C",
            forenames = "Test",
            surname = "Test",
            unallocated = false,
          ),
          active = true,
        ),
      ),
    )

    val aUser = User(
      id = 2000,
      username = "smills",
      email = "testemail@probation.gov.uk",
      name = Name(
        forename = "Test",
        surname = "Test",
      ),
      teams = emptyList(),
      code = "AB012C",
    )
  }
}
