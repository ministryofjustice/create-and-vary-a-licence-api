package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonCaseAdminSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.PrisonUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCaseRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadType.CaPrisonCaseload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadType.CaProbationCaseload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecordService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService.HdcStatuses
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TelemetryService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aCvlRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.caCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ReleaseDateLabelFactory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison.CaPrisonCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison.ExistingCasesCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison.NotStartedCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.probation.CaProbationCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManagerWithoutUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Detail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.TeamDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.StaffNameResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.workingDays.WorkingDaysService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class CaCaseloadServiceTest {
  private val licenceService = mock<LicenceService>()
  private val hdcService = mock<HdcService>()
  private val deliusApiClient = mock<DeliusApiClient>()
  private val eligibilityService = mock<EligibilityService>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val workingDaysService = mock<WorkingDaysService>()
  private val releaseDateLabelFactory = ReleaseDateLabelFactory(workingDaysService)
  private val cvlRecordService = mock<CvlRecordService>()
  private val licenceCaseRepository = mock<LicenceCaseRepository>()
  private val telemetryService = Mockito.mock<TelemetryService>()

  private val service = CaCaseloadService(
    prisonCaseloadService = CaPrisonCaseloadService(
      licenceCaseRepository = licenceCaseRepository,
      existingCasesCaseloadService = ExistingCasesCaseloadService(
        prisonerSearchApiClient,
        deliusApiClient,
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
      telemetryService,
    ),
    probationCaseloadService = CaProbationCaseloadService(
      licenceCaseRepository,
      releaseDateService,
      deliusApiClient,
      releaseDateLabelFactory,
      telemetryService,
    ),

  )

  private object PrisonQuery {
    val statusCodes = listOf(
      LicenceStatus.APPROVED,
      SUBMITTED,
      LicenceStatus.IN_PROGRESS,
      LicenceStatus.TIMED_OUT,
      LicenceStatus.ACTIVE,
    )
    val prisonCodes = listOf("BAI")
  }

  private object ProbationQuery {
    val statusCodes = listOf(
      LicenceStatus.ACTIVE,
      LicenceStatus.VARIATION_APPROVED,
      LicenceStatus.VARIATION_IN_PROGRESS,
      LicenceStatus.VARIATION_SUBMITTED,
    )
    val prisonCodes = listOf("BAI")
  }

  @BeforeEach
  fun reset() {
    reset(
      prisonerSearchApiClient,
      licenceService,
      hdcService,
      eligibilityService,
      deliusApiClient,
      prisonerSearchApiClient,
      releaseDateService,
      cvlRecordService,
      telemetryService,
    )

    // Given licences returned by the service
    whenever(licenceCaseRepository.findLicenceCases(PrisonQuery.statusCodes, PrisonQuery.prisonCodes)).thenReturn(
      listOf(
        createLicenceCase(),
        createLicenceCase(
          licenceId = 2,
          licenceStatus = LicenceStatus.IN_PROGRESS,
          nomisId = "A1234AB",
          forename = "Person",
          surname = "Two",
          comUsername = "tcom",
        ),
        createLicenceCase(
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

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(
        prisonerSearchResult(),
        prisonerSearchResult().copy(
          prisonerNumber = "A1234AB",
          firstName = "Person",
          lastName = "Two",
          legalStatus = "SENTENCED",
          dateOfBirth = LocalDate.of(1985, 12, 28),
          mostSeriousOffence = "Robbery",
        ),
        prisonerSearchResult().copy(
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

    whenever(deliusApiClient.getStaffDetailsByUsername(any())).thenReturn(
      listOf(
        comUser01,
        atcomUser02,
        tcomUser03,
      ),
    )
    whenever(deliusApiClient.getProbationCases(any(), anyOrNull())).thenReturn(listOf(probationCase))
    whenever(deliusApiClient.getOffenderManagersWithoutUser(any(), anyOrNull())).thenReturn(
      listOf(
        aCommunityManagerWithoutUser,
      ),
    )
  }

  @Nested
  inner class `Prison tab caseload` {

    @Nested
    inner class `in the hard stop period` {
      @Test
      fun `Sets NOT_STARTED licences to TIMED_OUT when in the hard stop period`() {
        whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull())).thenReturn(true)
        whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
          listOf(
            aPrisonerSearchPrisoner,
          ),
        )
        whenever(licenceCaseRepository.findLicenceCases(PrisonQuery.statusCodes, PrisonQuery.prisonCodes))
          .thenReturn(emptyList())

        val licenceCase = createLicenceCase(
          licenceId = 1,
          nomisId = "A1234AA",
          forename = "Person",
          surname = "Four",
          licenceStatus = LicenceStatus.NOT_STARTED,
        )

        whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(
          listOf(
            aCvlRecord(
              nomsId = licenceCase.prisonNumber!!,
              kind = LicenceKind.CRD,
              licenceStartDate = twoDaysFromNow,
              isInHardStopPeriod = true,
            ),
          ),
        )

        whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull()))
          .thenReturn(
            PageImpl(
              listOf(
                aPrisonerSearchPrisoner.copy(
                  bookingId = "1",
                  prisonerNumber = licenceCase.prisonNumber,
                  confirmedReleaseDate = twoMonthsFromNow,
                  conditionalReleaseDate = twoDaysFromNow,
                ),
              ),
            ),
          )

        whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

        // WHEN
        val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")

        // THEN
        assertThat(prisonOmuCaseload).hasSize(1)
        with(prisonOmuCaseload.first()) {
          assertThat(name).isEqualTo("Person Four")
          assertThat(licenceStatus).isEqualTo(LicenceStatus.TIMED_OUT)
          assertThat(isInHardStopPeriod).isTrue()
          assertThat(hardStopKind).isEqualTo(LicenceKind.HARD_STOP)
        }

        verify(licenceCaseRepository, times(1)).findLicenceCases(
          PrisonQuery.statusCodes,
          PrisonQuery.prisonCodes,
        )
        verify(prisonerSearchApiClient, times(0)).searchPrisonersByNomisIds(listOf(licenceCase.prisonNumber))
        verify(prisonerSearchApiClient, times(1)).searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull())
      }
    }

    @Nested
    inner class `apply Search` {

      @Test
      fun `should successfully search by name`() {
        val licenceCase = createLicenceCase(
          licenceId = 2,
          nomisId = "A1234AB",
          forename = "Person",
          surname = "Two",
        )

        assertThat(service.getPrisonOmuCaseload(setOf("BAI"), "Two")).isEqualTo(
          listOf(
            caCase().copy(
              licenceId = licenceCase.licenceId,
              prisonerNumber = licenceCase.prisonNumber!!,
              name = "Person Two",
              nomisLegalStatus = "SENTENCED",
              lastWorkedOnBy = "X Y",
              probationPractitioner = probationPractitionerFor(tcomUser03),
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
            caCase().copy(
              name = "Person One",
              lastWorkedOnBy = "X Y",
              probationPractitioner = probationPractitionerFor(comUser01),
              prisonCode = "BAI",
              prisonDescription = "Moorland (HMP)",
            ),
          ),
        )
      }

      @Test
      fun `should successfully search by probation practitioner`() {
        val licenceCase1 = createLicenceCase(
          licenceId = 1,
          nomisId = "A1234AA",
          forename = "Person",
          surname = "One",
        )
        val licenceCase2 = createLicenceCase(
          licenceId = 2,
          nomisId = "A1234AB",
          forename = "Person",
          surname = "Two",
          licenceStatus = LicenceStatus.IN_PROGRESS,
        )

        whenever(licenceCaseRepository.findLicenceCases(PrisonQuery.statusCodes, PrisonQuery.prisonCodes))
          .thenReturn(listOf(licenceCase1, licenceCase2))

        assertThat(service.getPrisonOmuCaseload(setOf("BAI"), "com")).isEqualTo(
          listOf(
            caCase().copy(
              name = "Person One",
              lastWorkedOnBy = "X Y",
              probationPractitioner = probationPractitionerFor(comUser01),
              prisonCode = "BAI",
              prisonDescription = "Moorland (HMP)",
            ),
            caCase().copy(
              licenceId = licenceCase2.licenceId,
              prisonerNumber = licenceCase2.prisonNumber!!,
              name = "Person Two",
              nomisLegalStatus = "SENTENCED",
              lastWorkedOnBy = "X Y",
              probationPractitioner = probationPractitionerFor(comUser01),
              prisonCode = "BAI",
              prisonDescription = "Moorland (HMP)",
            ),
          ),
        )
      }
    }

    @Test
    fun `should filter out cases with an existing ACTIVE licence`() {
      whenever(
        licenceCaseRepository.findLicenceCases(
          PrisonQuery.statusCodes,
          PrisonQuery.prisonCodes,
        ),
      ).thenReturn(
        listOf(
          createLicenceCase(
            forename = "Person",
            surname = "Three",
            nomisId = "AB1234E",
            licenceId = 2,
            licenceStatus = LicenceStatus.ACTIVE,
            conditionalReleaseDate = twoMonthsFromNow,
            actualReleaseDate = twoDaysFromNow,
          ),
        ),
      )

      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
        listOf(
          prisonerSearchResult().copy(
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
      whenever(
        licenceCaseRepository.findLicenceCases(
          PrisonQuery.statusCodes,
          PrisonQuery.prisonCodes,
        ),
      ).thenReturn(
        listOf(
          createLicenceCase(
            forename = "Person",
            surname = "Three",
            nomisId = "AB1234E",
            licenceId = 2,
            licenceStatus = LicenceStatus.IN_PROGRESS,
            licenceStartDate = twoDaysFromNow,
            conditionalReleaseDate = twoMonthsFromNow,
            actualReleaseDate = twoDaysFromNow,
          ),
        ),
      )

      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
        listOf(
          prisonerSearchResult().copy(
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

      whenever(releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(any())).thenReturn(true)

      val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")
      assertThat(prisonOmuCaseload).isEqualTo(
        listOf(
          caCase().copy(
            licenceId = 2,
            name = "Person Three",
            prisonerNumber = "AB1234E",
            probationPractitioner = probationPractitionerFor(comUser01),
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
      whenever(
        licenceCaseRepository.findLicenceCases(
          PrisonQuery.statusCodes,
          PrisonQuery.prisonCodes,
        ),
      ).thenReturn(
        listOf(
          createLicenceCase(
            conditionalReleaseDate = tenDaysFromNow,
            actualReleaseDate = twoMonthsFromNow,
            licenceStartDate = tenDaysFromNow,
          ),
          createLicenceCase(
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
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
        listOf(
          prisonerSearchResult(),
          prisonerSearchResult().copy(
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
          caCase().copy(
            licenceId = 2,
            name = "Person Two",
            prisonerNumber = "A1234AB",
            releaseDate = twoDaysFromNow,
            releaseDateLabel = "Confirmed release date",
            probationPractitioner = probationPractitionerFor(tcomUser03),
            lastWorkedOnBy = "X Y",
            prisonCode = "BAI",
            prisonDescription = "Moorland (HMP)",
          ),
          caCase().copy(
            name = "Person One",
            releaseDate = tenDaysFromNow,
            releaseDateLabel = "CRD",
            probationPractitioner = probationPractitionerFor(comUser01),
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
      val licenceCase = createLicenceCase(
        actualReleaseDate = twoDaysFromNow,
        licenceStartDate = twoDaysFromNow,
      )

      whenever(
        licenceCaseRepository.findLicenceCases(
          PrisonQuery.statusCodes,
          PrisonQuery.prisonCodes,
        ),
      ).thenReturn(listOf(licenceCase))
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisonerSearchResult()))

      // When
      val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")

      // Then
      assertThat(prisonOmuCaseload).hasSize(1)
      assertThat(prisonOmuCaseload[0].releaseDateLabel).isEqualTo("Confirmed release date")
    }

    @Test
    fun `should have correct releaseDateLabel when postRecallReleaseDate is the same as licenceStartDate`() {
      // Given
      val licenceCase = createLicenceCase(
        licenceStartDate = tenDaysFromNow,
        postRecallReleaseDate = tenDaysFromNow,
      )
      whenever(workingDaysService.getLastWorkingDay(licenceCase.postRecallReleaseDate)).thenReturn(licenceCase.postRecallReleaseDate)
      whenever(
        licenceCaseRepository.findLicenceCases(
          PrisonQuery.statusCodes,
          PrisonQuery.prisonCodes,
        ),
      ).thenReturn(listOf(licenceCase))
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisonerSearchResult()))

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
        whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
          listOf(
            aPrisonerSearchPrisoner,
          ),
        )

        whenever(licenceCaseRepository.findLicenceCases(PrisonQuery.statusCodes, PrisonQuery.prisonCodes))
          .thenReturn(emptyList())

        whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(
          listOf(
            aCvlRecord(
              nomsId = "A1234AA",
              kind = LicenceKind.CRD,
              licenceStartDate = twoDaysFromNow,
            ),
            aCvlRecord(
              nomsId = "A1234AB",
              kind = null,
              licenceStartDate = null,
            ).copy(isEligible = false),
          ),
        )

        whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull()))
          .thenReturn(
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
        whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
          listOf(
            aPrisonerSearchPrisoner.copy(
              prisonerNumber = "A1234AA",
              legalStatus = "DEAD",
            ),
          ),
        )

        val licenceCase = createLicenceCase(
          licenceId = 1,
          nomisId = "A1234AA",
          forename = "Person",
          surname = "One",
          licenceStatus = LicenceStatus.IN_PROGRESS,
        )

        whenever(licenceCaseRepository.findLicenceCases(PrisonQuery.statusCodes, PrisonQuery.prisonCodes))
          .thenReturn(listOf(licenceCase))

        whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(
          listOf(
            aCvlRecord(
              nomsId = "A1234AA",
              kind = LicenceKind.CRD,
              licenceStartDate = twoDaysFromNow,
            ),
          ),
        )

        val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEmpty()
      }

      @Test
      fun `should filter out cases with no CRD`() {
        whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull()))
          .thenReturn(PageImpl(listOf(aPrisonerSearchPrisoner.copy(conditionalReleaseDate = null))))

        whenever(licenceCaseRepository.findLicenceCases(PrisonQuery.statusCodes, PrisonQuery.prisonCodes))
          .thenReturn(emptyList())

        whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(
          listOf(
            aCvlRecord(
              nomsId = aPrisonerSearchPrisoner.prisonerNumber,
              kind = null,
              licenceStartDate = null,
            ),
          ),
        )

        val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEmpty()
      }

      @Test
      fun `should filter out cases with an approved HDC licence and HDCED`() {
        whenever(licenceCaseRepository.findLicenceCases(PrisonQuery.statusCodes, PrisonQuery.prisonCodes))
          .thenReturn(emptyList())

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

        whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull()))
          .thenReturn(PageImpl(listOf(prisoner)))

        whenever(hdcService.getHdcStatus(listOf(prisoner)))
          .thenReturn(HdcStatuses(setOf(prisoner.bookingId!!.toLong())))

        val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEmpty()
      }

      @Test
      fun `should not filter out cases with an unapproved HDC licence`() {
        whenever(licenceCaseRepository.findLicenceCases(PrisonQuery.statusCodes, PrisonQuery.prisonCodes))
          .thenReturn(emptyList())

        whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(
          listOf(aCvlRecord(kind = LicenceKind.CRD, licenceStartDate = twoDaysFromNow)),
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

        whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull()))
          .thenReturn(PageImpl(listOf(prisoner)))

        whenever(hdcService.getHdcStatus(listOf(prisoner)))
          .thenReturn(HdcStatuses(emptySet()))

        val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEqualTo(
          listOf(
            caCase().copy(
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
              hardStopKind = LicenceKind.HARD_STOP,
            ),
          ),
        )
      }

      @Test
      fun `should filter out cases with no deliusRecord`() {
        whenever(licenceCaseRepository.findLicenceCases(PrisonQuery.statusCodes, PrisonQuery.prisonCodes))
          .thenReturn(emptyList())

        whenever(cvlRecordService.getCvlRecords(any(), any())).thenReturn(
          listOf(aCvlRecord(kind = LicenceKind.CRD)),
        )

        whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull()))
          .thenReturn(
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
        whenever(deliusApiClient.getOffenderManagersWithoutUser(any(), anyOrNull())).thenReturn(emptyList())

        val prisonOmuCaseload = service.getPrisonOmuCaseload(setOf("BAI"), "")
        assertThat(prisonOmuCaseload).isEmpty()
      }
    }
  }

  @Nested
  inner class `Probation tab caseload` {
    @Test
    fun `should return sorted results in descending order`() {
      whenever(licenceCaseRepository.findLicenceCases(any(), any())).thenReturn(
        listOf(
          createLicenceCase(
            licenceId = 1,
            nomisId = "A1234AA",
            forename = "Person",
            surname = "One",
            licenceStatus = LicenceStatus.IN_PROGRESS,
            licenceStartDate = twoDaysFromNow,
            actualReleaseDate = twoDaysFromNow,
          ),
          createLicenceCase(
            licenceId = 2,
            nomisId = "A1234AB",
            forename = "Person",
            surname = "Two",
            licenceStatus = LicenceStatus.IN_PROGRESS,
            comUsername = "tcom",
            licenceStartDate = tenDaysFromNow,
            actualReleaseDate = tenDaysFromNow,
          ),
          createLicenceCase(
            licenceId = 3,
            nomisId = "A1234AC",
            forename = "Person",
            surname = "Six",
            licenceStatus = LicenceStatus.IN_PROGRESS,
            comUsername = "tcom",
            licenceStartDate = twoMonthsFromNow,
            actualReleaseDate = twoMonthsFromNow,
          ),
          createLicenceCase(
            licenceId = 4,
            nomisId = "A1234AD",
            forename = "Person",
            surname = "Five",
            licenceStatus = LicenceStatus.IN_PROGRESS,
            comUsername = "tcom",
            licenceStartDate = oneDayFromNow,
            actualReleaseDate = oneDayFromNow,
          ),
        ),
      )

      val probationOmuCaseload = service.getProbationOmuCaseload(setOf("BAI"), "")

      assertThat(probationOmuCaseload).isEqualTo(
        listOf(
          caCase().copy(
            licenceId = 3,
            name = "Person Six",
            prisonerNumber = "A1234AC",
            releaseDate = twoMonthsFromNow,
            tabType = null,
            nomisLegalStatus = null,
            probationPractitioner = probationPractitionerFor(tcomUser03),
            lastWorkedOnBy = "X Y",
            prisonCode = "BAI",
            prisonDescription = "Moorland (HMP)",
          ),
          caCase().copy(
            licenceId = 2,
            name = "Person Two",
            prisonerNumber = "A1234AB",
            releaseDate = tenDaysFromNow,
            tabType = null,
            nomisLegalStatus = null,
            probationPractitioner = probationPractitionerFor(tcomUser03),
            lastWorkedOnBy = "X Y",
            prisonCode = "BAI",
            prisonDescription = "Moorland (HMP)",
          ),
          caCase().copy(
            licenceId = 1,
            name = "Person One",
            prisonerNumber = "A1234AA",
            releaseDate = twoDaysFromNow,
            tabType = null,
            nomisLegalStatus = null,
            probationPractitioner = probationPractitionerFor(comUser01),
            lastWorkedOnBy = "X Y",
            prisonCode = "BAI",
            prisonDescription = "Moorland (HMP)",
          ),
          caCase().copy(
            licenceId = 4,
            prisonerNumber = "A1234AD",
            name = "Person Five",
            releaseDate = oneDayFromNow,
            tabType = null,
            nomisLegalStatus = null,
            probationPractitioner = probationPractitionerFor(tcomUser03),
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
      assertThat(
        service.searchForOffenderOnPrisonCaseAdminCaseload(
          aPrisonUserSearchRequest.copy(query = "One"),
        ),
      ).isEqualTo(
        PrisonCaseAdminSearchResult(
          inPrisonResults = listOf(
            caCase().copy(
              licenceId = 1,
              prisonerNumber = "A1234AA",
              name = "Person One",
              nomisLegalStatus = "SENTENCED",
              lastWorkedOnBy = "X Y",
              probationPractitioner = probationPractitionerFor(comUser01),
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
      assertThat(
        service.searchForOffenderOnPrisonCaseAdminCaseload(
          aPrisonUserSearchRequest.copy(query = "A1234AA"),
        ),
      ).isEqualTo(
        PrisonCaseAdminSearchResult(
          inPrisonResults = listOf(
            caCase().copy(
              licenceId = 1,
              prisonerNumber = "A1234AA",
              name = "Person One",
              nomisLegalStatus = "SENTENCED",
              lastWorkedOnBy = "X Y",
              probationPractitioner = probationPractitionerFor(comUser01),
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
    fun `telemetry is captured`() {
      val result = service.searchForOffenderOnPrisonCaseAdminCaseload(
        aPrisonUserSearchRequest.copy(query = "A1234AA"),
      )

      assertThat(result.inPrisonResults).hasSize(1)
      assertThat(result.onProbationResults).hasSize(0)

      // We record the number of results pulled back before filtering as we want to measure how many cases are being loaded in total
      argumentCaptor<List<CaCase>> {
        verify(telemetryService).recordCaseloadLoad(
          eq(CaPrisonCaseload),
          eq(aPrisonUserSearchRequest.prisonCaseloads),
          capture(),
        )

        assertThat(firstValue).hasSize(3)
      }
      argumentCaptor<List<CaCase>> {
        verify(telemetryService).recordCaseloadLoad(
          eq(CaProbationCaseload),
          eq(aPrisonUserSearchRequest.prisonCaseloads),
          capture(),
        )

        assertThat(firstValue).hasSize(0)
      }
    }

    @Test
    fun `should successfully search by probation practitioner name for offender in prison`() {
      assertThat(
        service.searchForOffenderOnPrisonCaseAdminCaseload(
          aPrisonUserSearchRequest.copy(query = "com"),
        ),
      ).isEqualTo(
        PrisonCaseAdminSearchResult(
          inPrisonResults = listOf(
            caCase().copy(
              licenceId = 1,
              prisonerNumber = "A1234AA",
              name = "Person One",
              nomisLegalStatus = "SENTENCED",
              lastWorkedOnBy = "X Y",
              probationPractitioner = probationPractitionerFor(comUser01),
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
      val licenceCases = listOf(
        createLicenceCase(
          licenceId = 1,
          licenceStatus = SUBMITTED,
          nomisId = "A1234AC",
          forename = "Last",
          licenceStartDate = LocalDate.now().minusDays(1),
        ),
        createLicenceCase(
          licenceId = 2,
          licenceStatus = SUBMITTED,
          nomisId = "A1234BC",
          forename = "Second",
          licenceStartDate = LocalDate.now(),
        ),
        createLicenceCase(
          licenceId = 5,
          licenceStatus = SUBMITTED,
          nomisId = "A1234BD",
          forename = "Forth",
          licenceStartDate = LocalDate.now(),
        ),
        createLicenceCase(
          licenceId = 3,
          licenceStatus = SUBMITTED,
          nomisId = "A1234CC",
          forename = "First",
          licenceStartDate = LocalDate.now().plusDays(1),
        ),
        createLicenceCase(
          licenceId = 4,
          licenceStatus = SUBMITTED,
          nomisId = "A1234DC",
          forename = "Third",
          licenceStartDate = LocalDate.now(),
        ),
      )

      whenever(
        licenceCaseRepository.findLicenceCases(
          PrisonQuery.statusCodes,
          PrisonQuery.prisonCodes,
        ),
      ).thenReturn(licenceCases)

      val prisoners = licenceCases.map {
        prisonerSearchResult().copy(
          prisonerNumber = it.prisonNumber!!,
          firstName = it.forename!!,
          dateOfBirth = LocalDate.of(1985, 12, 28),
        )
      }

      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(prisoners)

      val results = service.searchForOffenderOnPrisonCaseAdminCaseload(
        aPrisonUserSearchRequest.copy(query = "com"),
      )

      assertThat(results.inPrisonResults.map { it.licenceId }).isEqualTo(listOf(1L, 2L, 4L, 5L, 3L))
    }

    @Test
    fun `should return all results when query string is empty`() {
      whenever(
        licenceCaseRepository.findLicenceCases(
          ProbationQuery.statusCodes,
          ProbationQuery.prisonCodes,
        ),
      ).thenReturn(
        listOf(
          createLicenceCase(
            licenceId = 3,
            licenceStatus = LicenceStatus.ACTIVE,
            nomisId = "A1234AC",
            forename = "Person",
            surname = "Three",
          ),
        ),
      )

      assertThat(
        service.searchForOffenderOnPrisonCaseAdminCaseload(
          aPrisonUserSearchRequest.copy(query = ""),
        ),
      ).isEqualTo(
        PrisonCaseAdminSearchResult(
          inPrisonResults = listOf(
            caCase().copy(
              licenceId = 1,
              prisonerNumber = "A1234AA",
              name = "Person One",
              nomisLegalStatus = "SENTENCED",
              lastWorkedOnBy = "X Y",
              probationPractitioner = probationPractitionerFor(comUser01),
              prisonCode = "BAI",
              prisonDescription = "Moorland (HMP)",
            ),
            caCase().copy(
              licenceId = 2,
              prisonerNumber = "A1234AB",
              name = "Person Two",
              nomisLegalStatus = "SENTENCED",
              lastWorkedOnBy = "X Y",
              probationPractitioner = probationPractitionerFor(tcomUser03),
              prisonCode = "BAI",
              prisonDescription = "Moorland (HMP)",
            ),
          ),
          onProbationResults = listOf(
            caCase().copy(
              licenceId = 3,
              prisonerNumber = "A1234AC",
              licenceStatus = LicenceStatus.ACTIVE,
              name = "Person Three",
              nomisLegalStatus = null,
              tabType = null,
              lastWorkedOnBy = "X Y",
              probationPractitioner = probationPractitionerFor(comUser01),
              prisonCode = "BAI",
              prisonDescription = "Moorland (HMP)",
            ),
          ),
          attentionNeededResults = listOf(
            caCase().copy(
              licenceId = 3,
              prisonerNumber = "A1234AC",
              name = "Person Three",
              nomisLegalStatus = "SENTENCED",
              releaseDate = null,
              releaseDateLabel = "CRD",
              tabType = CaViewCasesTab.ATTENTION_NEEDED,
              lastWorkedOnBy = "X Y",
              probationPractitioner = probationPractitionerFor(atcomUser02),
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
      whenever(
        licenceCaseRepository.findLicenceCases(
          ProbationQuery.statusCodes,
          ProbationQuery.prisonCodes,
        ),
      ).thenReturn(
        listOf(
          createLicenceCase(
            licenceId = 4,
            licenceStatus = LicenceStatus.ACTIVE,
            nomisId = "A1234AD",
            forename = "Person",
            surname = "Four",
          ),
        ),
      )

      assertThat(
        service.searchForOffenderOnPrisonCaseAdminCaseload(
          aPrisonUserSearchRequest.copy(query = "Four"),
        ),
      ).isEqualTo(
        PrisonCaseAdminSearchResult(
          inPrisonResults = emptyList(),
          onProbationResults = listOf(
            caCase().copy(
              licenceId = 4,
              prisonerNumber = "A1234AD",
              licenceStatus = LicenceStatus.ACTIVE,
              name = "Person Four",
              nomisLegalStatus = null,
              tabType = null,
              lastWorkedOnBy = "X Y",
              probationPractitioner = probationPractitionerFor(comUser01),
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
      whenever(
        licenceCaseRepository.findLicenceCases(
          ProbationQuery.statusCodes,
          ProbationQuery.prisonCodes,
        ),
      ).thenReturn(
        listOf(
          createLicenceCase(
            licenceId = 4,
            licenceStatus = LicenceStatus.ACTIVE,
            nomisId = "A1234AD",
            forename = "Person",
            surname = "Four",
          ),
        ),
      )

      assertThat(
        service.searchForOffenderOnPrisonCaseAdminCaseload(
          aPrisonUserSearchRequest.copy(query = "A1234AD"),
        ),
      ).isEqualTo(
        PrisonCaseAdminSearchResult(
          inPrisonResults = emptyList(),
          onProbationResults = listOf(
            caCase().copy(
              licenceId = 4,
              prisonerNumber = "A1234AD",
              licenceStatus = LicenceStatus.ACTIVE,
              name = "Person Four",
              nomisLegalStatus = null,
              tabType = null,
              lastWorkedOnBy = "X Y",
              probationPractitioner = probationPractitionerFor(comUser01),
              prisonCode = "BAI",
              prisonDescription = "Moorland (HMP)",
            ),
          ),
          attentionNeededResults = emptyList(),
        ),
      )
    }

    @Test
    fun `telemetry is captured`() {
      whenever(
        licenceCaseRepository.findLicenceCases(
          ProbationQuery.statusCodes,
          ProbationQuery.prisonCodes,
        ),
      ).thenReturn(
        listOf(
          createLicenceCase(
            licenceId = 4,
            licenceStatus = LicenceStatus.ACTIVE,
            nomisId = "A1234AD",
            forename = "Person",
            surname = "Four",
          ),
        ),
      )

      val results = service.searchForOffenderOnPrisonCaseAdminCaseload(
        aPrisonUserSearchRequest.copy(query = "A1234AD"),
      )

      verify(telemetryService).recordCaseloadLoad(
        eq(CaProbationCaseload),
        eq(aPrisonUserSearchRequest.prisonCaseloads),
        eq(results.onProbationResults),
      )
    }

    @Test
    fun `should successfully search by probation practitioner name for offender on probation`() {
      whenever(licenceCaseRepository.findLicenceCases(PrisonQuery.statusCodes, ProbationQuery.prisonCodes))
        .thenReturn(emptyList())
      whenever(
        licenceCaseRepository.findLicenceCases(
          ProbationQuery.statusCodes,
          ProbationQuery.prisonCodes,
        ),
      ).thenReturn(
        listOf(
          createLicenceCase(
            licenceId = 3,
            licenceStatus = LicenceStatus.ACTIVE,
            nomisId = "A1234AC",
            forename = "Person",
            surname = "Three",
          ),
        ),
      )

      assertThat(
        service.searchForOffenderOnPrisonCaseAdminCaseload(
          aPrisonUserSearchRequest.copy(query = "com"),
        ),
      ).isEqualTo(
        PrisonCaseAdminSearchResult(
          inPrisonResults = emptyList(),
          onProbationResults = listOf(
            caCase().copy(
              licenceId = 3,
              prisonerNumber = "A1234AC",
              licenceStatus = LicenceStatus.ACTIVE,
              name = "Person Three",
              nomisLegalStatus = null,
              tabType = null,
              lastWorkedOnBy = "X Y",
              probationPractitioner = probationPractitionerFor(comUser01),
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
      whenever(
        licenceCaseRepository.findLicenceCases(
          ProbationQuery.statusCodes,
          ProbationQuery.prisonCodes,
        ),
      ).thenReturn(
        listOf(
          createLicenceCase(
            licenceId = 3,
            licenceStatus = LicenceStatus.ACTIVE,
            nomisId = "A1234AC",
            forename = "Person",
            surname = "Three",
          ),
        ),
      )

      val results = service.searchForOffenderOnPrisonCaseAdminCaseload(
        aPrisonUserSearchRequest.copy(query = "com"),
      )

      assertThat(results).isEqualTo(
        PrisonCaseAdminSearchResult(
          inPrisonResults = listOf(
            caCase().copy(
              licenceId = 1,
              prisonerNumber = "A1234AA",
              name = "Person One",
              nomisLegalStatus = "SENTENCED",
              lastWorkedOnBy = "X Y",
              probationPractitioner = probationPractitionerFor(comUser01),
              prisonCode = "BAI",
              prisonDescription = "Moorland (HMP)",
            ),
          ),
          onProbationResults = listOf(
            caCase().copy(
              licenceId = 3,
              prisonerNumber = "A1234AC",
              licenceStatus = LicenceStatus.ACTIVE,
              name = "Person Three",
              nomisLegalStatus = null,
              tabType = null,
              lastWorkedOnBy = "X Y",
              probationPractitioner = probationPractitionerFor(comUser01),
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
      val licenceCases = listOf(
        createLicenceCase(
          licenceId = 1,
          licenceStatus = LicenceStatus.ACTIVE,
          nomisId = "A1234AC",
          licenceStartDate = LocalDate.now().minusDays(1),
          forename = "Last",
        ),
        createLicenceCase(
          licenceId = 2,
          licenceStatus = LicenceStatus.ACTIVE,
          nomisId = "A1234BC",
          licenceStartDate = LocalDate.now(),
          forename = "Second",
        ),
        createLicenceCase(
          licenceId = 5,
          licenceStatus = LicenceStatus.ACTIVE,
          nomisId = "A1234BD",
          licenceStartDate = LocalDate.now(),
          forename = "Forth",
        ),
        createLicenceCase(
          licenceId = 3,
          licenceStatus = LicenceStatus.ACTIVE,
          nomisId = "A1234CC",
          licenceStartDate = LocalDate.now().plusDays(1),
          forename = "First",
        ),
        createLicenceCase(
          licenceId = 4,
          licenceStatus = LicenceStatus.ACTIVE,
          nomisId = "A1234DC",
          licenceStartDate = LocalDate.now(),
          forename = "Third",
        ),
      )

      whenever(
        licenceCaseRepository.findLicenceCases(
          ProbationQuery.statusCodes,
          ProbationQuery.prisonCodes,
        ),
      ).thenReturn(licenceCases)

      val results = service.searchForOffenderOnPrisonCaseAdminCaseload(
        aPrisonUserSearchRequest.copy(query = "com"),
      )

      assertThat(results.onProbationResults.map { it.licenceId }).isEqualTo(listOf(3L, 2L, 4L, 5L, 1L))
    }

    @Test
    fun `should return all results when query string is empty`() {
      whenever(
        licenceCaseRepository.findLicenceCases(
          ProbationQuery.statusCodes,
          ProbationQuery.prisonCodes,
        ),
      ).thenReturn(
        listOf(
          createLicenceCase(
            licenceId = 4,
            licenceStatus = LicenceStatus.ACTIVE,
            nomisId = "A1234AD",
            forename = "Person",
            surname = "Four",
          ),
        ),
      )

      assertThat(
        service.searchForOffenderOnPrisonCaseAdminCaseload(
          aPrisonUserSearchRequest.copy(query = ""),
        ),
      ).isEqualTo(
        PrisonCaseAdminSearchResult(
          inPrisonResults = listOf(
            caCase().copy(
              licenceId = 1,
              prisonerNumber = "A1234AA",
              name = "Person One",
              nomisLegalStatus = "SENTENCED",
              lastWorkedOnBy = "X Y",
              probationPractitioner = probationPractitionerFor(comUser01),
              prisonCode = "BAI",
              prisonDescription = "Moorland (HMP)",
            ),
            caCase().copy(
              licenceId = 2,
              prisonerNumber = "A1234AB",
              name = "Person Two",
              nomisLegalStatus = "SENTENCED",
              lastWorkedOnBy = "X Y",
              probationPractitioner = probationPractitionerFor(tcomUser03),
              prisonCode = "BAI",
              prisonDescription = "Moorland (HMP)",
            ),
          ),
          onProbationResults = listOf(
            caCase().copy(
              licenceId = 4,
              prisonerNumber = "A1234AD",
              licenceStatus = LicenceStatus.ACTIVE,
              name = "Person Four",
              nomisLegalStatus = null,
              tabType = null,
              lastWorkedOnBy = "X Y",
              probationPractitioner = probationPractitionerFor(comUser01),
              prisonCode = "BAI",
              prisonDescription = "Moorland (HMP)",
            ),
          ),
          attentionNeededResults = listOf(
            caCase().copy(
              licenceId = 3,
              prisonerNumber = "A1234AC",
              name = "Person Three",
              nomisLegalStatus = "SENTENCED",
              releaseDate = null,
              releaseDateLabel = "CRD",
              tabType = CaViewCasesTab.ATTENTION_NEEDED,
              lastWorkedOnBy = "X Y",
              probationPractitioner = probationPractitionerFor(atcomUser02),
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
              caCase().copy(
                licenceId = 3,
                prisonerNumber = "A1234AC",
                name = "Person Three",
                nomisLegalStatus = "SENTENCED",
                releaseDate = null,
                releaseDateLabel = "CRD",
                tabType = CaViewCasesTab.ATTENTION_NEEDED,
                lastWorkedOnBy = "X Y",
                probationPractitioner = probationPractitionerFor(atcomUser02),
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
              caCase().copy(
                licenceId = 3,
                prisonerNumber = "A1234AC",
                name = "Person Three",
                nomisLegalStatus = "SENTENCED",
                releaseDate = null,
                releaseDateLabel = "CRD",
                tabType = CaViewCasesTab.ATTENTION_NEEDED,
                lastWorkedOnBy = "X Y",
                probationPractitioner = probationPractitionerFor(atcomUser02),
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
            inPrisonResults = listOf(
              caCase().copy(
                licenceId = 2,
                prisonerNumber = "A1234AB",
                name = "Person Two",
                nomisLegalStatus = "SENTENCED",
                releaseDate = LocalDate.of(2021, 10, 22),
                releaseDateLabel = "Confirmed release date",
                tabType = CaViewCasesTab.FUTURE_RELEASES,
                lastWorkedOnBy = "X Y",
                probationPractitioner = probationPractitionerFor(tcomUser03),
                prisonCode = "BAI",
                prisonDescription = "Moorland (HMP)",
              ),
            ),
            onProbationResults = emptyList(),
            attentionNeededResults = listOf(
              caCase().copy(
                licenceId = 3,
                prisonerNumber = "A1234AC",
                name = "Person Three",
                nomisLegalStatus = "SENTENCED",
                releaseDate = null,
                releaseDateLabel = "CRD",
                tabType = CaViewCasesTab.ATTENTION_NEEDED,
                lastWorkedOnBy = "X Y",
                probationPractitioner = probationPractitionerFor(atcomUser02),
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
      val licenceCases = listOf(
        createLicenceCase(
          licenceId = 1,
          licenceStatus = SUBMITTED,
          nomisId = "A1234AC",
          licenceStartDate = null,
          forename = "Last",
          comUsername = comUser01.username?.uppercase(),
        ),
        createLicenceCase(
          licenceId = 2,
          licenceStatus = SUBMITTED,
          nomisId = "A1234BC",
          licenceStartDate = null,
          forename = "Second",
        ),
        createLicenceCase(
          licenceId = 5,
          licenceStatus = SUBMITTED,
          nomisId = "A1234BD",
          licenceStartDate = null,
          forename = "Forth",
        ),
        createLicenceCase(
          licenceId = 3,
          licenceStatus = SUBMITTED,
          nomisId = "A1234CC",
          licenceStartDate = null,
          forename = "First",
        ),
        createLicenceCase(
          licenceId = 4,
          licenceStatus = SUBMITTED,
          nomisId = "A1234DC",
          licenceStartDate = null,
          forename = "Third",
        ),
      )

      whenever(
        licenceCaseRepository.findLicenceCases(
          PrisonQuery.statusCodes,
          PrisonQuery.prisonCodes,
        ),
      ).thenReturn(licenceCases)

      val prisoners = licenceCases.map {
        prisonerSearchResult().copy(
          prisonerNumber = it.prisonNumber!!,
          firstName = it.forename!!,
          dateOfBirth = LocalDate.of(1985, 12, 28),
        )
      }

      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(prisoners)

      // When
      val results =
        service.searchForOffenderOnPrisonCaseAdminCaseload(aPrisonUserSearchRequest.copy(query = "com"))

      // Then
      assertThat(results.attentionNeededResults.map { it.licenceId }).isEqualTo(listOf(1L, 2L, 3L, 4L, 5L))
    }

    @Test
    fun `should have correct releaseDateLabel when postRecallReleaseDate is the same as licenceStartDate`() {
      // Given
      val licenceCase = createLicenceCase(
        licenceId = 1,
        licenceStartDate = tenDaysFromNow,
        postRecallReleaseDate = tenDaysFromNow,
      )
      whenever(workingDaysService.getLastWorkingDay(licenceCase.postRecallReleaseDate))
        .thenReturn(licenceCase.postRecallReleaseDate)
      whenever(licenceCaseRepository.findLicenceCases(any(), any())).thenReturn(listOf(licenceCase))

      // When
      val prisonOmuCaseload = service.getProbationOmuCaseload(setOf("BAI"), "")

      // Then
      assertThat(prisonOmuCaseload).hasSize(1)
      assertThat(prisonOmuCaseload[0].releaseDateLabel).isEqualTo("Post-recall release date (PRRD)")
    }

    @Test
    fun `should use HDCAD as release label where a HDCAD is set`() {
      whenever(licenceCaseRepository.findLicenceCases(any(), any())).thenReturn(
        listOf(
          createLicenceCase(
            licenceId = 1,
            licenceStartDate = oneDayFromNow,
            homeDetentionCurfewActualDate = oneDayFromNow,
          ),
        ),
      )
      whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

      val probationOmuCaseload = service.getProbationOmuCaseload(setOf("BAI"), "")

      assertThat(probationOmuCaseload).isEqualTo(
        listOf(
          caCase().copy(
            licenceId = 1,
            name = "Person One",
            prisonerNumber = "A1234AA",
            releaseDate = oneDayFromNow,
            tabType = null,
            nomisLegalStatus = null,
            probationPractitioner = probationPractitionerFor(comUser01),
            lastWorkedOnBy = "X Y",
            releaseDateLabel = "HDCAD",
            prisonCode = "BAI",
            prisonDescription = "Moorland (HMP)",
          ),
        ),
      )
    }
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

    val probationCase = ProbationCase(crn = "X12347", nomisId = "A1234AA")

    val comUser01 = StaffNameResponse(
      id = 2000L,
      username = "com-user",
      name = Name(
        forename = "com",
        surname = "user",
      ),
      code = "AB00001",
    )
    val atcomUser02 = StaffNameResponse(
      id = 2001L,
      username = "atcom",
      name = Name(
        forename = "anotherforename",
        surname = "anothersurname",
      ),
      code = "AB00002",
    )
    val tcomUser03 = StaffNameResponse(
      id = 2001L,
      username = "tcom",
      name = Name(
        forename = "yetanotherforename",
        surname = "yetanothersurname",
      ),
      code = "AB00003",
    )

    fun probationPractitionerFor(staff: StaffNameResponse) = ProbationPractitioner(staffCode = staff.code, name = staff.name.fullName())

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

    val aCommunityManagerWithoutUser =
      CommunityManagerWithoutUser(
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
