package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComCreateCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCaseRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadType.ComCreateStaffCaseload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadType.ComCreateTeamCaseload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecordService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TelemetryService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aCvlRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.communityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com.ComCreateCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.CaseAccessResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import kotlin.collections.get

class ComCreateCaseloadServiceTest {
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val deliusApiClient = mock<DeliusApiClient>()
  private val licenceCaseRepository = mock<LicenceCaseRepository>()
  private val eligibilityService = mock<EligibilityService>()
  private val cvlRecordService = mock<CvlRecordService>()
  private val telemetryService = mock<TelemetryService>()

  private var service = ComCreateCaseloadService(
    prisonerSearchApiClient,
    deliusApiClient,
    licenceCaseRepository,
    cvlRecordService,
    telemetryService,
  )

  private val elevenDaysFromNow = LocalDate.now().plusDays(11)
  private val tenDaysFromNow = LocalDate.now().plusDays(10)
  private val nineDaysFromNow = LocalDate.now().plusDays(9)
  private val twoDaysFromNow = LocalDate.now().plusDays(2)
  private val yesterday = LocalDate.now().minusDays(1)
  private val twoDaysAgo = LocalDate.now().minusDays(2)
  private val fiveDaysAgo = LocalDate.now().minusDays(5)
  private val deliusStaffIdentifier = 213L
  private val staffDetail = StaffDetail(code = "X1234", name = Name(forename = "Joe", surname = "Bloggs"))

  @BeforeEach
  fun reset() {
    val authentication = org.mockito.kotlin.mock<Authentication>()
    val securityContext = org.mockito.kotlin.mock<SecurityContext>()

    whenever(authentication.name).thenReturn(aCom().username)
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    reset(deliusApiClient, licenceCaseRepository, eligibilityService)
  }

