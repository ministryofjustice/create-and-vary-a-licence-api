package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService.HdcStatuses
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Detail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.TeamDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.User
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

  private val service = CaCaseloadService(
    caseloadService,
    licenceService,
    hdcService,
    eligibilityService,
    clock,
    deliusApiClient,
    prisonerSearchApiClient,
    releaseDateService,
  )

  private val statuses = listOf(
    LicenceStatus.APPROVED,
    LicenceStatus.SUBMITTED,
    LicenceStatus.IN_PROGRESS,
    LicenceStatus.TIMED_OUT,
    LicenceStatus.ACTIVE,
  )

  private val licenceQueryObject = LicenceQueryObject(
    statusCodes = statuses,
    prisonCodes = listOf("BAI"),
    sortBy = "licenceStartDate",
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
    )
    whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(
      listOf(
        aLicenceSummary,
        aLicenceSummary.copy(
          licenceId = 2,
          licenceStatus = LicenceStatus.IN_PROGRESS,
          nomisId = "A1234AB",
          forename = "Smith",
          surname = "Cena",
          comUsername = "Andy",
        ),
      ),
    )
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        TestData.caseLoadItem(),
        TestData.caseLoadItem().copy(
          prisoner = Prisoner(
            prisonerNumber = "A1234AB",
            firstName = "Smith",
            lastName = "Cena",
            legalStatus = "SENTENCED",
            dateOfBirth = LocalDate.of(1985, 12, 28),
            mostSeriousOffence = "Robbery",
          ),
        ),
      ),
    )
    whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
      PageImpl(
        listOf(
          aPrisonerSearchPrisoner,
          aPrisonerSearchPrisoner.copy(
            prisonerNumber = "A1234AB",
            firstName = "Smith",
            lastName = "Cena",
            legalStatus = "SENTENCED",
            dateOfBirth = LocalDate.of(1985, 12, 28),
            mostSeriousOffence = "Robbery",
          ),
        ),
      ),
    )
    whenever(deliusApiClient.getStaffDetailsByUsername(any())).thenReturn(listOf(comUser))
    whenever(deliusApiClient.getProbationCases(any(), anyOrNull())).thenReturn(listOf(probationCase))
    whenever(deliusApiClient.getOffenderManagers(any(), anyOrNull())).thenReturn(listOf(aCommunityManager))
  }

  @Nested
  inner class `Prison tab caseload` {
    @Nested
    inner class `in the hard stop period` {
      @Test
      fun `Sets NOT_STARTED licences to TIMED_OUT when in the hard stop period`() {
        whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(
          emptyList(),
        )
        whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
        whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
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
        whenever(releaseDateService.getHardStopDate(any())).thenReturn(LocalDate.of(2023, 10, 12))
        whenever(releaseDateService.getHardStopWarningDate(any())).thenReturn(LocalDate.of(2023, 10, 11))
        whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull())).thenReturn(true)
        whenever(releaseDateService.isDueForEarlyRelease(any())).thenReturn(true)
        whenever(releaseDateService.isEligibleForEarlyRelease(any<SentenceDateHolder>())).thenReturn(true)
        whenever(releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(any())).thenReturn(true)
        whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

        val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")

        assertThat(prisonOmuCaseload).hasSize(1)

        with(prisonOmuCaseload.first()) {
          assertThat(name).isEqualTo("Phil Cena")
          assertThat(licenceStatus).isEqualTo(LicenceStatus.TIMED_OUT)
          assertThat(isInHardStopPeriod).isTrue()
        }

        verify(licenceService, times(1)).findLicencesMatchingCriteria(licenceQueryObject)
        verify(caseloadService, times(0)).getPrisonersByNumber(listOf(aLicenceSummary.nomisId))
        verify(prisonerSearchApiClient, times(1)).searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull())
      }
    }

    @Nested
    inner class `find latest licenceSummary` {
      @Test
      fun `should return the first element if the licences length is one`() {
        val licences = aLicenceSummary.copy(licenceStatus = LicenceStatus.APPROVED)
        assertThat(service.findLatestLicenceSummary(listOf(licences))).isEqualTo(licences)
      }

      @Test
      fun `should return the IN_PROGRESS licence if there are IN_PROGRESS and TIMED_OUT licences`() {
        val licences =
          listOf(
            aLicenceSummary.copy(licenceStatus = LicenceStatus.IN_PROGRESS),
            aLicenceSummary.copy(licenceStatus = LicenceStatus.TIMED_OUT),
          )
        assertThat(service.findLatestLicenceSummary(licences)).isEqualTo(licences.first())
      }

      @Test
      fun `should return the SUBMITTED licence if there are IN_PROGRESS and SUBMITTED licences`() {
        val licences =
          listOf(
            aLicenceSummary.copy(licenceStatus = LicenceStatus.SUBMITTED),
            aLicenceSummary.copy(licenceStatus = LicenceStatus.IN_PROGRESS),
          )
        assertThat(service.findLatestLicenceSummary(licences)).isEqualTo(licences.first())
      }
    }

    @Nested
    inner class `split Cases By Com Details` {
      val caseWithComUsername = TestData.caCase().copy(
        probationPractitioner = ProbationPractitioner(
          staffUsername = "ABC123",
        ),
      )
      val caseWithComCode = TestData.caCase().copy(probationPractitioner = aProbationPractitioner)
      val caseWithNoComId = TestData.caCase().copy(probationPractitioner = ProbationPractitioner())

      @Test
      fun `initialises params to empty arrays if there are no relevant cases`() {
        assertThat(service.splitCasesByComDetails(listOf(caseWithComUsername))).isEqualTo(
          CaCaseloadService.GroupedByCom(
            withStaffCode = emptyList(),
            withStaffUsername = listOf(caseWithComUsername),
            withNoComId = emptyList(),
          ),
        )
        assertThat(service.splitCasesByComDetails(listOf(caseWithComCode))).isEqualTo(
          CaCaseloadService.GroupedByCom(
            withStaffCode = listOf(caseWithComCode),
            withStaffUsername = emptyList(),
            withNoComId = emptyList(),
          ),
        )
        assertThat(service.splitCasesByComDetails(listOf(caseWithNoComId))).isEqualTo(
          CaCaseloadService.GroupedByCom(
            withStaffCode = emptyList(),
            withStaffUsername = emptyList(),
            withNoComId = listOf(caseWithNoComId),
          ),
        )
      }
    }

    @Nested
    inner class `apply Search` {
      @Test
      fun `should successfully search by name`() {
        assertThat(service.getPrisonOmuCaseload(setOf("BAI"), "Smith")).isEqualTo(
          listOf(
            TestData.caCase().copy(
              licenceId = 2,
              prisonerNumber = "A1234AB",
              name = "Smith Cena",
              nomisLegalStatus = "SENTENCED",
              lastWorkedOnBy = "X Y",
              probationPractitioner = ProbationPractitioner(staffUsername = "Andy"),
            ),
          ),
        )
      }

      @Test
      fun `should successfully search by prison number`() {
        assertThat(service.getPrisonOmuCaseload(setOf("BAI"), "A1234AA")).isEqualTo(
          listOf(
            TestData.caCase().copy(
              name = "John Cena",
              lastWorkedOnBy = "X Y",
              probationPractitioner = ProbationPractitioner(staffCode = "AB00001", name = "com user"),
            ),
          ),
        )
      }

      @Test
      fun `should successfully search by probation practitioner`() {
        whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(
          listOf(
            aLicenceSummary,
            aLicenceSummary.copy(
              licenceId = 2,
              licenceStatus = LicenceStatus.IN_PROGRESS,
              nomisId = "A1234AB",
              forename = "Smith",
              surname = "Cena",
            ),
          ),
        )
        assertThat(service.getPrisonOmuCaseload(setOf("BAI"), "com")).isEqualTo(
          listOf(
            TestData.caCase().copy(
              name = "John Cena",
              lastWorkedOnBy = "X Y",
              probationPractitioner = ProbationPractitioner(
                staffCode = "AB00001",
                name = "com user",
                staffIdentifier = null,
                staffUsername = null,
              ),
            ),
            TestData.caCase().copy(
              licenceId = 2,
              prisonerNumber = "A1234AB",
              name = "Smith Cena",
              nomisLegalStatus = "SENTENCED",
              lastWorkedOnBy = "X Y",
              probationPractitioner = ProbationPractitioner(
                staffCode = "AB00001",
                name = "com user",
                staffIdentifier = null,
                staffUsername = null,
              ),
            ),
          ),
        )
      }
    }

    @Test
    fun `should filter out cases with an existing ACTIVE licence`() {
      whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(
        listOf(
          aLicenceSummary.copy(
            forename = "Steve",
            surname = "Cena",
            nomisId = "AB1234E",
            licenceId = 2,
            licenceType = LicenceType.PSS,
            licenceStatus = LicenceStatus.ACTIVE,
            isInHardStopPeriod = false,
            isDueToBeReleasedInTheNextTwoWorkingDays = true,
            conditionalReleaseDate = twoMonthsFromNow,
            actualReleaseDate = twoDaysFromNow,
            isDueForEarlyRelease = true,
          ),
        ),
      )

      whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
        listOf(
          TestData.caseLoadItem().copy(
            prisoner = Prisoner(
              firstName = "Steve",
              lastName = "Cena",
              prisonerNumber = "AB1234E",
              conditionalReleaseDate = twoMonthsFromNow,
              confirmedReleaseDate = twoDaysFromNow,
              status = "ACTIVE IN",
              legalStatus = "SENTENCED",
              dateOfBirth = LocalDate.of(1985, 12, 28),
              mostSeriousOffence = "Robbery",
            ),
            cvl = CvlFields(
              licenceType = LicenceType.AP,
              isDueForEarlyRelease = true,
              isInHardStopPeriod = false,
              isDueToBeReleasedInTheNextTwoWorkingDays = false,
            ),
          ),
        ),
      )

      whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
        PageImpl(
          listOf(
            aPrisonerSearchPrisoner.copy(
              firstName = "Steve",
              lastName = "Cena",
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
      service.getPrisonOmuCaseload(setOf("BAI"), "Smith")
      verify(prisonerSearchApiClient, times(1)).searchPrisonersByReleaseDate(
        LocalDate.now(clock),
        LocalDate.now(clock).plusWeeks(4),
        setOf("BAI"),
        0,
      )
    }

    @Test
    fun `should filter out duplicate cases, prioritising existing licences`() {
      whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(
        listOf(
          aLicenceSummary.copy(
            forename = "Steve",
            surname = "Cena",
            nomisId = "AB1234E",
            licenceId = 2,
            licenceType = LicenceType.PSS,
            licenceStatus = LicenceStatus.IN_PROGRESS,
            isInHardStopPeriod = false,
            isDueToBeReleasedInTheNextTwoWorkingDays = true,
            licenceStartDate = twoDaysFromNow,
            conditionalReleaseDate = twoMonthsFromNow,
            actualReleaseDate = twoDaysFromNow,
            isDueForEarlyRelease = true,
          ),
        ),
      )

      whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
        listOf(
          TestData.caseLoadItem().copy(
            prisoner = Prisoner(
              firstName = "Steve",
              lastName = "Cena",
              prisonerNumber = "AB1234E",
              conditionalReleaseDate = twoMonthsFromNow,
              confirmedReleaseDate = twoDaysFromNow,
              status = "ACTIVE IN",
              legalStatus = "SENTENCED",
              dateOfBirth = LocalDate.of(1985, 12, 28),
              mostSeriousOffence = "Robbery",
            ),
            cvl = CvlFields(
              licenceType = LicenceType.AP,
              isDueForEarlyRelease = true,
              isInHardStopPeriod = false,
              isDueToBeReleasedInTheNextTwoWorkingDays = false,
            ),
          ),
        ),
      )

      whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
        PageImpl(
          listOf(
            aPrisonerSearchPrisoner.copy(
              firstName = "Steve",
              lastName = "Cena",
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
            name = "Steve Cena",
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
            isDueForEarlyRelease = true,
            isInHardStopPeriod = false,
          ),
        ),
      )
    }

    @Test
    fun `should return sorted results in ascending order`() {
      whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(
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
            forename = "Smith",
            surname = "Cena",
            comUsername = "Andy",
            conditionalReleaseDate = tenDaysFromNow,
            actualReleaseDate = twoDaysFromNow,
            licenceStartDate = twoDaysFromNow,
          ),
        ),
      )
      whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
        listOf(
          TestData.caseLoadItem(),
          TestData.caseLoadItem().copy(
            prisoner = Prisoner(
              prisonerNumber = "A1234AB",
              firstName = "Smith",
              lastName = "Cena",
              legalStatus = "SENTENCED",
              dateOfBirth = LocalDate.of(1985, 12, 28),
              mostSeriousOffence = "Robbery",
            ),
          ),
        ),
      )
      whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
        listOf(
          TestData.caseLoadItem(),
          TestData.caseLoadItem().copy(
            prisoner = Prisoner(
              prisonerNumber = "A1234AB",
              firstName = "Smith",
              lastName = "Cena",
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
            name = "Smith Cena",
            prisonerNumber = "A1234AB",
            releaseDate = twoDaysFromNow,
            releaseDateLabel = "Confirmed release date",
            probationPractitioner = ProbationPractitioner(
              staffCode = null,
              name = null,
              staffIdentifier = null,
              staffUsername = "Andy",
            ),
            lastWorkedOnBy = "X Y",
          ),
          TestData.caCase().copy(
            name = "John Cena",
            releaseDate = tenDaysFromNow,
            releaseDateLabel = "CRD",
            probationPractitioner = ProbationPractitioner(
              staffCode = "AB00001",
              name = "com user",
              staffIdentifier = null,
              staffUsername = null,
            ),
            lastWorkedOnBy = "X Y",
          ),
        ),
      )
    }

    @Nested
    inner class `filtering rules` {
      @Test
      fun `should filter out cases with a future PED`() {
        whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
          PageImpl(
            listOf(
              aPrisonerSearchPrisoner.copy(
                paroleEligibilityDate = twoDaysFromNow,
              ),
              aPrisonerSearchPrisoner.copy(
                prisonerNumber = "A1234AB",
                firstName = "Smith",
                lastName = "Cena",
                legalStatus = "SENTENCED",
                paroleEligibilityDate = twoDaysFromNow,
                dateOfBirth = LocalDate.of(1985, 12, 28),
                mostSeriousOffence = "Robbery",
              ),
            ),
          ),
        )

        whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(emptyList())

        val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEqualTo(emptyList<CaCase>())
      }

      @Test
      fun `Should filter out cases with a legal status of DEAD`() {
        whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
          PageImpl(
            listOf(
              aPrisonerSearchPrisoner.copy(
                legalStatus = "DEAD",
              ),
            ),
          ),
        )

        whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(emptyList())

        val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEqualTo(emptyList<CaCase>())
      }

      @Test
      fun `should filter out cases on an indeterminate sentence`() {
        whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
          PageImpl(
            listOf(
              aPrisonerSearchPrisoner.copy(
                indeterminateSentence = true,
              ),
            ),
          ),
        )

        whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(emptyList())

        val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEqualTo(emptyList<CaCase>())
      }

      @Test
      fun `should filter out cases with no CRD`() {
        whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
          PageImpl(
            listOf(
              aPrisonerSearchPrisoner.copy(
                conditionalReleaseDate = null,
              ),
            ),
          ),
        )

        whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(emptyList())

        val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEqualTo(emptyList<CaCase>())
      }

      @Test
      fun `should filter out cases that are on an ineligible EDS`() {
        whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
          PageImpl(
            listOf(
              aPrisonerSearchPrisoner.copy(
                conditionalReleaseDate = twoMonthsFromNow,
                confirmedReleaseDate = twoDaysFromNow,
                status = "ACTIVE IN",
                legalStatus = "SENTENCED",
                actualParoleDate = twoDaysFromNow,
              ),
            ),
          ),
        )

        whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(emptyList())

        val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEqualTo(emptyList<CaCase>())
      }

      @Test
      fun `should filter out cases with an approved HDC licence and HDCED`() {
        whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(
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
        whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
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
        whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(
          emptyList(),
        )
        whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(
          true,
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
        whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
          PageImpl(listOf(prisoner)),
        )

        whenever(hdcService.getHdcStatus(listOf(prisoner))).thenReturn(HdcStatuses(emptySet()))

        whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(mapOf("A1234AA" to twoDaysFromNow))

        val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEqualTo(
          listOf(
            TestData.caCase().copy(
              kind = null,
              licenceId = null,
              name = "Phil Cena",
              prisonerNumber = "A1234AA",
              releaseDate = twoDaysFromNow,
              licenceStatus = LicenceStatus.NOT_STARTED,
              probationPractitioner = ProbationPractitioner(
                staffCode = "X1234",
                name = "Joe Bloggs",
                staffIdentifier = null,
                staffUsername = null,
              ),
              isDueForEarlyRelease = false,
              lastWorkedOnBy = null,
            ),
          ),
        )
      }

      @Test
      fun `should filter out cases with no deliusRecord`() {
        whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(
          emptyList(),
        )
        whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(
          true,
        )
        whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
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
            forename = "Smith",
            surname = "Cena",
            comUsername = "Andy",
            licenceStartDate = tenDaysFromNow,
            actualReleaseDate = tenDaysFromNow,
          ),
          aLicenceSummary.copy(
            licenceId = 3,
            licenceStatus = LicenceStatus.IN_PROGRESS,
            nomisId = "A1234AC",
            forename = "Andy",
            surname = "Smith",
            comUsername = "Andy",
            licenceStartDate = twoMonthsFromNow,
            actualReleaseDate = twoMonthsFromNow,
          ),
          aLicenceSummary.copy(
            licenceId = 4,
            licenceStatus = LicenceStatus.IN_PROGRESS,
            nomisId = "A1234AD",
            forename = "John",
            surname = "Smith",
            comUsername = "Andy",
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
            name = "Andy Smith",
            prisonerNumber = "A1234AC",
            releaseDate = twoMonthsFromNow,
            tabType = null,
            nomisLegalStatus = null,
            probationPractitioner = ProbationPractitioner(
              staffCode = null,
              name = null,
              staffIdentifier = null,
              staffUsername = "Andy",
            ),
            lastWorkedOnBy = "X Y",
          ),
          TestData.caCase().copy(
            licenceId = 2,
            name = "Smith Cena",
            prisonerNumber = "A1234AB",
            releaseDate = tenDaysFromNow,
            tabType = null,
            nomisLegalStatus = null,
            probationPractitioner = ProbationPractitioner(
              staffCode = null,
              name = null,
              staffIdentifier = null,
              staffUsername = "Andy",
            ),
            lastWorkedOnBy = "X Y",
          ),
          TestData.caCase().copy(
            licenceId = 1,
            name = "John Cena",
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
          ),
          TestData.caCase().copy(
            licenceId = 4,
            prisonerNumber = "A1234AD",
            name = "John Smith",
            releaseDate = oneDayFromNow,
            tabType = null,
            nomisLegalStatus = null,
            probationPractitioner = ProbationPractitioner(
              staffCode = null,
              name = null,
              staffIdentifier = null,
              staffUsername = "Andy",
            ),
            lastWorkedOnBy = "X Y",
          ),
        ),
      )
    }
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
          name = "John Cena",
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
        ),
      ),
    )
  }

  private companion object {
    private fun createClock(timestamp: String) = Clock.fixed(Instant.parse(timestamp), ZoneId.systemDefault())

    val dateTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 13, 39))
    val instant = dateTime.atZone(ZoneId.systemDefault()).toInstant()
    val clock: Clock = createClock(instant.toString())

    val oneDayFromNow = LocalDate.now(clock).plusDays(1)
    val twoDaysFromNow = LocalDate.now(clock).plusDays(2)
    val tenDaysFromNow = LocalDate.now(clock).plusDays(10)
    val twoMonthsFromNow = LocalDate.now(clock).plusMonths(2)
    val fiveDaysFromNow = LocalDate.now(clock).plusDays(5)

    val aLicenceSummary = LicenceSummary(
      kind = LicenceKind.CRD,
      licenceId = 1,
      licenceType = LicenceType.AP,
      licenceStatus = LicenceStatus.IN_PROGRESS,
      nomisId = "A1234AA",
      forename = "John",
      surname = "Cena",
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
      approvedByName = "jim smith",
      approvedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
      licenceVersion = "1.0",
      isReviewNeeded = false,
      isDueForEarlyRelease = false,
      isInHardStopPeriod = false,
      isDueToBeReleasedInTheNextTwoWorkingDays = false,
      updatedByFullName = "X Y",
    )

    val probationCase = ProbationCase(crn = "X12347", nomisId = "A1234AA")

    val aProbationPractitioner = ProbationPractitioner(staffCode = "DEF456")
    val comUser = User(
      id = 2000L,
      username = "com-user",
      email = "comuser@probation.gov.uk",
      name = Name(
        forename = "com",
        surname = "user",
      ),
      teams = null,
      code = "AB00001",
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
      firstName = "PHIL",
      middleNames = null,
      lastName = "CENA",
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
  }
}
