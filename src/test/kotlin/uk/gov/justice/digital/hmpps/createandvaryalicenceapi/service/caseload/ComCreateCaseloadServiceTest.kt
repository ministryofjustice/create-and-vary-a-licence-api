package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseloadItem
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService.HdcStatuses
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceCreationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ManagedCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.User
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.toPrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

class ComCreateCaseloadServiceTest {
  private val caseloadService = mock<CaseloadService>()
  private val deliusApiClient = mock<DeliusApiClient>()
  private val licenceService = mock<LicenceService>()
  private val hdcService = mock<HdcService>()
  private val eligibilityService = mock<EligibilityService>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val licenceCreationService = mock<LicenceCreationService>()

  private val service = ComCreateCaseloadService(
    caseloadService,
    deliusApiClient,
    licenceService,
    eligibilityService,
    hdcService,
    releaseDateService,
    licenceCreationService,
  )

  private val elevenDaysFromNow = LocalDate.now().plusDays(11)
  private val tenDaysFromNow = LocalDate.now().plusDays(10)
  private val nineDaysFromNow = LocalDate.now().plusDays(9)
  private val yesterday = LocalDate.now().minusDays(1)
  private val deliusStaffIdentifier = 213L

  @BeforeEach
  fun reset() {
    reset(deliusApiClient, licenceService, hdcService, eligibilityService)
  }

  @Test
  fun `it filters out cases with no NOMIS record`() {
    val cases = listOf(ManagedCase(nomisRecord = null, cvlFields = CvlFields(LicenceType.AP_PSS)))
    val casesAndLicences = service.mapOffendersToLicences(cases)

    assertThat(casesAndLicences).hasSize(0)
    verify(licenceService, never()).findLicencesMatchingCriteria(any())
  }

  @Test
  fun `it calls the licence service when Nomis records are found`() {
    val cases = listOf(
      ManagedCase(
        nomisRecord = Prisoner(
          prisonerNumber = "ABC123",
          firstName = "Person",
          lastName = "One",
          dateOfBirth = LocalDate.of(1970, 1, 1),
          conditionalReleaseDate = tenDaysFromNow,
        ),
        cvlFields = CvlFields(
          hardStopDate = LocalDate.of(2023, Month.FEBRUARY, 3),
          hardStopWarningDate = LocalDate.of(2023, Month.FEBRUARY, 1),
          licenceType = LicenceType.PSS,
        ),
      ),
    )

    val casesAndLicences = service.mapOffendersToLicences(cases)

    assertThat(casesAndLicences).hasSize(1)
    with(casesAndLicences.first()) {
      assertThat(licences).hasSize(1)
      assertThat(licences[0].licenceStatus).isEqualTo(LicenceStatus.NOT_STARTED)
    }
    verify(licenceService).findLicencesMatchingCriteria(any())
  }

  @Test
  fun `it sets not started licences to timed out when in the hard stop period`() {
    val cases = listOf(
      ManagedCase(
        nomisRecord = Prisoner(
          prisonerNumber = "ABC123",
          firstName = "Person",
          lastName = "One",
          dateOfBirth = LocalDate.of(1970, 1, 1),
        ),
        cvlFields = CvlFields(isInHardStopPeriod = true, licenceType = LicenceType.AP),
      ),
    )

    val caseAndLicences = service.mapOffendersToLicences(cases)

    assertThat(caseAndLicences).hasSize(1)
    with(caseAndLicences.first()) {
      assertThat(nomisRecord?.prisonerNumber).isEqualTo("ABC123")
      assertThat(cvlFields.isInHardStopPeriod).isTrue()
      assertThat(licences).hasSize(1)
      assertThat(licences[0].licenceStatus).isEqualTo(LicenceStatus.TIMED_OUT)
      assertThat(licences[0].licenceType).isEqualTo(LicenceType.AP)
    }
  }