  @Test
  fun `it filters out cases with no NOMIS record`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E"),
    )
    whenever(
      deliusApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(emptyList())

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(0)
    verify(cvlRecordService, times(1)).getCvlRecords(emptyList())
  }

  @Test
  fun `it filters out cases with no NOMIS ID on their Delius records`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12348", nomisId = null),
    )
    whenever(
      deliusApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(emptyList())

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(0)
  }

  @Test
  fun `it filters invalid data due to mismatch between delius and nomis`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12346", nomisId = "AB1234D", staff = staffDetail),
      ManagedOffenderCrn(crn = "X12347", staff = staffDetail),
      ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E", staff = staffDetail),
    )

    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = tenDaysFromNow,
          bookingId = "1",
        ),
      ),
    )
    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(
          nomsId = "AB1234E",
          licenceStartDate = tenDaysFromNow,
        ),
      ),
    )
    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(
          nomsId = "AB1234E",
          licenceStartDate = tenDaysFromNow,
        ),
      ),
    )

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload.first(),
      "X12348",
      "AB1234E",
      LicenceStatus.NOT_STARTED,
      LicenceType.AP,
      LicenceCreationType.LICENCE_NOT_STARTED,
      expectedReleaseDate = tenDaysFromNow,
    )
  }

  @Test
  fun `telemetry is captured for staff`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12346", nomisId = "AB1234D", staff = staffDetail),
      ManagedOffenderCrn(crn = "X12347", staff = staffDetail),
      ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E", staff = staffDetail),
    )

    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = tenDaysFromNow,
          bookingId = "1",
        ),
      ),
    )
    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(
          nomsId = "AB1234E",
          licenceStartDate = tenDaysFromNow,
        ),
      ),
    )
    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(
          nomsId = "AB1234E",
          licenceStartDate = tenDaysFromNow,
        ),
      ),
    )

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload.first(),
      "X12348",
      "AB1234E",
      LicenceStatus.NOT_STARTED,
      LicenceType.AP,
      LicenceCreationType.LICENCE_NOT_STARTED,
      expectedReleaseDate = tenDaysFromNow,
    )

    verify(telemetryService).recordCaseloadLoad(eq(ComCreateStaffCaseload), eq(setOf("213")), eq(caseload))
  }

  @Test
  fun `telemetry is captured for teams`() {
    val selectedTeam = "team c"

    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        nomisId = "AB1234E",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
      ManagedOffenderCrn(
        crn = "X12349",
        nomisId = "AB1234F",
        staff = StaffDetail(name = Name(forename = "John", surname = "Doe"), code = "X54321"),
      ),
    )

    whenever(
      deliusApiClient.getManagedOffendersByTeam(selectedTeam),
    ).thenReturn(managedOffenders)

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(
          bookingId = "1",
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = tenDaysFromNow,
          status = "ACTIVE IN",
        ),
        prisonerSearchResult().copy(
          bookingId = "2",
          prisonerNumber = "AB1234F",
          conditionalReleaseDate = tenDaysFromNow,
          licenceExpiryDate = null,
          topupSupervisionExpiryDate = LocalDate.of(2022, Month.DECEMBER, 26),
        ),
      ),
    )

    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(nomsId = "AB1234E", licenceStartDate = tenDaysFromNow),
        aCvlRecord(nomsId = "AB1234F", licenceStartDate = tenDaysFromNow),
      ),
    )

    val caseload = service.getTeamCreateCaseload(listOf("team A", "team B"), listOf(selectedTeam))

    assertThat(caseload).hasSize(2)

    verify(telemetryService).recordCaseloadLoad(
      eq(ComCreateTeamCaseload),
      eq(setOf(selectedTeam)),
      eq(caseload),
    )
  }

  @Test
  fun `it filters offenders who are ineligible for a licence`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E", staff = staffDetail),
      ManagedOffenderCrn(crn = "X12349", nomisId = "AB1234F", staff = staffDetail),
      ManagedOffenderCrn(crn = "X12350", nomisId = "AB1234G", staff = staffDetail),
      ManagedOffenderCrn(crn = "X12351", nomisId = "AB1234L", staff = staffDetail),
      ManagedOffenderCrn(crn = "X12352", nomisId = "AB1234M", staff = staffDetail),
      ManagedOffenderCrn(crn = "X12353", nomisId = "AB1234N", staff = staffDetail),
      ManagedOffenderCrn(crn = "X12354", nomisId = "AB1234P", staff = staffDetail),
      ManagedOffenderCrn(crn = "X12355", nomisId = "AB1234Q", staff = staffDetail),
      ManagedOffenderCrn(crn = "X12356", nomisId = "AB1234R", staff = staffDetail),
    )

    whenever(
      deliusApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)

    val caseloadItems = listOf(
      prisonerSearchResult().copy(
        prisonerNumber = "AB1234E",
        conditionalReleaseDate = nineDaysFromNow,
        paroleEligibilityDate = yesterday,
        bookingId = "1",
        firstName = "ABC DEF",
      ),
      prisonerSearchResult().copy(
        prisonerNumber = "AB1234F",
        conditionalReleaseDate = tenDaysFromNow,
        paroleEligibilityDate = tenDaysFromNow,
        bookingId = "2",
      ),
      prisonerSearchResult().copy(
        prisonerNumber = "AB1234G",
        conditionalReleaseDate = tenDaysFromNow,
        legalStatus = "DEAD",
        bookingId = "3",
      ),
      prisonerSearchResult().copy(
        prisonerNumber = "AB1234H",
        conditionalReleaseDate = tenDaysFromNow,
        indeterminateSentence = true,
        bookingId = "4",
      ),
      prisonerSearchResult().copy(prisonerNumber = "AB1234I", conditionalReleaseDate = tenDaysFromNow, bookingId = "5"),
      prisonerSearchResult().copy(prisonerNumber = "AB1234J", conditionalReleaseDate = tenDaysFromNow, bookingId = "6"),
      prisonerSearchResult().copy(
        prisonerNumber = "AB1234K",
        conditionalReleaseDate = tenDaysFromNow,
        bookingId = "123",
      ),
      prisonerSearchResult().copy(
        prisonerNumber = "AB1234L",
        conditionalReleaseDate = nineDaysFromNow,
        bookingId = "123",
        firstName = "ABC XYZ",
      ),
      // This case tests that recalls are overridden if the PRRD < the conditionalReleaseDate - so NOT_STARTED
      prisonerSearchResult().copy(
        prisonerNumber = "AB1234M",
        conditionalReleaseDate = tenDaysFromNow,
        postRecallReleaseDate = nineDaysFromNow,
        recall = true,
        bookingId = "7",
      ),
      prisonerSearchResult().copy(
        prisonerNumber = "AB1234N",
        conditionalReleaseDate = tenDaysFromNow,
        postRecallReleaseDate = elevenDaysFromNow,
        recall = true,
        bookingId = "8",
      ),
      // This case tests that recalls are overridden if the PRRD is equal to the conditionalReleaseDate - so NOT_STARTED
      prisonerSearchResult().copy(
        prisonerNumber = "AB1234P",
        conditionalReleaseDate = nineDaysFromNow,
        postRecallReleaseDate = nineDaysFromNow,
        recall = true,
        bookingId = "9",
        firstName = "G",
      ),
      // This case tests that recalls are overridden if no PRRD exists and there is only the conditionalReleaseDate - so NOT_STARTED
      prisonerSearchResult().copy(
        prisonerNumber = "AB1234Q",
        conditionalReleaseDate = nineDaysFromNow,
        recall = true,
        bookingId = "10",
        firstName = "P",
      ),
      // This case tests that the case is included when the status is INACTIVE TRN
      prisonerSearchResult().copy(
        prisonerNumber = "AB1234R",
        conditionalReleaseDate = nineDaysFromNow,
        status = "INACTIVE TRN",
        bookingId = "11",
        firstName = "S",
      ),
    )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(caseloadItems)

    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(nomsId = "AB1234E", licenceStartDate = nineDaysFromNow),
        aCvlRecord(
          nomsId = "AB1234F",
          kind = null,
          licenceStartDate = tenDaysFromNow,
        ).copy(isEligible = false),
        aCvlRecord(
          nomsId = "AB1234G",
          kind = null,
          licenceStartDate = tenDaysFromNow,
        ).copy(isEligible = false),
        aCvlRecord(nomsId = "AB1234H", licenceStartDate = tenDaysFromNow),
        aCvlRecord(nomsId = "AB1234I", licenceStartDate = tenDaysFromNow),
        aCvlRecord(nomsId = "AB1234J", licenceStartDate = tenDaysFromNow),
        aCvlRecord(nomsId = "AB1234K", licenceStartDate = tenDaysFromNow),
        aCvlRecord(nomsId = "AB1234L", licenceStartDate = nineDaysFromNow),
        aCvlRecord(nomsId = "AB1234M", licenceStartDate = tenDaysFromNow),
        aCvlRecord(
          nomsId = "AB1234N",
          kind = null,
          licenceStartDate = tenDaysFromNow,
        ).copy(isEligible = false),
        aCvlRecord(nomsId = "AB1234P", licenceStartDate = nineDaysFromNow),
        aCvlRecord(nomsId = "AB1234Q", licenceStartDate = nineDaysFromNow),
        aCvlRecord(nomsId = "AB1234R", licenceStartDate = nineDaysFromNow),
      ),
    )

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(6)
    verifyCase(
      case = caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = nineDaysFromNow,
      expectedLicenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED,
    )
    verifyCase(
      case = caseload[1],
      expectedCrn = "X12351",
      expectedPrisonerNumber = "AB1234L",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = nineDaysFromNow,
      expectedLicenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED,
    )
    verifyCase(
      case = caseload[2],
      expectedCrn = "X12354",
      expectedPrisonerNumber = "AB1234P",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = nineDaysFromNow,
      expectedLicenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED,
    )
    verifyCase(
      case = caseload[3],
      expectedCrn = "X12355",
      expectedPrisonerNumber = "AB1234Q",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = nineDaysFromNow,
      expectedLicenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED,
    )
    verifyCase(
      case = caseload[4],
      expectedCrn = "X12356",
      expectedPrisonerNumber = "AB1234R",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = nineDaysFromNow,
      expectedLicenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED,
    )
    verifyCase(
      case = caseload[5],
      expectedCrn = "X12352",
      expectedPrisonerNumber = "AB1234M",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = tenDaysFromNow,
      expectedLicenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED,
    )
  }

  @Test
  fun `it filters out cases with a CRD in the past unless they are eligible for time served`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        nomisId = "AB1234E",
        staff = StaffDetail(name = Name(forename = "John", surname = "Doe"), code = "X54321"),
      ),
      ManagedOffenderCrn(
        crn = "X12349",
        nomisId = "AB1234F",
        staff = StaffDetail(name = Name(forename = "John", surname = "Doe"), code = "X54321"),
      ),
      ManagedOffenderCrn(
        crn = "X12350",
        nomisId = "AB1234G",
        staff = StaffDetail(name = Name(forename = "John", surname = "Doe"), code = "X54321"),
      ),
    )

    whenever(
      deliusApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(
          bookingId = "1",
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = twoDaysAgo,
          releaseDate = twoDaysAgo,
          licenceExpiryDate = LocalDate.of(
            2022,
            Month.DECEMBER,
            26,
          ),
        ),
        prisonerSearchResult().copy(
          bookingId = "2",
          prisonerNumber = "AB1234F",
          conditionalReleaseDate = tenDaysFromNow,
          releaseDate = tenDaysFromNow,
          licenceExpiryDate = LocalDate.of(
            2022,
            Month.DECEMBER,
            26,
          ),
        ),
        prisonerSearchResult().copy(
          bookingId = "3",
          prisonerNumber = "AB1234G",
          conditionalReleaseDate = fiveDaysAgo,
        ),
      ),
    )

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.SUBMITTED,
          comUsername = "johndoe",
          licenceStartDate = twoDaysAgo,
        ),
        createLicenceComCase(
          crn = "X12349",
          nomisId = "AB1234F",
          kind = LicenceKind.CRD,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.SUBMITTED,
          comUsername = "johndoe",
          licenceStartDate = tenDaysFromNow,
        ),
      ),
    )
    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(nomsId = "AB1234E", licenceStartDate = twoDaysAgo),
        aCvlRecord(nomsId = "AB1234F", licenceStartDate = tenDaysFromNow),
        aCvlRecord(
          nomsId = "AB1234G",
          hardStopKind = LicenceKind.TIME_SERVED,
          licenceStartDate = fiveDaysAgo,
        ),
      ),
    )

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)
    assertThat(caseload).hasSize(2)
    verifyCase(
      case = caseload[0],
      expectedCrn = "X12350",
      expectedPrisonerNumber = "AB1234G",
      expectedLicenceStatus = LicenceStatus.TIMED_OUT,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = fiveDaysAgo,
      expectedLicenceKind = LicenceKind.TIME_SERVED,
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X54321", name = "John Doe", allocated = true),
      expectedLicenceCreationType = LicenceCreationType.PRISON_WILL_CREATE_THIS_LICENCE,
    )
    verifyCase(
      case = caseload[1],
      expectedCrn = "X12349",
      expectedPrisonerNumber = "AB1234F",
      expectedLicenceStatus = LicenceStatus.SUBMITTED,
      expectedLicenceType = LicenceType.AP_PSS,
      expectedReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X54321", name = "John Doe", allocated = true),
      expectedLicenceCreationType = LicenceCreationType.LICENCE_IN_PROGRESS,
    )
  }

  @Test
  fun `it builds the staff create caseload`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        nomisId = "AB1234E",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
      ManagedOffenderCrn(
        crn = "X12349",
        nomisId = "AB1234F",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
      ManagedOffenderCrn(
        crn = "X12350",
        nomisId = "AB1234G",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
      ManagedOffenderCrn(
        crn = "X12351",
        nomisId = "AB1234H",
        staff = StaffDetail(name = Name(forename = "Ann", surname = "Officer"), code = "X1235", unallocated = true),
      ),
      ManagedOffenderCrn(
        crn = "X12352",
        nomisId = "AB1234I",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
    )

    whenever(
      deliusApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)

    val case1 = prisonerSearchResult().copy(
      bookingId = "1",
      prisonerNumber = "AB1234E",
      conditionalReleaseDate = tenDaysFromNow,
      releaseDate = tenDaysFromNow,
      licenceExpiryDate = LocalDate.of(
        2022,
        Month.DECEMBER,
        26,
      ),
    )
    val case2 = prisonerSearchResult().copy(
      bookingId = "2",
      prisonerNumber = "AB1234F",
      conditionalReleaseDate = tenDaysFromNow,
      releaseDate = tenDaysFromNow,
      status = "INACTIVE OUT",
    )
    val case3 = prisonerSearchResult().copy(
      bookingId = "3",
      prisonerNumber = "AB1234G",
      conditionalReleaseDate = tenDaysFromNow,
      releaseDate = tenDaysFromNow,
      status = "INACTIVE OUT",
    )
    val case4 = prisonerSearchResult().copy(
      bookingId = "4",
      prisonerNumber = "AB1234H",
      conditionalReleaseDate = tenDaysFromNow,
      releaseDate = tenDaysFromNow,
      licenceExpiryDate = null,
      topupSupervisionExpiryDate = LocalDate.of(2023, Month.JUNE, 22),
    )
    val case5 = prisonerSearchResult().copy(
      bookingId = "5",
      prisonerNumber = "AB1234I",
      conditionalReleaseDate = elevenDaysFromNow,
      releaseDate = elevenDaysFromNow,
      topupSupervisionExpiryDate = LocalDate.of(2023, Month.JUNE, 22),
      licenceExpiryDate = elevenDaysFromNow,
    )

    val caseloadItems = listOf(
      case1,
      case2,
      case3,
      case4,
      case5,
    )
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(caseloadItems)

    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(nomsId = "AB1234E", kind = LicenceKind.CRD, licenceStartDate = tenDaysFromNow),
        aCvlRecord(
          nomsId = "AB1234F",
          licenceStartDate = tenDaysFromNow,
        ).copy(isEligible = false),
        aCvlRecord(
          nomsId = "AB1234G",
          licenceStartDate = tenDaysFromNow,
        ).copy(isEligible = false),
        aCvlRecord(
          nomsId = "AB1234H",
          licenceStartDate = tenDaysFromNow,
          hardStopWarningDate = tenDaysFromNow,
          licenceType = LicenceType.PSS,
        ),
        aCvlRecord(
          nomsId = "AB1234I",
          licenceStartDate = elevenDaysFromNow,
          licenceType = LicenceType.AP_PSS,
        ),
      ),
    )

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceComCase(
          crn = "X12352",
          nomisId = "AB1234I",
          kind = LicenceKind.CRD,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.SUBMITTED,
          comUsername = "johndoe",
          licenceStartDate = elevenDaysFromNow,
        ),
      ),
    )

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)
    assertThat(caseload).hasSize(3)
    verifyCase(
      case = caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X1234", name = "Joe Bloggs", allocated = true),
      expectedLicenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED,
    )
    verifyCase(
      case = caseload[1],
      expectedCrn = "X12351",
      expectedPrisonerNumber = "AB1234H",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.PSS,
      expectedReleaseDate = tenDaysFromNow,
      expectedLicenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED,
      expectedProbationPractitioner = ProbationPractitioner.unallocated("X1235"),
      expectedHardstopWarningDate = tenDaysFromNow,
    )
    verifyCase(
      case = caseload[2],
      expectedCrn = "X12352",
      expectedPrisonerNumber = "AB1234I",
      expectedLicenceStatus = LicenceStatus.SUBMITTED,
      expectedLicenceType = LicenceType.AP_PSS,
      expectedReleaseDate = elevenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X1234",
        name = "Joe Bloggs",
        allocated = true,
      ),
      expectedLicenceCreationType = LicenceCreationType.LICENCE_IN_PROGRESS,
    )
  }

  @Test
  fun `it builds the team create caseload`() {
    val selectedTeam = "team c"

    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        nomisId = "AB1234E",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X12352"),
      ),
      ManagedOffenderCrn(
        crn = "X12349",
        nomisId = "AB1234F",
        staff = StaffDetail(name = Name(forename = "John", surname = "Doe"), code = "X54321"),
      ),
    )

    whenever(
      deliusApiClient.getManagedOffendersByTeam(selectedTeam),
    ).thenReturn(managedOffenders)

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(
          bookingId = "1",
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = tenDaysFromNow,
          status = "ACTIVE IN",
        ),
        prisonerSearchResult().copy(
          bookingId = "2",
          prisonerNumber = "AB1234F",
          conditionalReleaseDate = tenDaysFromNow,
          licenceExpiryDate = null,
          topupSupervisionExpiryDate = LocalDate.of(2022, Month.DECEMBER, 26),
        ),
      ),
    )

    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(nomsId = "AB1234E", licenceStartDate = tenDaysFromNow),
        aCvlRecord(
          nomsId = "AB1234F",
          licenceStartDate = tenDaysFromNow,
          licenceType = LicenceType.PSS,
        ),
      ),
    )

    val caseload = service.getTeamCreateCaseload(listOf("team A", "team B"), listOf(selectedTeam))

    verify(deliusApiClient).getManagedOffendersByTeam(selectedTeam)
    assertThat(caseload).hasSize(2)
    verifyCase(
      case = caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X12352",
        name = "Joe Bloggs",
        allocated = true,
      ),
      expectedLicenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED,
    )

    verifyCase(
      case = caseload[1],
      expectedCrn = "X12349",
      expectedPrisonerNumber = "AB1234F",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.PSS,
      expectedReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X54321",
        name = "John Doe",
        allocated = true,
      ),
      expectedLicenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED,
    )
  }

  @Test
  fun `PRRD licences will be mapped to offenders and caseloads for team will be created`() {
    // Given
    val selectedTeam = "team c"
    val prisonerNumber = "AB1234E"
    val managedOffender = aManagedOffenderCrn(prisonerNumber)
    val managedOffenders = listOf(managedOffender)
    val caseLoadItem = prisonerSearchResult().copy(
      bookingId = "1",
      prisonerNumber = prisonerNumber,
      conditionalReleaseDate = null,
      postRecallReleaseDate = LocalDate.now(),
    )

    whenever(deliusApiClient.getManagedOffendersByTeam(selectedTeam)).thenReturn(managedOffenders)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(caseLoadItem))
    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(
          nomsId = prisonerNumber,
          kind = LicenceKind.PRRD,
          licenceStartDate = LocalDate.now(),
        ),
      ),
    )

    // When
    val caseload = service.getTeamCreateCaseload(listOf(), listOf(selectedTeam))

    // Then
    assertThat(caseload).hasSize(1)
    assertThat(caseload.first().kind).isEqualTo(LicenceKind.PRRD)
  }

  @Test
  fun `PRRD licences will be mapped to offenders and caseloads for staff will be created`() {
    // Given
    val prisonerNumber = "AB1234E"
    val managedOffender = aManagedOffenderCrn(prisonerNumber)
    val managedOffenders = listOf(managedOffender)
    val caseLoadItem = prisonerSearchResult().copy(
      bookingId = "1",
      prisonerNumber = prisonerNumber,
      conditionalReleaseDate = null,
      postRecallReleaseDate = LocalDate.now(),
    )

    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(caseLoadItem))
    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(
          nomsId = prisonerNumber,
          kind = LicenceKind.PRRD,
          licenceStartDate = LocalDate.now(),
        ),
      ),
    )

    // When
    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    // Then
    assertThat(caseload).hasSize(1)
    assertThat(caseload.first().kind).isEqualTo(LicenceKind.PRRD)
  }

  @Test
  fun `it filters out NOT_STARTED PRRD licences`() {
    whenever(
      deliusApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(listOf(ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E")))

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = yesterday,
          bookingId = "5",
          postRecallReleaseDate = tenDaysFromNow,
        ),
      ),
    )

    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(
          nomsId = "AB1234E",
          licenceStartDate = tenDaysFromNow,
        ).copy(isEligible = false),
      ),
    )

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(0)
  }

  @Test
  fun `it selects a licence edit over the approved licence`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E", staff = staffDetail),
    )
    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = tenDaysFromNow,
          bookingId = "1",
        ),
      ),
    )

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.APPROVED,
          comUsername = "johndoe",
          licenceStartDate = tenDaysFromNow,
        ),
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.IN_PROGRESS,
          comUsername = "johndoe",
          licenceStartDate = tenDaysFromNow,
        ),
      ),
    )
    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(
          nomsId = "AB1234E",
          licenceStartDate = tenDaysFromNow,
        ),
      ),
    )

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload.first(),
      "X12348",
      "AB1234E",
      LicenceStatus.IN_PROGRESS,
      LicenceType.AP_PSS,
      LicenceCreationType.LICENCE_IN_PROGRESS,
      expectedReleaseDate = tenDaysFromNow,
    )
  }

  @Test
  fun `it selects a hard stop licence over timed out licence`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E", staff = staffDetail),
    )
    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = twoDaysFromNow,
          bookingId = "1",
        ),
      ),
    )

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.TIMED_OUT,
          comUsername = "johndoe",
          licenceStartDate = twoDaysFromNow,
        ),
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.HARD_STOP,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.IN_PROGRESS,
          comUsername = "johndoe",
          licenceStartDate = twoDaysFromNow,
        ),
      ),
    )
    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(
          nomsId = "AB1234E",
          licenceStartDate = twoDaysFromNow,
        ),
      ),
    )

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload.first(),
      "X12348",
      "AB1234E",
      LicenceStatus.TIMED_OUT,
      LicenceType.AP_PSS,
      LicenceCreationType.PRISON_WILL_CREATE_THIS_LICENCE,
      expectedReleaseDate = twoDaysFromNow,
      expectedLicenceKind = LicenceKind.HARD_STOP,
    )
  }

  @Test
  fun `it sets LicenceCreationType to PRISON_WILL_CREATE_THIS_LICENCE if the hard stop licence has not been started`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E", staff = staffDetail),
    )
    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = twoDaysFromNow,
          bookingId = "1",
        ),
      ),
    )

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.TIMED_OUT,
          comUsername = "johndoe",
          licenceStartDate = twoDaysFromNow,
        ),
      ),
    )
    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(
          nomsId = "AB1234E",
          licenceStartDate = twoDaysFromNow,
        ),
      ),
    )

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload.first(),
      "X12348",
      "AB1234E",
      LicenceStatus.TIMED_OUT,
      LicenceType.AP_PSS,
      LicenceCreationType.PRISON_WILL_CREATE_THIS_LICENCE,
      expectedReleaseDate = twoDaysFromNow,
      expectedLicenceKind = LicenceKind.CRD,
    )
  }

  @Test
  fun `it sets LicenceCreationType to PRISON_WILL_CREATE_THIS_LICENCE if no licence has been started in the hard stop period`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E", staff = staffDetail),
    )

    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = twoDaysFromNow,
          bookingId = "1",
          licenceExpiryDate = tenDaysFromNow,
          topupSupervisionExpiryDate = elevenDaysFromNow,
        ),
      ),
    )

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      emptyList(),
    )
    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(
          nomsId = "AB1234E",
          licenceStartDate = twoDaysFromNow,
          isInHardStopPeriod = true,
          licenceType = LicenceType.AP_PSS,
          isTimedOut = true,
        ),
      ),
    )

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload.first(),
      "X12348",
      "AB1234E",
      LicenceStatus.TIMED_OUT,
      LicenceType.AP_PSS,
      LicenceCreationType.PRISON_WILL_CREATE_THIS_LICENCE,
      expectedReleaseDate = twoDaysFromNow,
      expectedLicenceKind = LicenceKind.CRD,
    )
  }

  @Test
  fun `when time served case with no licence then case kind uses CvlRecord hard stop kind`() {
    val prisonerNumber = "A1234AA"
    val managedOffenders = listOf(ManagedOffenderCrn(crn = "X12348", prisonerNumber))
    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(prisonerSearchResult(prisonerNumber = prisonerNumber)),
    )
    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(emptyList())
    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(
          nomsId = prisonerNumber,
          hardStopKind = LicenceKind.TIME_SERVED,
          licenceStartDate = LocalDate.now(),
          isInHardStopPeriod = true,
        ),
      ),
    )

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    assertThat(caseload[0].kind).isEqualTo(LicenceKind.TIME_SERVED)
  }

  @Test
  fun `when not time served case with no licence then case kind uses CvlRecord kind`() {
    val prisonerNumber = "A1234AA"
    val managedOffenders = listOf(ManagedOffenderCrn(crn = "X12348", prisonerNumber))
    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(prisonerSearchResult(prisonerNumber = prisonerNumber)),
    )
    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(emptyList())
    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(
          nomsId = prisonerNumber,
          licenceStartDate = tenDaysFromNow,
        ),
      ),
    )

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    assertThat(caseload[0].kind).isEqualTo(LicenceKind.CRD)
  }

  @Test
  fun `it sets LicenceCreationType to LICENCE_CREATED_BY_PRISON if the hard stop licence has been submitted`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E", staff = staffDetail),
    )
    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = twoDaysFromNow,
          bookingId = "1",
        ),
      ),
    )

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.HARD_STOP,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.SUBMITTED,
          comUsername = "johndoe",
          licenceStartDate = twoDaysFromNow,
        ),
      ),
    )
    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(
          nomsId = "AB1234E",
          licenceStartDate = twoDaysFromNow,
        ),
      ),
    )

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload.first(),
      "X12348",
      "AB1234E",
      LicenceStatus.TIMED_OUT,
      LicenceType.AP_PSS,
      LicenceCreationType.LICENCE_CREATED_BY_PRISON,
      expectedReleaseDate = twoDaysFromNow,
      expectedLicenceKind = LicenceKind.HARD_STOP,
    )
  }

  @Test
  fun `com cases which are need to be review not returned`() {
    // Given
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E"),
    )
    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = twoDaysFromNow,
          bookingId = "1",
        ),
      ),
    )

    val comCase = createLicenceComCase(
      crn = "X12348",
      nomisId = "AB1234E",
      kind = LicenceKind.HARD_STOP,
      typeCode = LicenceType.AP_PSS,
      licenceStatus = LicenceStatus.ACTIVE,
      comUsername = "johndoe",
      licenceStartDate = twoDaysFromNow,
      reviewDate = null,
    )

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(listOf(comCase))
    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(
          nomsId = "AB1234E",
          licenceStartDate = twoDaysFromNow,
        ),
      ),
    )

    // When
    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    // Then
    assertThat(caseload).isEmpty()
  }

  @Test
  fun `it sets LicenceCreationType to LICENCE_CHANGES_NOT_APPROVED_IN_TIME if an edit times out`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E", staff = staffDetail),
    )
    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = twoDaysFromNow,
          bookingId = "1",
        ),
      ),
    )

    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(
      listOf(
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.APPROVED,
          comUsername = "johndoe",
          licenceStartDate = twoDaysFromNow,
        ),
        createLicenceComCase(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          typeCode = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.TIMED_OUT,
          comUsername = "johndoe",
          licenceStartDate = twoDaysFromNow,
          versionOfId = 1,
        ),
      ),
    )
    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(
          nomsId = "AB1234E",
          licenceStartDate = twoDaysFromNow,
        ),
      ),
    )

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload.first(),
      "X12348",
      "AB1234E",
      LicenceStatus.TIMED_OUT,
      LicenceType.AP_PSS,
      LicenceCreationType.LICENCE_CHANGES_NOT_APPROVED_IN_TIME,
      expectedReleaseDate = twoDaysFromNow,
    )
  }

  @Test
  fun `search for offenders with no licence sets status to TIMED_OUT when cvl record is timed out`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E", staff = staffDetail),
    )

    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
      listOf(
        prisonerSearchResult().copy(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = tenDaysFromNow,
          bookingId = "1",
        ),
      ),
    )
    whenever(licenceCaseRepository.findLicenceCasesForCom(any(), any())).thenReturn(emptyList())
    whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
      listOf(
        aCvlRecord(
          nomsId = "AB1234E",
          licenceStartDate = tenDaysFromNow,
          isInHardStopPeriod = true,
          hardStopDate = LocalDate.of(2023, 9, 10),
          isTimedOut = true,
        ),
      ),
    )

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload.first(),
      "X12348",
      "AB1234E",
      LicenceStatus.TIMED_OUT,
      LicenceType.AP,
      LicenceCreationType.PRISON_WILL_CREATE_THIS_LICENCE,
      expectedReleaseDate = tenDaysFromNow,
    )
  }

  @Nested
  inner class `LAO cases in caseload` {
    @BeforeEach
    fun setup() {
      service = ComCreateCaseloadService(
        prisonerSearchApiClient,
        deliusApiClient,
        licenceCaseRepository,
        cvlRecordService,
        telemetryService,
        true,
      )
      val managedOffenders = listOf(
        ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E", staff = staffDetail),
      )
      whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
        listOf(
          prisonerSearchResult().copy(
            prisonerNumber = "AB1234E",
            conditionalReleaseDate = tenDaysFromNow,
            bookingId = "1",
          ),
        ),
      )
      whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
        listOf(aCvlRecord(nomsId = "AB1234E", licenceStartDate = tenDaysFromNow)),
      )
    }

    @Test
    fun `does not check Delius user access when laoEnabled is false`() {
      service = ComCreateCaseloadService(
        prisonerSearchApiClient,
        deliusApiClient,
        licenceCaseRepository,
        cvlRecordService,
        telemetryService,
        false,
      )
      whenever(deliusApiClient.getCheckUserAccess(any(), any(), any())).thenReturn(
        listOf(aCaseAccessResponse(crn = "X12348", excluded = true, restricted = false)),
      )

      val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

      assertThat(caseload).hasSize(1)
      verify(deliusApiClient, times(0)).getCheckUserAccess(any(), any(), any())
    }

    @Test
    fun `LAO cases are returned as restricted in the caseload`() {
      val selectedTeam = "team c"

      val managedOffenders = listOf(
        ManagedOffenderCrn(
          crn = "X12348",
          nomisId = "AB1234E",
          staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
        ),
        ManagedOffenderCrn(
          crn = "X12349",
          nomisId = "AB1234F",
          staff = StaffDetail(name = Name(forename = "John", surname = "Doe"), code = "X54321"),
        ),
      )

      whenever(deliusApiClient.getManagedOffendersByTeam(selectedTeam)).thenReturn(managedOffenders)
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
        listOf(
          prisonerSearchResult().copy(
            bookingId = "1",
            prisonerNumber = "AB1234E",
            conditionalReleaseDate = tenDaysFromNow,
          ),
          prisonerSearchResult().copy(
            bookingId = "2",
            prisonerNumber = "AB1234F",
            conditionalReleaseDate = tenDaysFromNow,
          ),
        ),
      )
      whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
        listOf(
          aCvlRecord(nomsId = "AB1234E", licenceStartDate = tenDaysFromNow),
          aCvlRecord(nomsId = "AB1234F", licenceStartDate = tenDaysFromNow),
        ),
      )
      whenever(deliusApiClient.getCheckUserAccess(any(), any(), any())).thenReturn(
        listOf(
          aCaseAccessResponse(crn = "X12348", excluded = true, restricted = false),
          aCaseAccessResponse(crn = "X12349", excluded = false, restricted = false),
        ),
      )

      val caseload = service.getTeamCreateCaseload(listOf("team A", "team B"), listOf(selectedTeam))

      assertThat(caseload).hasSize(2)
      with(caseload.last()) {
        assertThat(name).isEqualTo("Access restricted on NDelius")
        assertThat(crnNumber).isEqualTo("X12348")
        assertThat(probationPractitioner.name).isEqualTo("Restricted")
        assertThat(probationPractitioner.staffCode).isEqualTo("Restricted")
        assertThat(licenceCreationType).isEqualTo(LicenceCreationType.LICENCE_CREATION_RESTRICTED)
        assertThat(releaseDate).isEqualTo(tenDaysFromNow)
        assertThat(isLao).isTrue()
      }
    }

    @Test
    fun `it marks cases in the create caseload as LAO when user is excluded`() {
      whenever(deliusApiClient.getCheckUserAccess(any(), any(), any())).thenReturn(
        listOf(aCaseAccessResponse(crn = "X12348", excluded = true, restricted = false)),
      )

      val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

      assertThat(caseload).hasSize(1)

      with(caseload.first()) {
        assertThat(name).isEqualTo("Access restricted on NDelius")
        assertThat(crnNumber).isEqualTo("X12348")
        assertThat(probationPractitioner.name).isEqualTo("Restricted")
        assertThat(probationPractitioner.staffCode).isEqualTo("Restricted")
        assertThat(licenceCreationType).isEqualTo(LicenceCreationType.LICENCE_CREATION_RESTRICTED)
        assertThat(releaseDate).isEqualTo(tenDaysFromNow)
        assertThat(isLao).isTrue()
      }
    }

    @Test
    fun `it marks cases in the create caseload as LAO when user is restricted`() {
      whenever(deliusApiClient.getCheckUserAccess(any(), any(), any())).thenReturn(
        listOf(aCaseAccessResponse(crn = "X12348", excluded = false, restricted = true)),
      )

      val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

      assertThat(caseload).hasSize(1)
      with(caseload.first()) {
        assertThat(name).isEqualTo("Access restricted on NDelius")
        assertThat(crnNumber).isEqualTo("X12348")
        assertThat(probationPractitioner.name).isEqualTo("Restricted")
        assertThat(probationPractitioner.staffCode).isEqualTo("Restricted")
        assertThat(licenceCreationType).isEqualTo(LicenceCreationType.LICENCE_CREATION_RESTRICTED)
        assertThat(releaseDate).isEqualTo(tenDaysFromNow)
        assertThat(isLao).isTrue()
      }
    }

    @Test
    fun `it filters out LAO restricted cases with past release dates for non-time-served licences`() {
      val managedOffenders = listOf(
        ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E", staff = staffDetail),
        ManagedOffenderCrn(crn = "X12349", nomisId = "AB1234F", staff = staffDetail),
      )

      whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
        listOf(
          prisonerSearchResult().copy(
            prisonerNumber = "AB1234E",
            conditionalReleaseDate = twoDaysAgo,
            bookingId = "1",
          ),
          prisonerSearchResult().copy(
            prisonerNumber = "AB1234F",
            conditionalReleaseDate = tenDaysFromNow,
            bookingId = "2",
          ),
        ),
      )
      whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
        listOf(
          aCvlRecord(nomsId = "AB1234E", licenceStartDate = twoDaysAgo),
          aCvlRecord(nomsId = "AB1234F", licenceStartDate = tenDaysFromNow),
        ),
      )
      whenever(deliusApiClient.getCheckUserAccess(any(), any(), any())).thenReturn(
        listOf(
          aCaseAccessResponse(crn = "X12348", excluded = true, restricted = false),
          aCaseAccessResponse(crn = "X12349", excluded = true, restricted = false),
        ),
      )

      val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

      assertThat(caseload).hasSize(1)
      with(caseload.first()) {
        assertThat(name).isEqualTo("Access restricted on NDelius")
        assertThat(crnNumber).isEqualTo("X12349")
        assertThat(probationPractitioner.name).isEqualTo("Restricted")
        assertThat(probationPractitioner.staffCode).isEqualTo("Restricted")
        assertThat(licenceCreationType).isEqualTo(LicenceCreationType.LICENCE_CREATION_RESTRICTED)
        assertThat(releaseDate).isEqualTo(tenDaysFromNow)
        assertThat(isLao).isTrue()
      }
    }

    @Test
    fun `it shows LAO restricted cases with past release dates for time served licences`() {
      val managedOffenders = listOf(
        ManagedOffenderCrn(crn = "X12348", nomisId = "AB1234E", staff = staffDetail),
        ManagedOffenderCrn(crn = "X12349", nomisId = "AB1234F", staff = staffDetail),
      )

      whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
      whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(
        listOf(
          prisonerSearchResult().copy(
            prisonerNumber = "AB1234E",
            conditionalReleaseDate = twoDaysAgo,
            bookingId = "1",
          ),
          prisonerSearchResult().copy(
            prisonerNumber = "AB1234F",
            conditionalReleaseDate = twoDaysAgo,
            bookingId = "2",
          ),
        ),
      )
      whenever(cvlRecordService.getCvlRecords(any())).thenReturn(
        listOf(
          aCvlRecord(nomsId = "AB1234E", licenceStartDate = twoDaysAgo, hardStopDate = yesterday, hardStopWarningDate = twoDaysAgo, hardStopKind = LicenceKind.TIME_SERVED),
          aCvlRecord(nomsId = "AB1234F", licenceStartDate = twoDaysAgo, hardStopDate = yesterday, hardStopWarningDate = twoDaysAgo, hardStopKind = LicenceKind.TIME_SERVED),
        ),
      )
      whenever(deliusApiClient.getCheckUserAccess(any(), any(), any())).thenReturn(
        listOf(
          aCaseAccessResponse(crn = "X12348", excluded = true, restricted = false),
          aCaseAccessResponse(crn = "X12349", excluded = false, restricted = true),
        ),
      )

      val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

      assertThat(caseload).hasSize(2)
      assertThat(caseload.map { it.crnNumber }).containsExactlyInAnyOrder("X12348", "X12349")
      caseload.forEach { case ->
        assertThat(case.isLao).isTrue()
        assertThat(case.kind).isEqualTo(LicenceKind.TIME_SERVED)
        assertThat(case.releaseDate).isEqualTo(twoDaysAgo)
        assertThat(case.name).isEqualTo("Access restricted on NDelius")
        assertThat(case.probationPractitioner.name).isEqualTo("Restricted")
        assertThat(case.licenceCreationType).isEqualTo(LicenceCreationType.LICENCE_CREATION_RESTRICTED)
      }
    }
  }

  private fun verifyCase(
    case: ComCreateCase,
    expectedCrn: String,
    expectedPrisonerNumber: String,
    expectedLicenceStatus: LicenceStatus,
    expectedLicenceType: LicenceType,
    expectedLicenceCreationType: LicenceCreationType,
    expectedReleaseDate: LocalDate? = null,
    expectedProbationPractitioner: ProbationPractitioner = ProbationPractitioner("X1234", "Joe Bloggs", true),
    expectedReviewNeeded: Boolean = false,
    expectedLicenceKind: LicenceKind = LicenceKind.CRD,
    expectedHardstopWarningDate: LocalDate? = null,
  ) {
    with(case) {
      assertThat(crnNumber).isEqualTo(expectedCrn)
      assertThat(prisonerNumber).isEqualTo(expectedPrisonerNumber)
      assertThat(licenceStatus).isEqualTo(expectedLicenceStatus)
      assertThat(licenceType).isEqualTo(expectedLicenceType)
      expectedReleaseDate.let { assertThat(releaseDate).isEqualTo(expectedReleaseDate) }
      expectedProbationPractitioner.let { assertThat(probationPractitioner).isEqualTo(expectedProbationPractitioner) }
      assertThat(isReviewNeeded).isEqualTo(expectedReviewNeeded)
      assertThat(kind).isEqualTo(expectedLicenceKind)
      assertThat(licenceCreationType).isEqualTo(expectedLicenceCreationType)
      assertThat(hardStopWarningDate).isEqualTo(expectedHardstopWarningDate)
    }
  }

  private fun createLicenceComCase(
    crn: String,
    nomisId: String,
    typeCode: LicenceType,
    licenceStatus: LicenceStatus = LicenceStatus.NOT_STARTED,
    kind: LicenceKind = LicenceKind.CRD,
    prisonCode: String = "MDI",
    comUsername: String? = null,
    sentenceStartDate: LocalDate? = null,
    conditionalReleaseDate: LocalDate? = null,
    confirmedReleaseDate: LocalDate? = null,
    licenceStartDate: LocalDate? = null,
    versionOfId: Long? = null,
    reviewDate: LocalDateTime? = LocalDateTime.now(),
  ) = LicenceComCase(
    crn = crn,
    prisonNumber = nomisId,
    kind = kind,
    licenceId = 1,
    typeCode = typeCode,
    statusCode = licenceStatus,
    comUsername = comUsername,
    sentenceStartDate = sentenceStartDate,
    conditionalReleaseDate = conditionalReleaseDate,
    actualReleaseDate = confirmedReleaseDate,
    licenceStartDate = licenceStartDate,
    forename = null,
    surname = null,
    versionOfId = versionOfId,
    postRecallReleaseDate = LocalDate.now(),
    homeDetentionCurfewActualDate = LocalDate.now(),
    updatedByFirstName = "firstName",
    updatedByLastName = "lastName",
    reviewDate = reviewDate,
    prisonCode = prisonCode,
  )

  private fun aManagedOffenderCrn(nomisId: String? = "ABC123"): ManagedOffenderCrn = ManagedOffenderCrn(
    crn = "X12348",
    nomisId,
    staff = staffDetail,
  )

  private fun aCaseAccessResponse(crn: String, excluded: Boolean, restricted: Boolean) = CaseAccessResponse(
    crn = crn,
    userExcluded = excluded,
    userRestricted = restricted,
  )

  private fun aCom() = communityOffenderManager()
}
