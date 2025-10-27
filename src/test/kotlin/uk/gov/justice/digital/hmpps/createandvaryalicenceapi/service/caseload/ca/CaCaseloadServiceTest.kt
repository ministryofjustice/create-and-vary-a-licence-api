package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonCaseAdminSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.PrisonUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecordService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService.HdcStatuses
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison.CaPrisonCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison.ExistingCasesCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison.NotStartedCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.probation.CaProbationCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Detail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.TeamDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.StaffNameResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util.ReleaseDateLabelFactory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.workingDays.WorkingDaysService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class CaCaseloadServiceTest {
  private val caseloadService = mock<CaseloadService>()
  private val licenceService = mock<LicenceService>()
  private val hdcService = mock<HdcService>()
  private val deliusApiClient = mock<DeliusApiClient>()
  private val eligibilityService = mock<EligibilityService>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val workingDaysService = mock<WorkingDaysService>()
  private val releaseDateLabelFactory = ReleaseDateLabelFactory(workingDaysService)
  private val cvlRecordService = mock<CvlRecordService>()

  private val service = CaCaseloadService(
    prisonCaseloadService = CaPrisonCaseloadService(
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
    ),
    probationCaseloadService = CaProbationCaseloadService(
      licenceService,
      deliusApiClient,
      releaseDateLabelFactory,
    ),

  )

  private val prisonStatuses = listOf(
    LicenceStatus.APPROVED,
    LicenceStatus.SUBMITTED,
    LicenceStatus.IN_PROGRESS,
    LicenceStatus.TIMED_OUT,
    LicenceStatus.ACTIVE,
  )

  val probationStatuses = listOf(
    LicenceStatus.ACTIVE,
    LicenceStatus.VARIATION_APPROVED,
    LicenceStatus.VARIATION_IN_PROGRESS,
    LicenceStatus.VARIATION_SUBMITTED,
  )

  private val prisonLicenceQueryObject = LicenceQueryObject(
    statusCodes = prisonStatuses,
    prisonCodes = listOf("BAI"),
    sortBy = "licenceStartDate",
  )

  val probationLicenceQueryObject = LicenceQueryObject(
    statusCodes = probationStatuses,
    prisonCodes = listOf("BAI"),
  )

  @BeforeEach
  fun reset() {
    reset(
      caseloadService,
      licenceService,
      hdcService,
      eligibilityService,
      deliusApiClient,
      prisonerSearchApiClient,
      releaseDateService,
      cvlRecordService,
    )
    whenever(licenceService.findLicencesMatchingCriteria(prisonLicenceQueryObject)).thenReturn(
      listOf(
        aLicenceSummary,
        aLicenceSummary.copy(
          licenceId = 2,
          licenceStatus = LicenceStatus.IN_PROGRESS,
          nomisId = "A1234AB",
          forename = "Person",
          surname = "Two",
          comUsername = "tcom",
        ),
        aLicenceSummary.copy(
          licenceId = 3,
          licenceStatus = LicenceStatus.IN_PROGRESS,
          nomisId = "A1234AC",
          forename = "Person",
          surname = "Three",
          comUsername = "atcom",
          licenceStartDate = null,
          conditionalReleaseDate = null,
        ),
      ),
    )
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        TestData.prisonerSearchResult(),
        TestData.prisonerSearchResult().copy(
          prisonerNumber = "A1234AB",
          firstName = "Person",
          lastName = "Two",
          legalStatus = "SENTENCED",
          dateOfBirth = LocalDate.of(1985, 12, 28),
          mostSeriousOffence = "Robbery",
        ),
        TestData.prisonerSearchResult().copy(
          prisonerNumber = "A1234AC",
          firstName = "Person",
          lastName = "Three",
          legalStatus = "SENTENCED",
          dateOfBirth = LocalDate.of(1985, 12, 28),
          mostSeriousOffence = "Robbery",
          releaseDate = null,
        ),
      ),
    )

    whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
      PageImpl(
        listOf(
          aPrisonerSearchPrisoner,
          aPrisonerSearchPrisoner.copy(
            prisonerNumber = "A1234AB",
            firstName = "Person",
            lastName = "Two",
            legalStatus = "SENTENCED",
            dateOfBirth = LocalDate.of(1985, 12, 28),
            mostSeriousOffence = "Robbery",
          ),
          aPrisonerSearchPrisoner.copy(
            prisonerNumber = "A1234AC",
            firstName = "Person",
            lastName = "Three",
            legalStatus = "SENTENCED",
            dateOfBirth = LocalDate.of(1985, 12, 28),
            mostSeriousOffence = "Robbery",
            releaseDate = null,
          ),
        ),
      ),
    )
    whenever(deliusApiClient.getStaffDetailsByUsername(any())).thenReturn(listOf(comUser, atcomUser))

    whenever(deliusApiClient.getProbationCases(any(), anyOrNull())).thenReturn(listOf(probationCase))
    whenever(deliusApiClient.getOffenderManagers(any(), anyOrNull())).thenReturn(listOf(aCommunityManager))
  }

  @Nested
  inner class `Prison tab caseload` {
    @Nested
    inner class `in the hard stop period` {
      @Test
      fun `Sets NOT_STARTED licences to TIMED_OUT when in the hard stop period`() {
        whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
          listOf(
            aPrisonerSearchPrisoner,
          ),
        )
        whenever(licenceService.findLicencesMatchingCriteria(prisonLicenceQueryObject)).thenReturn(
          emptyList(),
        )
        whenever(
          cvlRecordService.getCvlRecords(
            any(),
            any(),
          ),
        ).thenReturn(
          listOf(
            TestData.aCvlRecord(
              nomsId = aLicenceSummary.nomisId,
              kind = LicenceKind.CRD,
              licenceStartDate = twoDaysFromNow,
              isInHardStopPeriod = true,
            ),
          ),
        )
        whenever(
          prisonerSearchApiClient.searchPrisonersByReleaseDate(
            any(),
            any(),
            any(),
            anyOrNull(),
          ),
        ).thenReturn(
          PageImpl(
            listOf(
              aPrisonerSearchPrisoner.copy(
                bookingId = "1",
                prisonerNumber = aLicenceSummary.nomisId,
                confirmedReleaseDate = twoMonthsFromNow,
                conditionalReleaseDate = twoDaysFromNow,
              ),
            ),
          ),
        )

        whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

        val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")

        assertThat(prisonOmuCaseload).hasSize(1)

        with(prisonOmuCaseload.first()) {
          assertThat(name).isEqualTo("Person Four")
          assertThat(licenceStatus).isEqualTo(LicenceStatus.TIMED_OUT)
          assertThat(isInHardStopPeriod).isTrue()
        }

        verify(licenceService, times(1)).findLicencesMatchingCriteria(prisonLicenceQueryObject)
        verify(caseloadService, times(0)).getPrisonersByNumber(listOf(aLicenceSummary.nomisId))
        verify(prisonerSearchApiClient, times(1)).searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull())
      }
    }

    @Nested
    inner class `apply Search` {
      @Test
      fun `should successfully search by name`() {
        assertThat(service.getPrisonOmuCaseload(setOf("BAI"), "Two")).isEqualTo(
          listOf(
            TestData.caCase().copy(
              licenceId = 2,
              prisonerNumber = "A1234AB",
              name = "Person Two",
              nomisLegalStatus = "SENTENCED",
              lastWorkedOnBy = "X Y",
              probationPractitioner = ProbationPractitioner(staffUsername = "tcom"),
              prisonCode = "BAI",
              prisonDescription = "Moorland (HMP)",
            ),
          ),
        )
      }

      @Test
      fun `should successfully search by prison number`() {
        assertThat(service.getPrisonOmuCaseload(setOf("BAI"), "A1234AA")).isEqualTo(
          listOf(
            TestData.caCase().copy(
              name = "Person One",
              lastWorkedOnBy = "X Y",
              probationPractitioner = ProbationPractitioner(staffCode = "AB00001", name = "com user"),
              prisonCode = "BAI",
              prisonDescription = "Moorland (HMP)",
            ),
          ),
        )
      }

      @Test
      fun `should successfully search by probation practitioner`() {
        whenever(licenceService.findLicencesMatchingCriteria(prisonLicenceQueryObject)).thenReturn(
          listOf(
            aLicenceSummary,
            aLicenceSummary.copy(
              licenceId = 2,
              licenceStatus = LicenceStatus.IN_PROGRESS,
              nomisId = "A1234AB",
              forename = "Person",
              surname = "Two",
            ),
          ),
        )
        assertThat(service.getPrisonOmuCaseload(setOf("BAI"), "com")).isEqualTo(
          listOf(
            TestData.caCase().copy(
              name = "Person One",
              lastWorkedOnBy = "X Y",
              probationPractitioner = ProbationPractitioner(
                staffCode = "AB00001",
                name = "com user",
                staffIdentifier = null,
                staffUsername = null,
              ),
              prisonCode = "BAI",
              prisonDescription = "Moorland (HMP)",
            ),
            TestData.caCase().copy(
              licenceId = 2,
              prisonerNumber = "A1234AB",
              name = "Person Two",
              nomisLegalStatus = "SENTENCED",
              lastWorkedOnBy = "X Y",
              probationPractitioner = ProbationPractitioner(
                staffCode = "AB00001",
                name = "com user",
                staffIdentifier = null,
                staffUsername = null,
              ),
              prisonCode = "BAI",
              prisonDescription = "Moorland (HMP)",
            ),
          ),
        )
      }
    }

    @Test
    fun `should filter out cases with an existing ACTIVE licence`() {
      whenever(licenceService.findLicencesMatchingCriteria(prisonLicenceQueryObject)).thenReturn(
        listOf(
          aLicenceSummary.copy(
            forename = "Person",
            surname = "Three",
            nomisId = "AB1234E",
            licenceId = 2,
            licenceType = LicenceType.PSS,
            licenceStatus = LicenceStatus.ACTIVE,
            isInHardStopPeriod = false,
            isDueToBeReleasedInTheNextTwoWorkingDays = true,
            conditionalReleaseDate = twoMonthsFromNow,
            actualReleaseDate = twoDaysFromNow,
          ),
        ),
      )

      whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
        listOf(
          TestData.prisonerSearchResult().copy(
            firstName = "Person",
            lastName = "Three",
            prisonerNumber = "AB1234E",
            conditionalReleaseDate = twoMonthsFromNow,
            confirmedReleaseDate = twoDaysFromNow,
            status = "ACTIVE IN",
            legalStatus = "SENTENCED",
            dateOfBirth = LocalDate.of(1985, 12, 28),
            mostSeriousOffence = "Robbery",
          ),
        ),
      )

      whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
        PageImpl(
          listOf(
            aPrisonerSearchPrisoner.copy(
              firstName = "Person",
              lastName = "Three",
              prisonerNumber = "AB1234E",
              conditionalReleaseDate = twoMonthsFromNow,
              confirmedReleaseDate = twoDaysFromNow,
              status = "ACTIVE IN",
              legalStatus = "SENTENCED",
              dateOfBirth = LocalDate.of(1985, 12, 28),
              mostSeriousOffence = "Robbery",
            ),
          ),
        ),
      )
      val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")
      assertThat(prisonOmuCaseload).isEqualTo(emptyList<CaCase>())
    }

    @Test
    fun `should query for cases being released within 4 weeks`() {
      service.getPrisonOmuCaseload(setOf("BAI"), "Five")
      verify(prisonerSearchApiClient, times(1)).searchPrisonersByReleaseDate(
        LocalDate.now(clock),
        LocalDate.now(clock).plusWeeks(4),
        setOf("BAI"),
        0,
      )
    }

    @Test
    fun `should filter out duplicate cases, prioritising existing licences`() {
      whenever(licenceService.findLicencesMatchingCriteria(prisonLicenceQueryObject)).thenReturn(
        listOf(
          aLicenceSummary.copy(
            forename = "Person",
            surname = "Three",
            nomisId = "AB1234E",
            licenceId = 2,
            licenceType = LicenceType.PSS,
            licenceStatus = LicenceStatus.IN_PROGRESS,
            isInHardStopPeriod = false,
            isDueToBeReleasedInTheNextTwoWorkingDays = true,
            licenceStartDate = twoDaysFromNow,
            conditionalReleaseDate = twoMonthsFromNow,
            actualReleaseDate = twoDaysFromNow,
          ),
        ),
      )

      whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
        listOf(
          TestData.prisonerSearchResult().copy(
            firstName = "Person",
            lastName = "Three",
            prisonerNumber = "AB1234E",
            conditionalReleaseDate = twoMonthsFromNow,
            confirmedReleaseDate = twoDaysFromNow,
            status = "ACTIVE IN",
            legalStatus = "SENTENCED",
            dateOfBirth = LocalDate.of(1985, 12, 28),
            mostSeriousOffence = "Robbery",
          ),
        ),
      )

      whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
        PageImpl(
          listOf(
            aPrisonerSearchPrisoner.copy(
              firstName = "Person",
              lastName = "Three",
              prisonerNumber = "AB1234E",
              conditionalReleaseDate = twoMonthsFromNow,
              confirmedReleaseDate = twoDaysFromNow,
              status = "ACTIVE IN",
              legalStatus = "SENTENCED",
              dateOfBirth = LocalDate.of(1985, 12, 28),
              mostSeriousOffence = "Robbery",
            ),
          ),
        ),
      )
      val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")
      assertThat(prisonOmuCaseload).isEqualTo(
        listOf(
          TestData.caCase().copy(
            licenceId = 2,
            name = "Person Three",
            prisonerNumber = "AB1234E",
            probationPractitioner = ProbationPractitioner(
              staffCode = "AB00001",
              name = "com user",
              staffIdentifier = null,
              staffUsername = null,
            ),
            releaseDate = twoDaysFromNow,
            releaseDateLabel = "Confirmed release date",
            tabType = CaViewCasesTab.RELEASES_IN_NEXT_TWO_WORKING_DAYS,
            nomisLegalStatus = "SENTENCED",
            lastWorkedOnBy = "X Y",

            isInHardStopPeriod = false,
            prisonCode = "BAI",
            prisonDescription = "Moorland (HMP)",
          ),
        ),
      )
    }

    @Test
    fun `should return sorted results in ascending order`() {
      whenever(licenceService.findLicencesMatchingCriteria(prisonLicenceQueryObject)).thenReturn(
        listOf(
          aLicenceSummary.copy(
            conditionalReleaseDate = tenDaysFromNow,
            actualReleaseDate = twoMonthsFromNow,
            licenceStartDate = tenDaysFromNow,
          ),
          aLicenceSummary.copy(
            licenceId = 2,
            licenceStatus = LicenceStatus.IN_PROGRESS,
            nomisId = "A1234AB",
            forename = "Person",
            surname = "Two",
            comUsername = "tcom",
            conditionalReleaseDate = tenDaysFromNow,
            actualReleaseDate = twoDaysFromNow,
            licenceStartDate = twoDaysFromNow,
          ),
        ),
      )
      whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
        listOf(
          TestData.prisonerSearchResult(),
          TestData.prisonerSearchResult().copy(
            prisonerNumber = "A1234AB",
            firstName = "Person",
            lastName = "Two",
            legalStatus = "SENTENCED",
            dateOfBirth = LocalDate.of(1985, 12, 28),
            mostSeriousOffence = "Robbery",
          ),
        ),
      )
      val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")
      assertThat(prisonOmuCaseload).isEqualTo(
        listOf(
          TestData.caCase().copy(
            licenceId = 2,
            name = "Person Two",
            prisonerNumber = "A1234AB",
            releaseDate = twoDaysFromNow,
            releaseDateLabel = "Confirmed release date",
            probationPractitioner = ProbationPractitioner(
              staffCode = null,
              name = null,
              staffIdentifier = null,
              staffUsername = "tcom",
            ),
            lastWorkedOnBy = "X Y",
            prisonCode = "BAI",
            prisonDescription = "Moorland (HMP)",
          ),
          TestData.caCase().copy(
            name = "Person One",
            releaseDate = tenDaysFromNow,
            releaseDateLabel = "CRD",
            probationPractitioner = ProbationPractitioner(
              staffCode = "AB00001",
              name = "com user",
              staffIdentifier = null,
              staffUsername = null,
            ),
            lastWorkedOnBy = "X Y",
            prisonCode = "BAI",
            prisonDescription = "Moorland (HMP)",
          ),
        ),
      )
    }

    @Test
    fun `should have correct releaseDateLabel when actualReleaseDate is the same as licenceStartDate`() {
      // Given
      val licenceSummary = aLicenceSummary.copy(
        actualReleaseDate = twoDaysFromNow,
        licenceStartDate = twoDaysFromNow,
      )

      whenever(licenceService.findLicencesMatchingCriteria(prisonLicenceQueryObject)).thenReturn(
        listOf(
          licenceSummary,
        ),
      )
      whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(listOf(TestData.prisonerSearchResult()))

      // When
      val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")

      // Then
      assertThat(prisonOmuCaseload).hasSize(1)
      assertThat(prisonOmuCaseload[0].releaseDateLabel).isEqualTo("Confirmed release date")
    }

    @Test
    fun `should have correct releaseDateLabel when postRecallReleaseDate is the same as licenceStartDate`() {
      // Given
      val licenceSummary = aLicenceSummary.copy(
        licenceStartDate = tenDaysFromNow,
        postRecallReleaseDate = tenDaysFromNow,
      )
      whenever(workingDaysService.getLastWorkingDay(licenceSummary.postRecallReleaseDate)).thenReturn(
        licenceSummary.postRecallReleaseDate,
      )
      whenever(licenceService.findLicencesMatchingCriteria(prisonLicenceQueryObject)).thenReturn(
        listOf(
          licenceSummary,
        ),
      )
      whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(listOf(TestData.prisonerSearchResult()))

      // When
      val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")

      // Then
      assertThat(prisonOmuCaseload).hasSize(1)
      assertThat(prisonOmuCaseload[0].releaseDateLabel).isEqualTo("Post-recall release date (PRRD)")
    }

    @Nested
    inner class `filtering rules` {
      @Test
      fun `should filter ineligible cases`() {
        whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull())).thenReturn(false)
        whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
          listOf(
            aPrisonerSearchPrisoner,
          ),
        )
        whenever(licenceService.findLicencesMatchingCriteria(prisonLicenceQueryObject)).thenReturn(
          emptyList(),
        )
        whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(
          listOf(
            TestData.aCvlRecord(
              nomsId = "A1234AA",
              kind = LicenceKind.CRD,
              licenceStartDate = twoDaysFromNow,
            ),
            TestData.aCvlRecord(nomsId = "A1234AB", kind = null, licenceStartDate = null)
              .copy(isEligible = false),
          ),
        )
        whenever(
          prisonerSearchApiClient.searchPrisonersByReleaseDate(
            any(),
            any(),
            any(),
            anyOrNull(),
          ),
        ).thenReturn(
          PageImpl(
            listOf(
              aPrisonerSearchPrisoner.copy(
                bookingId = "1",
                prisonerNumber = "A1234AA",
                conditionalReleaseDate = fiveDaysFromNow,
              ),
              aPrisonerSearchPrisoner.copy(
                bookingId = "2",
                prisonerNumber = "A1234AB",
                paroleEligibilityDate = fiveDaysFromNow,
              ),
            ),
          ),
        )

        whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

        val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")
        assertThat(prisonOmuCaseload).hasSize(1)
        assertThat(prisonOmuCaseload[0].prisonerNumber).isEqualTo("A1234AA")
      }

      @Test
      fun `Should filter out cases with a legal status of DEAD`() {
        whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
          listOf(
            aPrisonerSearchPrisoner.copy(
              prisonerNumber = "A1234AA",
              legalStatus = "DEAD",
            ),
          ),
        )

        whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(
          listOf(
            TestData.aCvlRecord(
              nomsId = "A1234AA",
              kind = LicenceKind.CRD,
              licenceStartDate = twoDaysFromNow,
            ),
          ),
        )

        whenever(licenceService.findLicencesMatchingCriteria(prisonLicenceQueryObject)).thenReturn(
          listOf(
            aLicenceSummary,
          ),
        )

        val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEqualTo(emptyList<CaCase>())
      }

      @Test
      fun `should filter out cases with no CRD`() {
        whenever(
          prisonerSearchApiClient.searchPrisonersByReleaseDate(
            any(),
            any(),
            any(),
            anyOrNull(),
          ),
        ).thenReturn(
          PageImpl(
            listOf(
              aPrisonerSearchPrisoner.copy(
                conditionalReleaseDate = null,
              ),
            ),
          ),
        )

        whenever(licenceService.findLicencesMatchingCriteria(prisonLicenceQueryObject)).thenReturn(emptyList())
        whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(
          listOf(
            TestData.aCvlRecord(
              nomsId = aPrisonerSearchPrisoner.prisonerNumber,
              kind = null,
              licenceStartDate = null,
            ),
          ),
        )

        val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEqualTo(emptyList<CaCase>())
      }

      @Test
      fun `should filter out cases with an approved HDC licence and HDCED`() {
        whenever(licenceService.findLicencesMatchingCriteria(prisonLicenceQueryObject)).thenReturn(
          emptyList(),
        )

        val prisoner = aPrisonerSearchPrisoner.copy(
          prisonerNumber = "A1234AC",
          actualParoleDate = null,
          conditionalReleaseDate = twoMonthsFromNow,
          confirmedReleaseDate = twoDaysFromNow,
          status = "ACTIVE IN",
          legalStatus = "SENTENCED",
          homeDetentionCurfewEligibilityDate = twoDaysFromNow,
          bookingId = "1234",
        )
        whenever(
          prisonerSearchApiClient.searchPrisonersByReleaseDate(
            any(),
            any(),
            any(),
            anyOrNull(),
          ),
        ).thenReturn(
          PageImpl(
            listOf(
              prisoner,
            ),
          ),
        )

        whenever(hdcService.getHdcStatus(listOf(prisoner))).thenReturn(HdcStatuses(setOf(prisoner.bookingId!!.toLong())))

        val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEqualTo(emptyList<CaCase>())
      }

      @Test
      fun `should not filter out cases with an unapproved HDC licence`() {
        whenever(licenceService.findLicencesMatchingCriteria(prisonLicenceQueryObject)).thenReturn(
          emptyList(),
        )
        whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(
          listOf(
            TestData.aCvlRecord(
              kind = LicenceKind.CRD,
              licenceStartDate = twoDaysFromNow,
            ),
          ),
        )
        val prisoner = aPrisonerSearchPrisoner.copy(
          prisonerNumber = "A1234AA",
          conditionalReleaseDate = fiveDaysFromNow,
          confirmedReleaseDate = twoDaysFromNow,
          status = "ACTIVE IN",
          legalStatus = "SENTENCED",
          homeDetentionCurfewEligibilityDate = twoDaysFromNow,
          bookingId = "1234",
        )
        whenever(
          prisonerSearchApiClient.searchPrisonersByReleaseDate(
            any(),
            any(),
            any(),
            anyOrNull(),
          ),
        ).thenReturn(
          PageImpl(listOf(prisoner)),
        )

        whenever(hdcService.getHdcStatus(listOf(prisoner))).thenReturn(HdcStatuses(emptySet()))

        val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEqualTo(
          listOf(
            TestData.caCase().copy(
              kind = LicenceKind.CRD,
              licenceId = null,
              name = "Person Four",
              prisonerNumber = "A1234AA",
              releaseDate = twoDaysFromNow,
              licenceStatus = LicenceStatus.NOT_STARTED,
              probationPractitioner = ProbationPractitioner(
                staffCode = "X1234",
                name = "Joe Bloggs",
                staffIdentifier = null,
                staffUsername = null,
              ),

              lastWorkedOnBy = null,
            ),
          ),
        )
      }

      @Test
      fun `should filter out cases with no deliusRecord`() {
        whenever(licenceService.findLicencesMatchingCriteria(prisonLicenceQueryObject)).thenReturn(
          emptyList(),
        )
        whenever(
          cvlRecordService.getCvlRecords(
            any(),
            any(),
          ),
        ).thenReturn(listOf(TestData.aCvlRecord(kind = LicenceKind.CRD)))
        whenever(
          prisonerSearchApiClient.searchPrisonersByReleaseDate(
            any(),
            any(),
            any(),
            anyOrNull(),
          ),
        ).thenReturn(
          PageImpl(
            listOf(
              aPrisonerSearchPrisoner.copy(
                prisonerNumber = "A1234AA",
                conditionalReleaseDate = fiveDaysFromNow,
                confirmedReleaseDate = twoDaysFromNow,
                status = "ACTIVE IN",
                legalStatus = "SENTENCED",
                homeDetentionCurfewEligibilityDate = null,
                bookingId = "1234",
              ),
            ),
          ),
        )
        whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

        whenever(deliusApiClient.getOffenderManagers(any(), anyOrNull())).thenReturn(emptyList())

        val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEmpty()
      }
    }
  }

  @Nested
  inner class `Probation tab caseload` {
    @Test
    fun `should return sorted results in descending order`() {
      whenever(licenceService.findLicencesMatchingCriteria(any())).thenReturn(
        listOf(
          aLicenceSummary.copy(
            licenceStartDate = twoDaysFromNow,
            actualReleaseDate = twoDaysFromNow,
          ),
          aLicenceSummary.copy(
            licenceId = 2,
            licenceStatus = LicenceStatus.IN_PROGRESS,
            nomisId = "A1234AB",
            forename = "Person",
            surname = "Two",
            comUsername = "tcom",
            licenceStartDate = tenDaysFromNow,
            actualReleaseDate = tenDaysFromNow,
          ),
          aLicenceSummary.copy(
            licenceId = 3,
            licenceStatus = LicenceStatus.IN_PROGRESS,
            nomisId = "A1234AC",
            forename = "Person",
            surname = "Six",
            comUsername = "tcom",
            licenceStartDate = twoMonthsFromNow,
            actualReleaseDate = twoMonthsFromNow,
          ),
          aLicenceSummary.copy(
            licenceId = 4,
            licenceStatus = LicenceStatus.IN_PROGRESS,
            nomisId = "A1234AD",
            forename = "Person",
            surname = "Five",
            comUsername = "tcom",
            licenceStartDate = oneDayFromNow,
            actualReleaseDate = oneDayFromNow,
          ),
        ),
      )

      val probationOmuCaseload = service.getProbationOmuCaseload(setOf("BAI"), "")
      assertThat(probationOmuCaseload).isEqualTo(
        listOf(
          TestData.caCase().copy(
            licenceId = 3,
            name = "Person Six",
            prisonerNumber = "A1234AC",
            releaseDate = twoMonthsFromNow,
            tabType = null,
            nomisLegalStatus = null,
            probationPractitioner = ProbationPractitioner(
              staffCode = null,
              name = null,
              staffIdentifier = null,
              staffUsername = "tcom",
            ),
            lastWorkedOnBy = "X Y",
            prisonCode = "BAI",
            prisonDescription = "Moorland (HMP)",
          ),
          TestData.caCase().copy(
            licenceId = 2,
            name = "Person Two",
            prisonerNumber = "A1234AB",
            releaseDate = tenDaysFromNow,
            tabType = null,
            nomisLegalStatus = null,
            probationPractitioner = ProbationPractitioner(
              staffCode = null,
              name = null,
              staffIdentifier = null,
              staffUsername = "tcom",
            ),
            lastWorkedOnBy = "X Y",
            prisonCode = "BAI",
            prisonDescription = "Moorland (HMP)",
          ),
          TestData.caCase().copy(
            licenceId = 1,
            name = "Person One",
            prisonerNumber = "A1234AA",
            releaseDate = twoDaysFromNow,
            tabType = null,
            nomisLegalStatus = null,
            probationPractitioner = ProbationPractitioner(
              staffCode = "AB00001",
              name = "com user",
              staffIdentifier = null,
              staffUsername = null,
            ),
            lastWorkedOnBy = "X Y",
            prisonCode = "BAI",
            prisonDescription = "Moorland (HMP)",
          ),
          TestData.caCase().copy(
            licenceId = 4,
            prisonerNumber = "A1234AD",
            name = "Person Five",
            releaseDate = oneDayFromNow,
            tabType = null,
            nomisLegalStatus = null,
            probationPractitioner = ProbationPractitioner(
              staffCode = null,
              name = null,
              staffIdentifier = null,
              staffUsername = "tcom",
            ),
            lastWorkedOnBy = "X Y",
            prisonCode = "BAI",
            prisonDescription = "Moorland (HMP)",
          ),
        ),
      )
    }
  }

  @Nested
  inner class `Search for offender on prison case admin caseload` {
    @Test
    fun `should successfully search by name for offender in prison`() {
      assertThat(service.searchForOffenderOnPrisonCaseAdminCaseload(aPrisonUserSearchRequest.copy(query = "One")))
        .isEqualTo(
          PrisonCaseAdminSearchResult(
            inPrisonResults = listOf(
              TestData.caCase().copy(
                licenceId = 1,
                prisonerNumber = "A1234AA",
                name = "Person One",
                nomisLegalStatus = "SENTENCED",
                lastWorkedOnBy = "X Y",
                probationPractitioner = ProbationPractitioner(staffCode = "AB00001", name = "com user"),
                prisonCode = "BAI",
                prisonDescription = "Moorland (HMP)",
              ),
            ),
            onProbationResults = emptyList(),
            attentionNeededResults = emptyList(),
          ),
        )
    }

    @Test
    fun `should successfully search by prison number for offender in prison`() {
      assertThat(service.searchForOffenderOnPrisonCaseAdminCaseload(aPrisonUserSearchRequest.copy(query = "A1234AA")))
        .isEqualTo(
          PrisonCaseAdminSearchResult(
            inPrisonResults = listOf(
              TestData.caCase().copy(
                licenceId = 1,
                prisonerNumber = "A1234AA",
                name = "Person One",
                nomisLegalStatus = "SENTENCED",
                lastWorkedOnBy = "X Y",
                probationPractitioner = ProbationPractitioner(staffCode = "AB00001", name = "com user"),
                prisonCode = "BAI",
                prisonDescription = "Moorland (HMP)",
              ),
            ),
            onProbationResults = emptyList(),
            attentionNeededResults = emptyList(),
          ),
        )
    }

    @Test
    fun `should successfully search by probation practitioner name for offender in prison`() {
      assertThat(service.searchForOffenderOnPrisonCaseAdminCaseload(aPrisonUserSearchRequest.copy(query = "com")))
        .isEqualTo(
          PrisonCaseAdminSearchResult(
            inPrisonResults = listOf(
              TestData.caCase().copy(
                licenceId = 1,
                prisonerNumber = "A1234AA",
                name = "Person One",
                nomisLegalStatus = "SENTENCED",
                lastWorkedOnBy = "X Y",
                probationPractitioner = ProbationPractitioner(staffCode = "AB00001", name = "com user"),
                prisonCode = "BAI",
                prisonDescription = "Moorland (HMP)",
              ),
            ),
            onProbationResults = emptyList(),
            attentionNeededResults = emptyList(),
          ),
        )
    }

    @Test
    fun `should successfully search prison should return results in LSD ascending and then secondary id order`() {
      // Given
      val licenceSummaryList = listOf(
        aLicenceSummary.copy(
          licenceId = 1,
          licenceStatus = LicenceStatus.SUBMITTED,
          nomisId = "A1234AC",
          licenceStartDate = LocalDate.now().minusDays(1),
          forename = "Last",
        ),
        aLicenceSummary.copy(
          licenceId = 2,
          licenceStatus = LicenceStatus.SUBMITTED,
          nomisId = "A1234BC",
          licenceStartDate = LocalDate.now(),
          forename = "Second",
        ),
        aLicenceSummary.copy(
          licenceId = 5,
          licenceStatus = LicenceStatus.SUBMITTED,
          nomisId = "A1234BD",
          licenceStartDate = LocalDate.now(),
          forename = "Forth",
        ),
        aLicenceSummary.copy(
          licenceId = 3,
          licenceStatus = LicenceStatus.SUBMITTED,
          nomisId = "A1234CC",
          licenceStartDate = LocalDate.now().plusDays(1),
          forename = "First",
        ),
        aLicenceSummary.copy(
          licenceId = 4,
          licenceStatus = LicenceStatus.SUBMITTED,
          nomisId = "A1234DC",
          licenceStartDate = LocalDate.now(),
          forename = "Third",
        ),
      )

      whenever(licenceService.findLicencesMatchingCriteria(prisonLicenceQueryObject)).thenReturn(
        licenceSummaryList,
      )

      val prisoners = licenceSummaryList.associateBy { it.nomisId }.values.map {
        TestData.prisonerSearchResult().copy(
          prisonerNumber = it.nomisId,
          firstName = it.forename!!,
          dateOfBirth = LocalDate.of(1985, 12, 28),
        )
      }

      whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
        prisoners,
      )

      // When
      val results =
        service.searchForOffenderOnPrisonCaseAdminCaseload(aPrisonUserSearchRequest.copy(query = "com"))

      // Then
      assertThat(results.inPrisonResults.map { it.licenceId }).isEqualTo(listOf(1L, 2L, 4L, 5L, 3L))
    }

    @Test
    fun `should return all results when query string is empty`() {
      whenever(licenceService.findLicencesMatchingCriteria(probationLicenceQueryObject)).thenReturn(
        listOf(
          aLicenceSummary.copy(
            licenceId = 3,
            licenceStatus = LicenceStatus.ACTIVE,
            nomisId = "A1234AC",
            forename = "Person",
            surname = "Three",
          ),
        ),
      )
      assertThat(service.searchForOffenderOnPrisonCaseAdminCaseload(aPrisonUserSearchRequest.copy(query = "")))
        .isEqualTo(
          PrisonCaseAdminSearchResult(
            inPrisonResults = listOf(
              TestData.caCase().copy(
                licenceId = 1,
                prisonerNumber = "A1234AA",
                name = "Person One",
                nomisLegalStatus = "SENTENCED",
                lastWorkedOnBy = "X Y",
                probationPractitioner = ProbationPractitioner(staffCode = "AB00001", name = "com user"),
                prisonCode = "BAI",
                prisonDescription = "Moorland (HMP)",
              ),
              TestData.caCase().copy(
                licenceId = 2,
                prisonerNumber = "A1234AB",
                name = "Person Two",
                nomisLegalStatus = "SENTENCED",
                lastWorkedOnBy = "X Y",
                probationPractitioner = ProbationPractitioner(staffUsername = "tcom"),
                prisonCode = "BAI",
                prisonDescription = "Moorland (HMP)",
              ),
            ),
            onProbationResults = listOf(
              TestData.caCase().copy(
                licenceId = 3,
                prisonerNumber = "A1234AC",
                licenceStatus = LicenceStatus.ACTIVE,
                name = "Person Three",
                nomisLegalStatus = null,
                tabType = null,
                lastWorkedOnBy = "X Y",
                probationPractitioner = ProbationPractitioner(staffCode = "AB00001", name = "com user"),
                prisonCode = "BAI",
                prisonDescription = "Moorland (HMP)",
              ),
            ),
            attentionNeededResults = listOf(
              TestData.caCase().copy(
                licenceId = 3,
                prisonerNumber = "A1234AC",
                name = "Person Three",
                nomisLegalStatus = "SENTENCED",
                releaseDate = null,
                releaseDateLabel = "CRD",
                tabType = CaViewCasesTab.ATTENTION_NEEDED,
                lastWorkedOnBy = "X Y",
                probationPractitioner = ProbationPractitioner(
                  staffCode = "AB00002",
                  name = "anotherforename anothersurname",
                ),
                prisonCode = "BAI",
                prisonDescription = "Moorland (HMP)",
              ),
            ),
          ),
        )
    }
  }

  @Nested
  inner class `Search for offender on probation case admin caseload` {
    @Test
    fun `should successfully search by name for offender on probation`() {
      whenever(licenceService.findLicencesMatchingCriteria(probationLicenceQueryObject)).thenReturn(
        listOf(
          aLicenceSummary.copy(
            licenceId = 4,
            licenceStatus = LicenceStatus.ACTIVE,
            nomisId = "A1234AD",
            forename = "Person",
            surname = "Four",
          ),
        ),
      )
      assertThat(service.searchForOffenderOnPrisonCaseAdminCaseload(aPrisonUserSearchRequest.copy(query = "Four")))
        .isEqualTo(
          PrisonCaseAdminSearchResult(
            inPrisonResults = emptyList(),
            onProbationResults = listOf(
              TestData.caCase().copy(
                licenceId = 4,
                prisonerNumber = "A1234AD",
                licenceStatus = LicenceStatus.ACTIVE,
                name = "Person Four",
                nomisLegalStatus = null,
                tabType = null,
                lastWorkedOnBy = "X Y",
                probationPractitioner = ProbationPractitioner(staffCode = "AB00001", name = "com user"),
                prisonCode = "BAI",
                prisonDescription = "Moorland (HMP)",
              ),
            ),
            attentionNeededResults = emptyList(),
          ),
        )
    }

    @Test
    fun `should successfully search by prison number for offender on probation`() {
      whenever(licenceService.findLicencesMatchingCriteria(probationLicenceQueryObject)).thenReturn(
        listOf(
          aLicenceSummary.copy(
            licenceId = 4,
            licenceStatus = LicenceStatus.ACTIVE,
            nomisId = "A1234AD",
            forename = "Person",
            surname = "Four",
          ),
        ),
      )
      assertThat(service.searchForOffenderOnPrisonCaseAdminCaseload(aPrisonUserSearchRequest.copy(query = "A1234AD")))
        .isEqualTo(
          PrisonCaseAdminSearchResult(
            inPrisonResults = emptyList(),
            onProbationResults = listOf(
              TestData.caCase().copy(
                licenceId = 4,
                prisonerNumber = "A1234AD",
                licenceStatus = LicenceStatus.ACTIVE,
                name = "Person Four",
                nomisLegalStatus = null,
                tabType = null,
                lastWorkedOnBy = "X Y",
                probationPractitioner = ProbationPractitioner(staffCode = "AB00001", name = "com user"),
                prisonCode = "BAI",
                prisonDescription = "Moorland (HMP)",
              ),
            ),
            attentionNeededResults = emptyList(),
          ),
        )
    }

    @Test
    fun `should successfully search by probation practitioner name for offender on probation`() {
      whenever(licenceService.findLicencesMatchingCriteria(prisonLicenceQueryObject)).thenReturn(emptyList())
      whenever(licenceService.findLicencesMatchingCriteria(probationLicenceQueryObject)).thenReturn(
        listOf(
          aLicenceSummary.copy(
            licenceId = 3,
            licenceStatus = LicenceStatus.ACTIVE,
            nomisId = "A1234AC",
            forename = "Person",
            surname = "Three",
          ),
        ),
      )
      assertThat(service.searchForOffenderOnPrisonCaseAdminCaseload(aPrisonUserSearchRequest.copy(query = "com")))
        .isEqualTo(
          PrisonCaseAdminSearchResult(
            inPrisonResults = emptyList(),
            onProbationResults = listOf(
              TestData.caCase().copy(
                licenceId = 3,
                prisonerNumber = "A1234AC",
                licenceStatus = LicenceStatus.ACTIVE,
                name = "Person Three",
                nomisLegalStatus = null,
                tabType = null,
                lastWorkedOnBy = "X Y",
                probationPractitioner = ProbationPractitioner(staffCode = "AB00001", name = "com user"),
                prisonCode = "BAI",
                prisonDescription = "Moorland (HMP)",
              ),
            ),
            attentionNeededResults = emptyList(),
          ),
        )
    }

    @Test
    fun `should successfully search and return both prison and probation results`() {
      // Given

      whenever(licenceService.findLicencesMatchingCriteria(probationLicenceQueryObject)).thenReturn(
        listOf(
          aLicenceSummary.copy(
            licenceId = 3,
            licenceStatus = LicenceStatus.ACTIVE,
            nomisId = "A1234AC",
            forename = "Person",
            surname = "Three",
          ),
        ),
      )

      // When
      val results =
        service.searchForOffenderOnPrisonCaseAdminCaseload(aPrisonUserSearchRequest.copy(query = "com"))

      // Then
      assertThat(results).isEqualTo(
        PrisonCaseAdminSearchResult(
          inPrisonResults = listOf(
            TestData.caCase().copy(
              licenceId = 1,
              prisonerNumber = "A1234AA",
              name = "Person One",
              nomisLegalStatus = "SENTENCED",
              lastWorkedOnBy = "X Y",
              probationPractitioner = ProbationPractitioner(staffCode = "AB00001", name = "com user"),
              prisonCode = "BAI",
              prisonDescription = "Moorland (HMP)",
            ),
          ),
          onProbationResults = listOf(
            TestData.caCase().copy(
              licenceId = 3,
              prisonerNumber = "A1234AC",
              licenceStatus = LicenceStatus.ACTIVE,
              name = "Person Three",
              nomisLegalStatus = null,
              tabType = null,
              lastWorkedOnBy = "X Y",
              probationPractitioner = ProbationPractitioner(staffCode = "AB00001", name = "com user"),
              prisonCode = "BAI",
              prisonDescription = "Moorland (HMP)",
            ),
          ),
          attentionNeededResults = emptyList(),
        ),
      )
    }

    @Test
    fun `should successfully search probation should return results in LSD descending and then secondary id order`() {
      // Given

      val licenceSummaryList = listOf(
        aLicenceSummary.copy(
          licenceId = 1,
          licenceStatus = LicenceStatus.ACTIVE,
          nomisId = "A1234AC",
          licenceStartDate = LocalDate.now().minusDays(1),
          forename = "Last",
        ),
        aLicenceSummary.copy(
          licenceId = 2,
          licenceStatus = LicenceStatus.ACTIVE,
          nomisId = "A1234BC",
          licenceStartDate = LocalDate.now(),
          forename = "Second",
        ),
        aLicenceSummary.copy(
          licenceId = 5,
          licenceStatus = LicenceStatus.ACTIVE,
          nomisId = "A1234BD",
          licenceStartDate = LocalDate.now(),
          forename = "Forth",
        ),
        aLicenceSummary.copy(
          licenceId = 3,
          licenceStatus = LicenceStatus.ACTIVE,
          nomisId = "A1234CC",
          licenceStartDate = LocalDate.now().plusDays(1),
          forename = "First",
        ),
        aLicenceSummary.copy(
          licenceId = 4,
          licenceStatus = LicenceStatus.ACTIVE,
          nomisId = "A1234DC",
          licenceStartDate = LocalDate.now(),
          forename = "Third",
        ),
      )

      whenever(licenceService.findLicencesMatchingCriteria(probationLicenceQueryObject)).thenReturn(
        licenceSummaryList,
      )

      // When
      val results =
        service.searchForOffenderOnPrisonCaseAdminCaseload(aPrisonUserSearchRequest.copy(query = "com"))

      // Then
      assertThat(results.onProbationResults.map { it.licenceId }).isEqualTo(listOf(3L, 2L, 4L, 5L, 1L))
    }

    @Test
    fun `should return all results when query string is empty`() {
      whenever(licenceService.findLicencesMatchingCriteria(probationLicenceQueryObject)).thenReturn(
        listOf(
          aLicenceSummary.copy(
            licenceId = 4,
            licenceStatus = LicenceStatus.ACTIVE,
            nomisId = "A1234AD",
            forename = "Person",
            surname = "Four",
          ),
        ),
      )
      assertThat(service.searchForOffenderOnPrisonCaseAdminCaseload(aPrisonUserSearchRequest.copy(query = "")))
        .isEqualTo(
          PrisonCaseAdminSearchResult(
            inPrisonResults = listOf(
              TestData.caCase().copy(
                licenceId = 1,
                prisonerNumber = "A1234AA",
                name = "Person One",
                nomisLegalStatus = "SENTENCED",
                lastWorkedOnBy = "X Y",
                probationPractitioner = ProbationPractitioner(staffCode = "AB00001", name = "com user"),
                prisonCode = "BAI",
                prisonDescription = "Moorland (HMP)",
              ),
              TestData.caCase().copy(
                licenceId = 2,
                prisonerNumber = "A1234AB",
                name = "Person Two",
                nomisLegalStatus = "SENTENCED",
                lastWorkedOnBy = "X Y",
                probationPractitioner = ProbationPractitioner(staffUsername = "tcom"),
                prisonCode = "BAI",
                prisonDescription = "Moorland (HMP)",
              ),
            ),
            onProbationResults = listOf(
              TestData.caCase().copy(
                licenceId = 4,
                prisonerNumber = "A1234AD",
                licenceStatus = LicenceStatus.ACTIVE,
                name = "Person Four",
                nomisLegalStatus = null,
                tabType = null,
                lastWorkedOnBy = "X Y",
                probationPractitioner = ProbationPractitioner(staffCode = "AB00001", name = "com user"),
                prisonCode = "BAI",
                prisonDescription = "Moorland (HMP)",
              ),
            ),
            attentionNeededResults = listOf(
              TestData.caCase().copy(
                licenceId = 3,
                prisonerNumber = "A1234AC",
                name = "Person Three",
                nomisLegalStatus = "SENTENCED",
                releaseDate = null,
                releaseDateLabel = "CRD",
                tabType = CaViewCasesTab.ATTENTION_NEEDED,
                lastWorkedOnBy = "X Y",
                probationPractitioner = ProbationPractitioner(
                  staffCode = "AB00002",
                  name = "anotherforename anothersurname",
                ),
                prisonCode = "BAI",
                prisonDescription = "Moorland (HMP)",
              ),
            ),
          ),
        )
    }
  }

  @Nested
  inner class `Search for offender on attention needed caseload` {
    @Test
    fun `should successfully search by name for offender in prison`() {
      assertThat(service.searchForOffenderOnPrisonCaseAdminCaseload(aPrisonUserSearchRequest.copy(query = "Three")))
        .isEqualTo(
          PrisonCaseAdminSearchResult(
            inPrisonResults = emptyList(),
            onProbationResults = emptyList(),
            attentionNeededResults = listOf(
              TestData.caCase().copy(
                licenceId = 3,
                prisonerNumber = "A1234AC",
                name = "Person Three",
                nomisLegalStatus = "SENTENCED",
                releaseDate = null,
                releaseDateLabel = "CRD",
                tabType = CaViewCasesTab.ATTENTION_NEEDED,
                lastWorkedOnBy = "X Y",
                probationPractitioner = ProbationPractitioner(
                  staffCode = "AB00002",
                  name = "anotherforename anothersurname",
                ),
                prisonCode = "BAI",
                prisonDescription = "Moorland (HMP)",
              ),
            ),
          ),
        )
    }

    @Test
    fun `should successfully search by prison number for offender in prison`() {
      assertThat(service.searchForOffenderOnPrisonCaseAdminCaseload(aPrisonUserSearchRequest.copy(query = "A1234AC")))
        .isEqualTo(
          PrisonCaseAdminSearchResult(
            inPrisonResults = emptyList(),
            onProbationResults = emptyList(),
            attentionNeededResults = listOf(
              TestData.caCase().copy(
                licenceId = 3,
                prisonerNumber = "A1234AC",
                name = "Person Three",
                nomisLegalStatus = "SENTENCED",
                releaseDate = null,
                releaseDateLabel = "CRD",
                tabType = CaViewCasesTab.ATTENTION_NEEDED,
                lastWorkedOnBy = "X Y",
                probationPractitioner = ProbationPractitioner(
                  staffCode = "AB00002",
                  name = "anotherforename anothersurname",
                ),
                prisonCode = "BAI",
                prisonDescription = "Moorland (HMP)",
              ),
            ),
          ),
        )
    }

    @Test
    fun `should successfully search by probation practitioner name for offender in prison`() {
      assertThat(service.searchForOffenderOnPrisonCaseAdminCaseload(aPrisonUserSearchRequest.copy(query = "anothersurname")))
        .isEqualTo(
          PrisonCaseAdminSearchResult(
            inPrisonResults = emptyList(),
            onProbationResults = emptyList(),
            attentionNeededResults = listOf(
              TestData.caCase().copy(
                licenceId = 3,
                prisonerNumber = "A1234AC",
                name = "Person Three",
                nomisLegalStatus = "SENTENCED",
                releaseDate = null,
                releaseDateLabel = "CRD",
                tabType = CaViewCasesTab.ATTENTION_NEEDED,
                lastWorkedOnBy = "X Y",
                probationPractitioner = ProbationPractitioner(
                  staffCode = "AB00002",
                  name = "anotherforename anothersurname",
                ),
                prisonCode = "BAI",
                prisonDescription = "Moorland (HMP)",
              ),
            ),
          ),
        )
    }

    @Test
    fun `should successfully search prison should return results by id`() {
      // Given
      val licenceSummaryList = listOf(
        aLicenceSummary.copy(
          licenceId = 1,
          licenceStatus = LicenceStatus.SUBMITTED,
          nomisId = "A1234AC",
          licenceStartDate = null,
          forename = "Last",
        ),
        aLicenceSummary.copy(
          licenceId = 2,
          licenceStatus = LicenceStatus.SUBMITTED,
          nomisId = "A1234BC",
          licenceStartDate = null,
          forename = "Second",
        ),
        aLicenceSummary.copy(
          licenceId = 5,
          licenceStatus = LicenceStatus.SUBMITTED,
          nomisId = "A1234BD",
          licenceStartDate = null,
          forename = "Forth",
        ),
        aLicenceSummary.copy(
          licenceId = 3,
          licenceStatus = LicenceStatus.SUBMITTED,
          nomisId = "A1234CC",
          licenceStartDate = null,
          forename = "First",
        ),
        aLicenceSummary.copy(
          licenceId = 4,
          licenceStatus = LicenceStatus.SUBMITTED,
          nomisId = "A1234DC",
          licenceStartDate = null,
          forename = "Third",
        ),
      )

      whenever(licenceService.findLicencesMatchingCriteria(prisonLicenceQueryObject)).thenReturn(
        licenceSummaryList,
      )

      val prisoners = licenceSummaryList.associateBy { it.nomisId }.values.map {
        TestData.prisonerSearchResult().copy(
          prisonerNumber = it.nomisId,
          firstName = it.forename!!,
          dateOfBirth = LocalDate.of(1985, 12, 28),
        )
      }

      whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
        prisoners,
      )

      // When
      val results =
        service.searchForOffenderOnPrisonCaseAdminCaseload(aPrisonUserSearchRequest.copy(query = "com"))

      // Then
      assertThat(results.attentionNeededResults.map { it.licenceId }).isEqualTo(listOf(1L, 2L, 3L, 4L, 5L))
    }
  }

  @Test
  fun `should have correct releaseDateLabel when postRecallReleaseDate is the same as licenceStartDate`() {
    // Given
    val licenceSummary = aLicenceSummary.copy(
      licenceStartDate = tenDaysFromNow,
      postRecallReleaseDate = tenDaysFromNow,
    )
    whenever(workingDaysService.getLastWorkingDay(licenceSummary.postRecallReleaseDate)).thenReturn(licenceSummary.postRecallReleaseDate)
    whenever(licenceService.findLicencesMatchingCriteria(any())).thenReturn(listOf(licenceSummary))

    // When
    val prisonOmuCaseload = service.getProbationOmuCaseload(setOf("BAI"), "")

    // Then
    assertThat(prisonOmuCaseload).hasSize(1)
    assertThat(prisonOmuCaseload[0].releaseDateLabel).isEqualTo("Post-recall release date (PRRD)")
  }

  @Test
  fun `should use HDCAD as release label where a HDCAD is set`() {
    whenever(licenceService.findLicencesMatchingCriteria(any())).thenReturn(
      listOf(
        aLicenceSummary.copy(
          licenceStartDate = oneDayFromNow,
          homeDetentionCurfewActualDate = oneDayFromNow,
        ),
      ),
    )
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

    val probationOmuCaseload = service.getProbationOmuCaseload(setOf("BAI"), "")
    assertThat(probationOmuCaseload).isEqualTo(
      listOf(
        TestData.caCase().copy(
          licenceId = 1,
          name = "Person One",
          prisonerNumber = "A1234AA",
          releaseDate = oneDayFromNow,
          tabType = null,
          nomisLegalStatus = null,
          probationPractitioner = ProbationPractitioner(
            staffCode = "AB00001",
            name = "com user",
            staffIdentifier = null,
            staffUsername = null,
          ),
          lastWorkedOnBy = "X Y",
          releaseDateLabel = "HDCAD",
          prisonCode = "BAI",
          prisonDescription = "Moorland (HMP)",
        ),
      ),
    )
  }

  private companion object {
    private fun createClock(timestamp: String) = Clock.fixed(Instant.parse(timestamp), ZoneId.systemDefault())

    val dateTime: LocalDateTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 13, 39))
    val instant: Instant = dateTime.atZone(ZoneId.systemDefault()).toInstant()
    val clock: Clock = createClock(instant.toString())

    val oneDayFromNow: LocalDate = LocalDate.now(clock).plusDays(1)
    val twoDaysFromNow: LocalDate = LocalDate.now(clock).plusDays(2)
    val tenDaysFromNow: LocalDate = LocalDate.now(clock).plusDays(10)
    val twoMonthsFromNow: LocalDate = LocalDate.now(clock).plusMonths(2)
    val fiveDaysFromNow: LocalDate = LocalDate.now(clock).plusDays(5)

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

    val probationCase = ProbationCase(crn = "X12347", nomisId = "A1234AA")

    val aProbationPractitioner = ProbationPractitioner(staffCode = "DEF456")
    val comUser = StaffNameResponse(
      id = 2000L,
      username = "com-user",
      name = Name(
        forename = "com",
        surname = "user",
      ),
      code = "AB00001",
    )
    val atcomUser = StaffNameResponse(
      id = 2001L,
      username = "atcom",
      name = Name(
        forename = "anotherforename",
        surname = "anothersurname",
      ),
      code = "AB00002",
    )

    val aPrisonerSearchPrisoner = PrisonerSearchPrisoner(
      prisonerNumber = "AB1234F",
      pncNumber = null,
      bookingId = null,
      status = "ACTIVE IN",
      mostSeriousOffence = "Robbery",
      licenceExpiryDate = null,
      topupSupervisionExpiryDate = null,
      homeDetentionCurfewEligibilityDate = null,
      homeDetentionCurfewActualDate = null,
      homeDetentionCurfewEndDate = null,
      releaseDate = null,
      confirmedReleaseDate = null,
      conditionalReleaseDate = LocalDate.of(2024, 8, 2),
      paroleEligibilityDate = null,
      actualParoleDate = null,
      releaseOnTemporaryLicenceDate = null,
      postRecallReleaseDate = null,
      legalStatus = "SENTENCED",
      indeterminateSentence = null,
      imprisonmentStatus = null,
      imprisonmentStatusDescription = null,
      recall = null,
      prisonId = null,
      locationDescription = null,
      prisonName = null,
      bookNumber = null,
      firstName = "PERSON",
      middleNames = null,
      lastName = "FOUR",
      dateOfBirth = LocalDate.of(1985, 12, 28),
      conditionalReleaseDateOverrideDate = null,
      sentenceStartDate = null,
      sentenceExpiryDate = null,
      topupSupervisionStartDate = null,
      croNumber = null,
    )

    val aCommunityManager =
      CommunityManager(
        code = "X1234",
        id = 2000L,
        team = TeamDetail(
          code = "NA01A2-A",
          description = "Cardiff South Team A",
          borough = Detail(
            code = "N01A",
            description = "Cardiff",
          ),
          district = Detail(
            code = "N01A2",
            description = "Cardiff South",
          ),
          provider = Detail(
            code = "N01",
            description = "Wales",
          ),
        ),
        provider = Detail(
          code = "N01",
          description = "Wales",
        ),
        case = ProbationCase(crn = "A123456", nomisId = "A1234AA"),
        name = Name("Joe", null, "Bloggs"),
        allocationDate = LocalDate.of(2000, 1, 1),
        unallocated = false,
      )

    val aPrisonUserSearchRequest = PrisonUserSearchRequest(
      query = "Person",
      prisonCaseloads = setOf("BAI"),
    )
  }
}