  @Test
  fun `it filters invalid data due to mismatch between delius and nomis`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12346"),
      ManagedOffenderCrn(crn = "X12347"),
      ManagedOffenderCrn(crn = "X12348"),
    )

    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)

    whenever(deliusApiClient.getProbationCases(managedOffenders.mapNotNull { it.crn })).thenReturn(
      listOf(
        createProbationCase(crn = "X12346", nomsNumber = "AB1234D"),
        createProbationCase(crn = "X12347"),
        createProbationCase(crn = "X12348", nomsNumber = "AB1234E"),
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        createCaseloadItem("AB1234E", tenDaysFromNow, bookingId = "1"),
      ),
    )
    val prisonersToLicenceStartDates = mapOf("AB1234E" to tenDaysFromNow)
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(releaseDateService.getLicenceStartDates(any(), any())).thenReturn(prisonersToLicenceStartDates)

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload.first(),
      "X12348",
      "AB1234E",
      LicenceStatus.NOT_STARTED,
      LicenceType.AP,
      expectedReleaseDate = tenDaysFromNow,
    )
  }

  @Test
  fun `it filters offenders who are ineligible for a licence`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(crn = "X12348"),
      ManagedOffenderCrn(crn = "X12349"),
      ManagedOffenderCrn(crn = "X12350"),
      ManagedOffenderCrn(crn = "X12351"),
      ManagedOffenderCrn(crn = "X12352"),
      ManagedOffenderCrn(crn = "X12353"),
      ManagedOffenderCrn(crn = "X12354"),
      ManagedOffenderCrn(crn = "X12355"),
      ManagedOffenderCrn(crn = "X12356"),
    )

    whenever(
      deliusApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)

    whenever(deliusApiClient.getProbationCases(managedOffenders.mapNotNull { it.crn })).thenReturn(
      listOf(
        createProbationCase(nomsNumber = "AB1234E", crn = "X12348"),
        createProbationCase(nomsNumber = "AB1234F", crn = "X12349"),
        createProbationCase(nomsNumber = "AB1234G", crn = "X12350"),
        createProbationCase(nomsNumber = "AB1234L", crn = "X12351"),
        createProbationCase(nomsNumber = "AB1234M", crn = "X12352"),
        createProbationCase(nomsNumber = "AB1234N", crn = "X12353"),
        createProbationCase(nomsNumber = "AB1234P", crn = "X12354"),
        createProbationCase(nomsNumber = "AB1234Q", crn = "X12355"),
        createProbationCase(nomsNumber = "AB1234R", crn = "X12356"),
      ),
    )

    val caseloadItems = listOf(
      createCaseloadItem("AB1234E", nineDaysFromNow, paroleEligibilityDate = yesterday, bookingId = "1"),
      createCaseloadItem("AB1234F", tenDaysFromNow, paroleEligibilityDate = tenDaysFromNow, bookingId = "2"),
      createCaseloadItem("AB1234G", tenDaysFromNow, legalStatus = "DEAD", bookingId = "3"),
      createCaseloadItem("AB1234H", tenDaysFromNow, indeterminateSentence = true, bookingId = "4"),
      createCaseloadItem("AB1234I", tenDaysFromNow, bookingId = "5"),
      createCaseloadItem("AB1234J", tenDaysFromNow, bookingId = "6"),
      createCaseloadItem("AB1234K", tenDaysFromNow, bookingId = "123"),
      createCaseloadItem("AB1234L", nineDaysFromNow, bookingId = "123"),
      // This case tests that recalls are overridden if the PRRD < the conditionalReleaseDate - so NOT_STARTED
      createCaseloadItem(
        "AB1234M",
        tenDaysFromNow,
        postRecallReleaseDate = nineDaysFromNow,
        recall = true,
        bookingId = "7",
      ),
      createCaseloadItem(
        prisonerNumber = "AB1234N",
        conditionalReleaseDate = tenDaysFromNow,
        postRecallReleaseDate = elevenDaysFromNow,
        recall = true,
        bookingId = "8",
      ),
      // This case tests that recalls are overridden if the PRRD is equal to the conditionalReleaseDate - so NOT_STARTED
      createCaseloadItem(
        prisonerNumber = "AB1234P",
        conditionalReleaseDate = nineDaysFromNow,
        postRecallReleaseDate = nineDaysFromNow,
        recall = true,
        bookingId = "9",
      ),
      // This case tests that recalls are overridden if no PRRD exists and there is only the conditionalReleaseDate - so NOT_STARTED
      createCaseloadItem("AB1234Q", nineDaysFromNow, recall = true, bookingId = "10"),
      // This case tests that the case is included when the status is INACTIVE TRN
      createCaseloadItem("AB1234R", nineDaysFromNow, status = "INACTIVE TRN", bookingId = "11"),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(caseloadItems)

    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(eligibilityService.isEligibleForCvl(caseloadItems[1].prisoner.toPrisonerSearchPrisoner())).thenReturn(
      false,
    )
    whenever(eligibilityService.isEligibleForCvl(caseloadItems[2].prisoner.toPrisonerSearchPrisoner())).thenReturn(
      false,
    )
    whenever(eligibilityService.isEligibleForCvl(caseloadItems[9].prisoner.toPrisonerSearchPrisoner())).thenReturn(
      false,
    )
    val prisonersToLicenceStartDates = caseloadItems.associate {
      it.prisoner.prisonerNumber!! to it.prisoner.conditionalReleaseDate
    }
    whenever(releaseDateService.getLicenceStartDates(any(), any())).thenReturn(prisonersToLicenceStartDates)

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(6)
    verifyCase(
      case = caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = nineDaysFromNow,
    )
    verifyCase(
      case = caseload[1],
      expectedCrn = "X12351",
      expectedPrisonerNumber = "AB1234L",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = nineDaysFromNow,
    )
    verifyCase(
      case = caseload[2],
      expectedCrn = "X12354",
      expectedPrisonerNumber = "AB1234P",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = nineDaysFromNow,
    )
    verifyCase(
      case = caseload[3],
      expectedCrn = "X12355",
      expectedPrisonerNumber = "AB1234Q",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = nineDaysFromNow,
    )
    verifyCase(
      case = caseload[4],
      expectedCrn = "X12356",
      expectedPrisonerNumber = "AB1234R",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = nineDaysFromNow,
    )
    verifyCase(
      case = caseload[5],
      expectedCrn = "X12352",
      expectedPrisonerNumber = "AB1234M",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = tenDaysFromNow,
    )
  }

  // isn't working properly
  @Test
  fun `it filters out cases passed LSD`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        staff = StaffDetail(name = Name(forename = "John", surname = "Doe"), code = "X54321"),
      ),
      ManagedOffenderCrn(
        crn = "X12349",
        staff = StaffDetail(name = Name(forename = "John", surname = "Doe"), code = "X54321"),
      ),
    )

    whenever(
      deliusApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)

    whenever(deliusApiClient.getProbationCases(managedOffenders.mapNotNull { it.crn })).thenReturn(
      listOf(
        createProbationCase(nomsNumber = "AB1234E", crn = "X12348"),
        createProbationCase(nomsNumber = "AB1234F", crn = "X12349"),
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        createCaseloadItem(
          bookingId = "1",
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = tenDaysFromNow,
          releaseDate = tenDaysFromNow,
          licenceExpiryDate = LocalDate.of(
            2022,
            Month.DECEMBER,
            26,
          ),
        ),
        createCaseloadItem(
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
      ),
    )

    whenever(licenceService.findLicencesMatchingCriteria(any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12348",
          nomisId = "AB1234E",
          kind = LicenceKind.CRD,
          licenceType = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.SUBMITTED,
          licenceExpiryDate = elevenDaysFromNow,
          comUsername = "johndoe",
          licenceStartDate = LocalDate.now().minusDays(2),
        ),
        createLicenceSummary(
          crn = "X12349",
          nomisId = "AB1234F",
          kind = LicenceKind.CRD,
          licenceType = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.SUBMITTED,
          licenceExpiryDate = elevenDaysFromNow,
          comUsername = "johndoe",
          licenceStartDate = tenDaysFromNow,
        ),
      ),
    )
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(deliusApiClient.getStaffDetailsByUsername(any())).thenReturn(
      listOf(
        User(
          username = "johndoe",
          code = "X54321",
          name = Name(forename = "John", surname = "Doe"),
          teams = emptyList(),
        ),
      ),
    )

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)
    assertThat(caseload).hasSize(1)
    verifyCase(
      case = caseload[0],
      expectedCrn = "X12349",
      expectedPrisonerNumber = "AB1234F",
      expectedLicenceStatus = LicenceStatus.SUBMITTED,
      expectedLicenceType = LicenceType.AP_PSS,
      expectedReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X54321", name = "John Doe"),
    )
  }

  @Test
  fun `it builds the staff create caseload`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
      ManagedOffenderCrn(
        crn = "X12349",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
      ManagedOffenderCrn(
        crn = "X12350",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
      ManagedOffenderCrn(
        crn = "X12351",
        staff = StaffDetail(name = Name(forename = null, surname = null), code = "X1234", unallocated = true),
      ),
      ManagedOffenderCrn(
        crn = "X12352",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
    )

    whenever(
      deliusApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)

    whenever(deliusApiClient.getProbationCases(managedOffenders.mapNotNull { it.crn })).thenReturn(
      listOf(
        createProbationCase(nomsNumber = "AB1234E", crn = "X12348"),
        createProbationCase(nomsNumber = "AB1234F", crn = "X12349"),
        createProbationCase(nomsNumber = "AB1234G", crn = "X12350"),
        createProbationCase(nomsNumber = "AB1234H", crn = "X12351"),
        createProbationCase(nomsNumber = "AB1234I", crn = "X12352"),
      ),
    )

    val caseloadItems = listOf(
      createCaseloadItem(
        bookingId = "1",
        prisonerNumber = "AB1234E",
        conditionalReleaseDate = tenDaysFromNow,
        releaseDate = tenDaysFromNow,
        licenceExpiryDate = LocalDate.of(
          2022,
          Month.DECEMBER,
          26,
        ),
      ),
      createCaseloadItem(
        bookingId = "2",
        prisonerNumber = "AB1234F",
        conditionalReleaseDate = tenDaysFromNow,
        releaseDate = tenDaysFromNow,
        status = "INACTIVE OUT",
      ),
      createCaseloadItem(
        bookingId = "3",
        prisonerNumber = "AB1234G",
        conditionalReleaseDate = tenDaysFromNow,
        releaseDate = tenDaysFromNow,
        status = "INACTIVE OUT",
      ),
      createCaseloadItem(
        bookingId = "4",
        prisonerNumber = "AB1234H",
        conditionalReleaseDate = tenDaysFromNow,
        releaseDate = tenDaysFromNow,
        topupSupervisionExpiryDate = LocalDate.of(2023, Month.JUNE, 22),
      ),
      createCaseloadItem(
        bookingId = "5",
        prisonerNumber = "AB1234I",
        conditionalReleaseDate = elevenDaysFromNow,
        releaseDate = elevenDaysFromNow,
        topupSupervisionExpiryDate = LocalDate.of(2023, Month.JUNE, 22),
        licenceExpiryDate = elevenDaysFromNow,
      ),
    )
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(caseloadItems)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)

    whenever(licenceService.findLicencesMatchingCriteria(any())).thenReturn(
      listOf(
        createLicenceSummary(
          crn = "X12352",
          nomisId = "AB1234I",
          kind = LicenceKind.CRD,
          licenceType = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.SUBMITTED,
          licenceExpiryDate = elevenDaysFromNow,
          comUsername = "johndoe",
          licenceStartDate = elevenDaysFromNow,
        ),
      ),
    )

    whenever(deliusApiClient.getStaffDetailsByUsername(any())).thenReturn(
      listOf(
        User(
          username = "johndoe",
          code = "X54321",
          name = Name(forename = "John", surname = "Doe"),
          teams = emptyList(),
        ),
      ),
    )

    val prisonersToLicenceStartDates = caseloadItems.associate {
      it.prisoner.prisonerNumber!! to it.prisoner.conditionalReleaseDate
    }
    whenever(releaseDateService.getLicenceStartDates(any(), any())).thenReturn(prisonersToLicenceStartDates)

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)
    assertThat(caseload).hasSize(3)
    verifyCase(
      case = caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X1234", name = "Joe Bloggs"),
    )
    verifyCase(
      case = caseload[1],
      expectedCrn = "X12351",
      expectedPrisonerNumber = "AB1234H",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.PSS,
      expectedReleaseDate = tenDaysFromNow,
    )
    verifyCase(
      case = caseload[2],
      expectedCrn = "X12352",
      expectedPrisonerNumber = "AB1234I",
      expectedLicenceStatus = LicenceStatus.SUBMITTED,
      expectedLicenceType = LicenceType.AP_PSS,
      expectedReleaseDate = elevenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X54321",
        name = "John Doe",
      ),
    )
  }

  @Test
  fun `it builds the team create caseload`() {
    val selectedTeam = "team c"

    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
      ManagedOffenderCrn(
        crn = "X12349",
        staff = StaffDetail(name = Name(forename = "John", surname = "Doe"), code = "X54321"),
      ),
    )

    whenever(
      deliusApiClient.getManagedOffendersByTeam(selectedTeam),
    ).thenReturn(managedOffenders)

    whenever(deliusApiClient.getProbationCases(managedOffenders.mapNotNull { it.crn })).thenReturn(
      listOf(
        createProbationCase(nomsNumber = "AB1234E", crn = "X12348"),
        createProbationCase(nomsNumber = "AB1234F", crn = "X12349"),
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        createCaseloadItem(
          bookingId = "1",
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = tenDaysFromNow,
          status = "ACTIVE IN",
        ),
        createCaseloadItem(
          bookingId = "2",
          prisonerNumber = "AB1234F",
          conditionalReleaseDate = tenDaysFromNow,
          topupSupervisionExpiryDate = LocalDate.of(2022, Month.DECEMBER, 26),
        ),
      ),
    )
    val prisonersToLicenceStartDates = mapOf("AB1234E" to tenDaysFromNow, "AB1234F" to tenDaysFromNow)

    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(releaseDateService.getLicenceStartDates(any(), any())).thenReturn(prisonersToLicenceStartDates)

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
        staffCode = "X1234",
        name = "Joe Bloggs",
      ),
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
      ),
    )
  }

  @Test
  fun `PRRD licences will be mapped to offenders and caseloads for team will be created`() {
    // Given
    val selectedTeam = "team c"
    val prisonerNumber = "AB1234E"
    val managedOffender = TestData.managedOffenderCrn()
    val managedOffenders = listOf(managedOffender)
    val probationCase = createProbationCase(nomsNumber = prisonerNumber, crn = managedOffender.crn!!)
    val caseLoadItem = createCaseloadItem(
      bookingId = "1",
      prisonerNumber = prisonerNumber,
      conditionalReleaseDate = null,
      postRecallReleaseDate = LocalDate.now(),
    )
    val prisonersToLicenceStartDates = mapOf(prisonerNumber to tenDaysFromNow, prisonerNumber to tenDaysFromNow)

    whenever(deliusApiClient.getManagedOffendersByTeam(selectedTeam)).thenReturn(managedOffenders)
    whenever(deliusApiClient.getProbationCases(managedOffenders.map { it.crn!! })).thenReturn(listOf(probationCase))
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(listOf(caseLoadItem))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(releaseDateService.getLicenceStartDates(any(), any())).thenReturn(prisonersToLicenceStartDates)

    // When
    val caseload = service.getTeamCreateCaseload(listOf(), listOf(selectedTeam))

    // Then
    assertThat(caseload).hasSize(1)
  }

  @Test
  fun `PRRD licences will be mapped to offenders and caseloads for staff will be created`() {
    // Given
    val prisonerNumber = "AB1234E"
    val managedOffender = TestData.managedOffenderCrn()
    val managedOffenders = listOf(managedOffender)
    val probationCase = createProbationCase(nomsNumber = prisonerNumber, crn = managedOffender.crn!!)
    val caseLoadItem = createCaseloadItem(
      bookingId = "1",
      prisonerNumber = prisonerNumber,
      conditionalReleaseDate = null,
      postRecallReleaseDate = LocalDate.now(),
    )
    val prisonersToLicenceStartDates = mapOf(prisonerNumber to tenDaysFromNow, prisonerNumber to tenDaysFromNow)

    whenever(deliusApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)
    whenever(deliusApiClient.getProbationCases(managedOffenders.mapNotNull { it.crn })).thenReturn(listOf(probationCase))
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(listOf(caseLoadItem))
    whenever(eligibilityService.isEligibleForCvl(any())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(releaseDateService.getLicenceStartDates(any(), any())).thenReturn(prisonersToLicenceStartDates)

    // When
    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    // Then
    assertThat(caseload).hasSize(1)
  }

  @Test
  fun `it filters recalls and breach of supervision on team create caseload`() {
    val selectedTeam = "team C"

    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
      ManagedOffenderCrn(
        crn = "X12349",
        staff = StaffDetail(name = Name(forename = "John", surname = "Doe"), code = "X54321"),
      ),
    )

    whenever(
      deliusApiClient.getManagedOffendersByTeam(selectedTeam),
    ).thenReturn(managedOffenders)

    whenever(deliusApiClient.getProbationCases(managedOffenders.mapNotNull { it.crn })).thenReturn(
      listOf(
        createProbationCase(nomsNumber = "AB1234E", crn = "X12348"),
        createProbationCase(nomsNumber = "AB1234F", crn = "X12349"),
      ),
    )

    val caseloadItems = listOf(
      createCaseloadItem(
        bookingId = "1",
        prisonerNumber = "AB1234E",
        conditionalReleaseDate = tenDaysFromNow,
        releaseDate = tenDaysFromNow,
        postRecallReleaseDate = tenDaysFromNow,
        recall = true,
      ),
      createCaseloadItem(
        bookingId = "2",
        prisonerNumber = "AB1234F",
        conditionalReleaseDate = tenDaysFromNow,
        releaseDate = tenDaysFromNow,
        imprisonmentStatus = "BOTUS",
        recall = true,
      ),
    )

    val prisonersToLicenceStartDates = mapOf("AB1234E" to tenDaysFromNow, "AB1234F" to tenDaysFromNow)
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(caseloadItems)
    whenever(eligibilityService.isEligibleForCvl(caseloadItems[0].prisoner.toPrisonerSearchPrisoner())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))
    whenever(releaseDateService.getLicenceStartDates(any(), any())).thenReturn(prisonersToLicenceStartDates)

    val caseload = service.getTeamCreateCaseload(listOf("team A", "team B"), listOf("team C"))

    verify(deliusApiClient).getManagedOffendersByTeam("team C")
    assertThat(caseload).hasSize(1)
    verifyCase(
      case = caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X1234", name = "Joe Bloggs"),
    )

    verify(deliusApiClient).getManagedOffendersByTeam("team C")
  }

  @Test
  fun `it filters out HDC approved licences on team create caseload`() {
    val selectedTeam = "team C"

    val managedOffenders = listOf(
      ManagedOffenderCrn(
        crn = "X12348",
        staff = StaffDetail(name = Name(forename = "Joe", surname = "Bloggs"), code = "X1234"),
      ),
      ManagedOffenderCrn(
        crn = "X12349",
        staff = StaffDetail(name = Name(forename = "John", surname = "Doe"), code = "X54321"),
      ),
    )

    whenever(
      deliusApiClient.getManagedOffendersByTeam(selectedTeam),
    ).thenReturn(managedOffenders)

    whenever(deliusApiClient.getProbationCases(managedOffenders.mapNotNull { it.crn })).thenReturn(
      listOf(
        createProbationCase(nomsNumber = "AB1234E", crn = "X12348"),
        createProbationCase(nomsNumber = "AB1234F", crn = "X12349"),
      ),
    )

    val caseloadItems = listOf(
      createCaseloadItem(
        bookingId = "1",
        prisonerNumber = "AB1234E",
        conditionalReleaseDate = tenDaysFromNow,
        releaseDate = tenDaysFromNow,
        postRecallReleaseDate = tenDaysFromNow,
        recall = true,
      ),
    )
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(caseloadItems)
    whenever(eligibilityService.isEligibleForCvl(caseloadItems[0].prisoner.toPrisonerSearchPrisoner())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(setOf(caseloadItems[0].prisoner.bookingId?.toLong()!!)))

    val caseload = service.getTeamCreateCaseload(listOf("team A", "team B"), listOf("team C"))

    verify(deliusApiClient).getManagedOffendersByTeam("team C")
    assertThat(caseload).isEmpty()
  }

  private fun createCaseloadItem(
    prisonerNumber: String,
    conditionalReleaseDate: LocalDate?,
    status: String = "ACTIVE IN",
    postRecallReleaseDate: LocalDate? = null,
    recall: Boolean = false,
    bookingId: String? = null,
    topupSupervisionExpiryDate: LocalDate? = null,
    licenceExpiryDate: LocalDate? = null,
    releaseDate: LocalDate? = null,
    imprisonmentStatus: String? = null,
    confirmedReleaseDate: LocalDate? = null,
    paroleEligibilityDate: LocalDate? = null,
    legalStatus: String? = null,
    indeterminateSentence: Boolean? = false,
  ): CaseloadItem = CaseloadItem(
    prisoner = Prisoner(
      prisonerNumber = prisonerNumber,
      conditionalReleaseDate = conditionalReleaseDate,
      firstName = "Person",
      lastName = "One",
      dateOfBirth = LocalDate.of(1970, 1, 1),
      status = status,
      bookingId = bookingId,
      postRecallReleaseDate = postRecallReleaseDate,
      recall = recall,
      topupSupervisionExpiryDate = topupSupervisionExpiryDate,
      licenceExpiryDate = licenceExpiryDate,
      releaseDate = releaseDate,
      imprisonmentStatus = imprisonmentStatus,
      confirmedReleaseDate = confirmedReleaseDate,
      paroleEligibilityDate = paroleEligibilityDate,
      legalStatus = legalStatus,
      indeterminateSentence = indeterminateSentence,
    ),
    cvl = CvlFields(licenceType = LicenceType.PSS),
  )

  private fun verifyCase(
    case: ComCase,
    expectedCrn: String,
    expectedPrisonerNumber: String,
    expectedLicenceStatus: LicenceStatus,
    expectedLicenceType: LicenceType,
    expectedReleaseDate: LocalDate? = null,
    expectedProbationPractitioner: ProbationPractitioner? = null,
    expectedReviewNeeded: Boolean = false,
  ) {
    with(case) {
      assertThat(crnNumber).isEqualTo(expectedCrn)
      assertThat(prisonerNumber).isEqualTo(expectedPrisonerNumber)
      assertThat(licenceStatus).isEqualTo(expectedLicenceStatus)
      assertThat(licenceType).isEqualTo(expectedLicenceType)
      expectedReleaseDate.let { assertThat(releaseDate).isEqualTo(expectedReleaseDate) }
      expectedProbationPractitioner.let { assertThat(probationPractitioner).isEqualTo(expectedProbationPractitioner) }
      assertThat(isReviewNeeded).isEqualTo(expectedReviewNeeded)
    }
  }

  private fun createProbationCase(crn: String, nomsNumber: String? = null) = ProbationCase(
    nomisId = nomsNumber,
    crn = crn,
  )

  private fun createLicenceSummary(
    crn: String,
    nomisId: String,
    licenceType: LicenceType,
    licenceStatus: LicenceStatus,
    kind: LicenceKind = LicenceKind.CRD,
    licenceExpiryDate: LocalDate? = null,
    comUsername: String? = null,
    conditionalReleaseDate: LocalDate? = null,
    confirmedReleaseDate: LocalDate? = null,
    licenceStartDate: LocalDate? = null,
    isReviewNeeded: Boolean = false,
  ): LicenceSummary = LicenceSummary(
    crn = crn,
    nomisId = nomisId,
    kind = kind,
    licenceId = 1,
    licenceType = licenceType,
    licenceStatus = licenceStatus,
    licenceExpiryDate = licenceExpiryDate,
    comUsername = comUsername,
    isReviewNeeded = isReviewNeeded,
    isDueForEarlyRelease = false,
    isInHardStopPeriod = false,
    isDueToBeReleasedInTheNextTwoWorkingDays = false,
    conditionalReleaseDate = conditionalReleaseDate,
    actualReleaseDate = confirmedReleaseDate,
    licenceStartDate = licenceStartDate,
    dateCreated = LocalDateTime.now(),
    updatedByFullName = "X Y",
    bookingId = null,
    dateOfBirth = null,
    forename = null,
    surname = null,
    prisonCode = null,
    prisonDescription = null,
    probationLauCode = null,
    probationPduCode = null,
    probationAreaCode = null,
    probationTeamCode = null,
    probationAreaDescription = null,
    probationTeamDescription = null,
    probationLauDescription = null,
    probationPduDescription = null,
  )
}
