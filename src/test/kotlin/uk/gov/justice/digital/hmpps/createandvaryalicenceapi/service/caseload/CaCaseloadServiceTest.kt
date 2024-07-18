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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCaseLoad
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.GroupedByCom
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.*
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerHdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OffenderDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OtherIds
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffHuman
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
  private val probationSearchApiClient = mock<ProbationSearchApiClient>()
  private val licenceService = mock<LicenceService>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val communityApiClient = mock<CommunityApiClient>()
  private val eligibilityService = mock<EligibilityService>()

  private val service = CaCaseloadService(
    caseloadService,
    probationSearchApiClient,
    licenceService,
    prisonApiClient,
    eligibilityService,
    clock,
    communityApiClient,
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
    sortBy = "conditionalReleaseDate",
  )

  @BeforeEach
  fun reset() {
    reset(caseloadService, probationSearchApiClient, licenceService, prisonApiClient, communityApiClient)
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
          prisoner = PrisonerSearchPrisoner(
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
    whenever(caseloadService.getPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
      PageImpl(
        listOf(
          TestData.caseLoadItem(),
          TestData.caseLoadItem().copy(
            prisoner = PrisonerSearchPrisoner(
              prisonerNumber = "A1234AB",
              firstName = "Smith",
              lastName = "Cena",
              legalStatus = "SENTENCED",
              dateOfBirth = LocalDate.of(1985, 12, 28),
              mostSeriousOffence = "Robbery",
            ),
          ),
        ),
      ),
    )
    whenever(communityApiClient.getStaffDetailsByUsername(any())).thenReturn(listOf(comUser))
    whenever(probationSearchApiClient.searchForPeopleByNomsNumber(any())).thenReturn(listOf(offenderDetail))
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
        whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(
          true,
        )
        whenever(caseloadService.getPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
          PageImpl(
            listOf(
              TestData.caseLoadItem().copy(
                prisoner = TestData.caseLoadItem().prisoner.copy(
                  prisonerNumber = aLicenceSummary.nomisId,
                  confirmedReleaseDate = twoMonthsFromNow,
                  conditionalReleaseDate = twoDaysFromNow,
                ),
              ),
            ),
          ),
        )
        val prisonOmuCaseload = service.getPrisonOmuCaseload(listOf("BAI"), "")

        assertThat(prisonOmuCaseload.cases).hasSize(1)

        with(prisonOmuCaseload.cases.first()) {
          assertThat(licenceStatus).isEqualTo(LicenceStatus.TIMED_OUT)
          assertThat(isInHardStopPeriod).isTrue()
        }

        verify(licenceService, times(1)).findLicencesMatchingCriteria(licenceQueryObject)
        verify(caseloadService, times(0)).getPrisonersByNumber(listOf(aLicenceSummary.nomisId))
        verify(caseloadService, times(1)).getPrisonersByReleaseDate(any(), any(), any(), anyOrNull())
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

    @Nested
    inner class `apply Search` {
      @Test
      fun `should successfully search by name`() {
        assertThat(service.getPrisonOmuCaseload(listOf("BAI"), "Smith")).isEqualTo(
          CaCaseLoad(
            cases = listOf(
              TestData.caCase().copy(
                licenceId = 2,
                prisonerNumber = "A1234AB",
                name = "Smith Cena",
                nomisLegalStatus = "SENTENCED",
                lastWorkedOnBy = "X Y",
                probationPractitioner = ProbationPractitioner(staffUsername = "Andy"),
              ),
            ),
            showAttentionNeededTab = false,
          ),
        )
      }

      @Test
      fun `should successfully search by prison number`() {
        assertThat(service.getPrisonOmuCaseload(listOf("BAI"), "A1234AA")).isEqualTo(
          CaCaseLoad(
            cases = listOf(
              TestData.caCase().copy(
                name = "John Cena",
                lastWorkedOnBy = "X Y",
                probationPractitioner = ProbationPractitioner(staffCode = "AB00001", name = "com user"),
              ),
            ),
            showAttentionNeededTab = false,
          ),
        )
      }

      @Test
      fun `should successfully search by probation practitioner`() {
        whenever(probationSearchApiClient.searchForPeopleByNomsNumber(any())).thenReturn(
          listOf(offenderDetail),
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
            ),
          ),
        )
        assertThat(service.getPrisonOmuCaseload(listOf("BAI"), "com")).isEqualTo(
          CaCaseLoad(
            cases = listOf(
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
            showAttentionNeededTab = false,
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
            prisoner = PrisonerSearchPrisoner(
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

      whenever(caseloadService.getPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
        PageImpl(
          listOf(
            TestData.caseLoadItem().copy(
              prisoner = PrisonerSearchPrisoner(
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
        ),
      )
      val prisonOmuCaseload = service.getPrisonOmuCaseload(listOf("BAI"), "")
      assertThat(prisonOmuCaseload).isEqualTo(
        CaCaseLoad(
          cases = emptyList(),
          showAttentionNeededTab = false,
        ),
      )
    }

    @Test
    fun `should query for cases being released within 4 weeks`() {
      service.getPrisonOmuCaseload(listOf("BAI"), "Smith")
      verify(caseloadService, times(1)).getPrisonersByReleaseDate(
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
            conditionalReleaseDate = twoMonthsFromNow,
            actualReleaseDate = twoDaysFromNow,
            isDueForEarlyRelease = true,
          ),
        ),
      )

      whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
        listOf(
          TestData.caseLoadItem().copy(
            prisoner = PrisonerSearchPrisoner(
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

      whenever(caseloadService.getPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
        PageImpl(
          listOf(
            TestData.caseLoadItem().copy(
              prisoner = PrisonerSearchPrisoner(
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
        ),
      )
      val prisonOmuCaseload = service.getPrisonOmuCaseload(listOf("BAI"), "")
      assertThat(prisonOmuCaseload).isEqualTo(
        CaCaseLoad(
          cases = listOf(
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
              tabType = CaViewCasesTab.RELEASES_IN_NEXT_TWO_WORKING_DAYS,
              nomisLegalStatus = "SENTENCED",
              lastWorkedOnBy = "X Y",
              isDueForEarlyRelease = true,
              isInHardStopPeriod = false,
            ),
          ),
          showAttentionNeededTab = false,
        ),
      )
    }

    @Test
    fun `should return showAttentionNeededTab false along with caseload if there are no attention needed licences`() {
      val prisonOmuCaseload = service.getPrisonOmuCaseload(listOf("BAI"), "")
      assertThat(prisonOmuCaseload).isEqualTo(
        CaCaseLoad(
          cases = listOf(
            TestData.caCase().copy(
              name = "John Cena",
              probationPractitioner = ProbationPractitioner(
                staffCode = "AB00001",
                name = "com user",
                staffIdentifier = null,
                staffUsername = null,
              ),
              lastWorkedOnBy = "X Y",
            ),
            TestData.caCase().copy(
              licenceId = 2,
              name = "Smith Cena",
              prisonerNumber = "A1234AB",
              probationPractitioner = ProbationPractitioner(
                staffCode = null,
                name = null,
                staffIdentifier = null,
                staffUsername = "Andy",
              ),
              lastWorkedOnBy = "X Y",
            ),
          ),
          showAttentionNeededTab = false,
        ),
      )
    }

    @Test
    fun `should return showAttentionNeededTab true along with caseload if there are attention needed licences`() {
      whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(
        listOf(
          aLicenceSummary.copy(
            forename = "Steve",
            surname = "Cena",
            nomisId = "AB1234E",
            licenceId = 2,
            licenceType = LicenceType.AP_PSS,
            licenceStatus = LicenceStatus.APPROVED,
            isInHardStopPeriod = false,
            isDueToBeReleasedInTheNextTwoWorkingDays = false,
            isDueForEarlyRelease = false,
          ),
          aLicenceSummary.copy(
            forename = "Dave",
            nomisId = "AB1234G",
            licenceId = 3,
            licenceType = LicenceType.AP_PSS,
            licenceStatus = LicenceStatus.SUBMITTED,
            isInHardStopPeriod = false,
            isDueToBeReleasedInTheNextTwoWorkingDays = false,
            conditionalReleaseDate = twoMonthsFromNow,
          ),
        ),
      )

      whenever(eligibilityService.isEligibleForCvl(aPrisonerSearchPrisoner)).thenReturn(
        true,
      )

      whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
        listOf(
          TestData.caseLoadItem().copy(
            prisoner = PrisonerSearchPrisoner(
              firstName = "Steve",
              lastName = "Cena",
              prisonerNumber = "AB1234E",
              status = "ACTIVE IN",
              legalStatus = "IMMIGRATION_DETAINEE",
              dateOfBirth = LocalDate.of(1985, 12, 28),
              mostSeriousOffence = "Robbery",
            ),
            cvl = CvlFields(
              licenceType = LicenceType.AP,
              isDueForEarlyRelease = false,
              isInHardStopPeriod = false,
              isDueToBeReleasedInTheNextTwoWorkingDays = false,
            ),
          ),
          TestData.caseLoadItem().copy(
            prisoner = PrisonerSearchPrisoner(
              firstName = "Dave",
              lastName = "Cena",
              prisonerNumber = "AB1234G",
              conditionalReleaseDate = twoMonthsFromNow,
              status = "ACTIVE IN",
              legalStatus = "SENTENCED",
              dateOfBirth = LocalDate.of(1985, 12, 28),
              mostSeriousOffence = "Robbery",
            ),
            cvl = CvlFields(
              licenceType = LicenceType.AP,
              isDueForEarlyRelease = false,
              isInHardStopPeriod = false,
              isDueToBeReleasedInTheNextTwoWorkingDays = false,
            ),
          ),
        ),
      )

      whenever(caseloadService.getPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
        PageImpl(
          listOf(
            TestData.caseLoadItem().copy(
              prisoner = PrisonerSearchPrisoner(
                firstName = "John",
                lastName = "Cena",
                prisonerNumber = "AB1234D",
                conditionalReleaseDate = null,
                status = "ACTIVE IN",
                legalStatus = "IMMIGRATION_DETAINEE",
                dateOfBirth = LocalDate.of(1985, 12, 28),
                mostSeriousOffence = "Robbery",
              ),
              cvl = CvlFields(
                licenceType = LicenceType.AP,
                isDueForEarlyRelease = false,
                isInHardStopPeriod = false,
                isDueToBeReleasedInTheNextTwoWorkingDays = false,
              ),
            ),
            TestData.caseLoadItem().copy(
              prisoner = PrisonerSearchPrisoner(
                firstName = "Steve",
                lastName = "Cena",
                prisonerNumber = "AB1234E",
                status = "ACTIVE IN",
                legalStatus = "IMMIGRATION_DETAINEE",
                dateOfBirth = LocalDate.of(1985, 12, 28),
                mostSeriousOffence = "Robbery",
              ),
              cvl = CvlFields(
                licenceType = LicenceType.AP,
                isDueForEarlyRelease = false,
                isInHardStopPeriod = false,
                isDueToBeReleasedInTheNextTwoWorkingDays = false,
              ),
            ),
            TestData.caseLoadItem().copy(
              prisoner = PrisonerSearchPrisoner(
                firstName = "Phil",
                lastName = "Cena",
                prisonerNumber = "AB1234F",
                conditionalReleaseDate = tenDaysFromNow,
                status = "ACTIVE IN",
                legalStatus = "SENTENCED",
                dateOfBirth = LocalDate.of(1985, 12, 28),
                mostSeriousOffence = "Robbery",
              ),
              cvl = CvlFields(
                licenceType = LicenceType.AP,
                isDueForEarlyRelease = false,
                isInHardStopPeriod = false,
                isDueToBeReleasedInTheNextTwoWorkingDays = false,
              ),
            ),
          ),
        ),
      )
      val prisonOmuCaseload = service.getPrisonOmuCaseload(listOf("BAI"), "")
      assertThat(prisonOmuCaseload).isEqualTo(
        CaCaseLoad(
          cases = listOf(
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
              licenceStatus = LicenceStatus.APPROVED,
              tabType = CaViewCasesTab.ATTENTION_NEEDED,
              nomisLegalStatus = "IMMIGRATION_DETAINEE",
              lastWorkedOnBy = "X Y",
            ),
            TestData.caCase().copy(
              licenceId = 3,
              name = "Dave Cena",
              prisonerNumber = "AB1234G",
              probationPractitioner = ProbationPractitioner(
                staffCode = "AB00001",
                name = "com user",
                staffIdentifier = null,
                staffUsername = null,
              ),
              licenceStatus = LicenceStatus.SUBMITTED,
              tabType = CaViewCasesTab.FUTURE_RELEASES,
              lastWorkedOnBy = "X Y",
            ),
            TestData.caCase().copy(
              kind = null,
              licenceId = null,
              name = "Phil Cena",
              prisonerNumber = "AB1234F",
              probationPractitioner = ProbationPractitioner(
                staffCode = null,
                name = null,
                staffIdentifier = null,
                staffUsername = null,
              ),
              releaseDateLabel = "CRD",
              releaseDate = tenDaysFromNow,
              licenceStatus = LicenceStatus.NOT_STARTED,
              tabType = CaViewCasesTab.FUTURE_RELEASES,
              lastWorkedOnBy = null,
            ),
          ),
          showAttentionNeededTab = true,
        ),
      )
    }

    @Test
    fun `should return sorted results in ascending order`() {
      whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(
        listOf(
          aLicenceSummary.copy(
            conditionalReleaseDate = twoDaysFromNow,
            actualReleaseDate = tenDaysFromNow,
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
          ),
        ),
      )
      whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
        listOf(
          TestData.caseLoadItem(),
          TestData.caseLoadItem().copy(
            prisoner = PrisonerSearchPrisoner(
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
            prisoner = PrisonerSearchPrisoner(
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
      val prisonOmuCaseload = service.getPrisonOmuCaseload(listOf("BAI"), "")
      assertThat(prisonOmuCaseload).isEqualTo(
        CaCaseLoad(
          cases = listOf(
            TestData.caCase().copy(
              licenceId = 2,
              name = "Smith Cena",
              prisonerNumber = "A1234AB",
              releaseDate = twoDaysFromNow,
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
              probationPractitioner = ProbationPractitioner(
                staffCode = "AB00001",
                name = "com user",
                staffIdentifier = null,
                staffUsername = null,
              ),
              lastWorkedOnBy = "X Y",
            ),
          ),
          showAttentionNeededTab = false,
        ),
      )
    }

    @Nested
    inner class `filtering rules` {
      @Test
      fun `should filter out cases with a future PED`() {
        whenever(caseloadService.getPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
          PageImpl(
            listOf(
              TestData.caseLoadItem().copy(
                TestData.caseLoadItem().prisoner.copy(
                  paroleEligibilityDate = twoDaysFromNow,
                ),
              ),
              TestData.caseLoadItem().copy(
                prisoner = PrisonerSearchPrisoner(
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
          ),
        )

        whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(emptyList())

        val prisonOmuCaseload = service.getPrisonOmuCaseload(listOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEqualTo(
          CaCaseLoad(
            cases = emptyList(),
            showAttentionNeededTab = false,
          ),
        )
      }

      @Test
      fun `Should filter out cases with a legal status of DEAD`() {
        whenever(caseloadService.getPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
          PageImpl(
            listOf(
              TestData.caseLoadItem().copy(
                TestData.caseLoadItem().prisoner.copy(
                  legalStatus = "DEAD",
                ),
              ),
            ),
          ),
        )

        whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(emptyList())

        val prisonOmuCaseload = service.getPrisonOmuCaseload(listOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEqualTo(
          CaCaseLoad(
            cases = emptyList(),
            showAttentionNeededTab = false,
          ),
        )
      }

      @Test
      fun `should filter out cases on an indeterminate sentence`() {
        whenever(caseloadService.getPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
          PageImpl(
            listOf(
              TestData.caseLoadItem().copy(
                TestData.caseLoadItem().prisoner.copy(
                  indeterminateSentence = true,
                ),
                cvl = CvlFields(
                  licenceType = LicenceType.AP,
                  isDueForEarlyRelease = true,
                  isInHardStopPeriod = false,
                  isDueToBeReleasedInTheNextTwoWorkingDays = false,
                ),
              ),
            ),
          ),
        )

        whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(emptyList())

        val prisonOmuCaseload = service.getPrisonOmuCaseload(listOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEqualTo(
          CaCaseLoad(
            cases = emptyList(),
            showAttentionNeededTab = false,
          ),
        )
      }

      @Test
      fun `should filter out cases with no CRD`() {
        whenever(caseloadService.getPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
          PageImpl(
            listOf(
              TestData.caseLoadItem().copy(
                TestData.caseLoadItem().prisoner.copy(
                  conditionalReleaseDate = null,
                ),
                cvl = CvlFields(
                  licenceType = LicenceType.AP,
                  isDueForEarlyRelease = true,
                  isInHardStopPeriod = false,
                  isDueToBeReleasedInTheNextTwoWorkingDays = false,
                ),
              ),
            ),
          ),
        )

        whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(emptyList())

        val prisonOmuCaseload = service.getPrisonOmuCaseload(listOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEqualTo(
          CaCaseLoad(
            cases = emptyList(),
            showAttentionNeededTab = false,
          ),
        )
      }

      @Test
      fun `should filter out cases that are on an ineligible EDS`() {
        whenever(caseloadService.getPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
          PageImpl(
            listOf(
              TestData.caseLoadItem().copy(
                TestData.caseLoadItem().prisoner.copy(
                  conditionalReleaseDate = twoMonthsFromNow,
                  confirmedReleaseDate = twoDaysFromNow,
                  status = "ACTIVE IN",
                  legalStatus = "SENTENCED",
                  actualParoleDate = twoDaysFromNow,
                ),
                cvl = CvlFields(
                  licenceType = LicenceType.AP,
                  isDueForEarlyRelease = true,
                  isInHardStopPeriod = false,
                  isDueToBeReleasedInTheNextTwoWorkingDays = false,
                ),
              ),
            ),
          ),
        )

        whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(emptyList())

        val prisonOmuCaseload = service.getPrisonOmuCaseload(listOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEqualTo(
          CaCaseLoad(
            cases = emptyList(),
            showAttentionNeededTab = false,
          ),
        )
      }

      @Test
      fun `should filter out cases with an approved HDC licence and HDCED`() {
        whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(
          emptyList(),
        )
        whenever(caseloadService.getPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
          PageImpl(
            listOf(
              TestData.caseLoadItem().copy(
                TestData.caseLoadItem().prisoner.copy(
                  prisonerNumber = "A1234AC",
                  actualParoleDate = null,
                  conditionalReleaseDate = twoMonthsFromNow,
                  confirmedReleaseDate = twoDaysFromNow,
                  status = "ACTIVE IN",
                  legalStatus = "SENTENCED",
                  homeDetentionCurfewEligibilityDate = twoDaysFromNow,
                  bookingId = "1234",
                ),
                cvl = CvlFields(
                  licenceType = LicenceType.AP,
                  isDueForEarlyRelease = true,
                  isInHardStopPeriod = false,
                  isDueToBeReleasedInTheNextTwoWorkingDays = false,
                ),
              ),
            ),
          ),
        )

        whenever(prisonApiClient.getHdcStatuses(listOf(1234))).thenReturn(
          listOf(
            PrisonerHdcStatus(
              bookingId = 1234,
              passed = true,
              approvalStatus = "APPROVED",
            ),
          ),
        )

        val prisonOmuCaseload = service.getPrisonOmuCaseload(listOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEqualTo(
          CaCaseLoad(
            cases = emptyList(),
            showAttentionNeededTab = false,
          ),
        )
      }

      @Test
      fun `should not filter out cases with an unapproved HDC licence`() {
        whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(
          emptyList(),
        )
        whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(
          true,
        )
        whenever(caseloadService.getPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
          PageImpl(
            listOf(
              TestData.caseLoadItem().copy(
                TestData.caseLoadItem().prisoner.copy(
                  conditionalReleaseDate = fiveDaysFromNow,
                  confirmedReleaseDate = twoDaysFromNow,
                  status = "ACTIVE IN",
                  legalStatus = "SENTENCED",
                  homeDetentionCurfewEligibilityDate = twoDaysFromNow,
                  bookingId = "1234",
                ),
                cvl = CvlFields(
                  licenceType = LicenceType.AP,
                  isDueForEarlyRelease = true,
                  isInHardStopPeriod = false,
                  isDueToBeReleasedInTheNextTwoWorkingDays = false,
                ),
              ),
            ),
          ),
        )

        whenever(prisonApiClient.getHdcStatuses(listOf(1234))).thenReturn(
          listOf(
            PrisonerHdcStatus(
              bookingId = 1234,
              passed = true,
              approvalStatus = "REJECTED",
            ),
          ),
        )

        val prisonOmuCaseload = service.getPrisonOmuCaseload(listOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEqualTo(
          CaCaseLoad(
            cases = listOf(
              TestData.caCase().copy(
                kind = null,
                licenceId = null,
                name = "Bob Mortimar",
                prisonerNumber = "A1234AA",
                releaseDate = twoDaysFromNow,
                licenceStatus = LicenceStatus.NOT_STARTED,
                probationPractitioner = ProbationPractitioner(
                  staffCode = "X1234",
                  name = "Joe Bloggs",
                  staffIdentifier = null,
                  staffUsername = null,
                ),
                isDueForEarlyRelease = true,
                lastWorkedOnBy = null,
              ),
            ),
            showAttentionNeededTab = false,
          ),
        )
      }

      @Test
      fun `should not filter out cases with an approved HDC licence but no HDCED`() {
        whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(
          emptyList(),
        )
        whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(
          true,
        )
        whenever(caseloadService.getPrisonersByReleaseDate(any(), any(), any(), anyOrNull())).thenReturn(
          PageImpl(
            listOf(
              TestData.caseLoadItem().copy(
                TestData.caseLoadItem().prisoner.copy(
                  conditionalReleaseDate = fiveDaysFromNow,
                  confirmedReleaseDate = twoDaysFromNow,
                  status = "ACTIVE IN",
                  legalStatus = "SENTENCED",
                  homeDetentionCurfewEligibilityDate = null,
                  bookingId = "1234",
                ),
                cvl = CvlFields(
                  licenceType = LicenceType.AP,
                  isDueForEarlyRelease = true,
                  isInHardStopPeriod = false,
                  isDueToBeReleasedInTheNextTwoWorkingDays = false,
                ),
              ),
            ),
          ),
        )

        whenever(prisonApiClient.getHdcStatuses(listOf(1234))).thenReturn(
          listOf(
            PrisonerHdcStatus(
              bookingId = 1234,
              passed = true,
              approvalStatus = "APPROVED",
            ),
          ),
        )

        val prisonOmuCaseload = service.getPrisonOmuCaseload(listOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEqualTo(
          CaCaseLoad(
            cases = listOf(
              TestData.caCase().copy(
                kind = null,
                licenceId = null,
                name = "Bob Mortimar",
                prisonerNumber = "A1234AA",
                releaseDate = twoDaysFromNow,
                licenceStatus = LicenceStatus.NOT_STARTED,
                probationPractitioner = ProbationPractitioner(
                  staffCode = "X1234",
                  name = "Joe Bloggs",
                  staffIdentifier = null,
                  staffUsername = null,
                ),
                isDueForEarlyRelease = true,
                lastWorkedOnBy = null,
              ),
            ),
            showAttentionNeededTab = false,
          ),
        )
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
            actualReleaseDate = twoDaysFromNow,
          ),
          aLicenceSummary.copy(
            licenceId = 2,
            licenceStatus = LicenceStatus.IN_PROGRESS,
            nomisId = "A1234AB",
            forename = "Smith",
            surname = "Cena",
            comUsername = "Andy",
            actualReleaseDate = tenDaysFromNow,
          ),
          aLicenceSummary.copy(
            licenceId = 3,
            licenceStatus = LicenceStatus.IN_PROGRESS,
            nomisId = "A1234AC",
            forename = "Andy",
            surname = "Smith",
            comUsername = "Andy",
            actualReleaseDate = twoMonthsFromNow,
          ),
          aLicenceSummary.copy(
            licenceId = 4,
            licenceStatus = LicenceStatus.IN_PROGRESS,
            nomisId = "A1234AD",
            forename = "John",
            surname = "Smith",
            comUsername = "Andy",
            actualReleaseDate = oneDayFromNow,
          ),
        ),
      )

      val probationOmuCaseload = service.getProbationOmuCaseload(listOf("BAI"), "")
      assertThat(probationOmuCaseload).isEqualTo(
        CaCaseLoad(
          cases = listOf(
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
          showAttentionNeededTab = false,
        ),
      )
    }
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

    val offenderDetail = OffenderDetail(
      offenderId = 1L,
      otherIds = OtherIds(crn = "X12347", nomsNumber = "A1234AA"),
      offenderManagers = listOf(
        OffenderManager(
          active = true,
          staffDetail = StaffDetail(
            forenames = "Joe",
            surname = "Bloggs",
            code = "X1234",
          ),
        ),
      ),
    )

    val aProbationPractitioner = ProbationPractitioner(staffCode = "DEF456")
    val comUser = User(
      staffIdentifier = 2000,
      username = "com-user",
      email = "comuser@probation.gov.uk",
      staff = StaffHuman(
        forenames = "com",
        surname = "user",
      ),
      teams = emptyList(),
      staffCode = "AB00001",
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
      conditionalReleaseDate = LocalDate.of(2024, 7, 28),
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
      firstName = "Phil",
      middleNames = null,
      lastName = "Cena",
      dateOfBirth = LocalDate.of(1985, 12, 28),
      conditionalReleaseDateOverrideDate = null,
      sentenceStartDate = null,
      sentenceExpiryDate = null,
      topupSupervisionStartDate = null,
      croNumber = null,
    )
  }
}
