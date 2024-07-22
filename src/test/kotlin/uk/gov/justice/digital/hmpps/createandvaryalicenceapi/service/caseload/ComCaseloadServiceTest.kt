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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ManagedCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonerSearchService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.StaffService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OffenderDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OtherIds
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffHuman
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.User
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

class ComCaseloadServiceTest {
  private val caseloadService = mock<CaseloadService>()
  private val communityApiClient = mock<CommunityApiClient>()
  private val licenceService = mock<LicenceService>()
  private val probationSearchApiClient = mock<ProbationSearchApiClient>()
  private val prisonerSearchService = mock<PrisonerSearchService>()
  private val staffService = mock<StaffService>()

  private val service = ComCaseloadService(
    caseloadService,
    communityApiClient,
    licenceService,
    prisonerSearchService,
    probationSearchApiClient,
    staffService,
  )

  private val elevenDaysFromNow = LocalDate.now().plusDays(11)
  private val tenDaysFromNow = LocalDate.now().plusDays(10)
  private val nineDaysFromNow = LocalDate.now().plusDays(9)
  private val yesterday = LocalDate.now().minusDays(1)

  @BeforeEach
  fun reset() {
    reset(communityApiClient, licenceService, prisonerSearchService)
  }

  @Test
  fun `It does not call Licence API when no Nomis records are found`() {
    val cases = listOf(ManagedCase(nomisRecord = Prisoner(), cvlFields = CvlFields(LicenceType.AP_PSS)))
    val casesAndLicences = service.mapOffendersToLicences(cases)

    assertThat(casesAndLicences).hasSize(1)
    with(casesAndLicences.first()) {
      assertThat(licences).hasSize(1)
      assertThat(licences?.get(0)?.licenceStatus).isEqualTo(LicenceStatus.NOT_STARTED)
    }
    verify(licenceService, never()).findLicencesMatchingCriteria(any())
  }

  @Test
  fun `It calls the licence service when Nomis records are found`() {
    val cases = listOf(
      ManagedCase(
        nomisRecord = Prisoner(prisonerNumber = "ABC123", conditionalReleaseDate = tenDaysFromNow),
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
      assertThat(licences?.get(0)?.licenceStatus).isEqualTo(LicenceStatus.NOT_STARTED)
    }
    verify(licenceService).findLicencesMatchingCriteria(any())
  }

  @Test
  fun `in the hard stop period Sets NOT_STARTED licences to TIMED_OUT when in the hard stop period`() {
    val cases = listOf(
      ManagedCase(
        nomisRecord = Prisoner(prisonerNumber = "ABC123"),
        cvlFields = CvlFields(isInHardStopPeriod = true, licenceType = LicenceType.PSS),
      ),
    )

    val caseAndLicences = service.mapOffendersToLicences(cases)

    assertThat(caseAndLicences).hasSize(1)
    with(caseAndLicences.first()) {
      assertThat(nomisRecord?.prisonerNumber).isEqualTo("ABC123")
      assertThat(cvlFields.isInHardStopPeriod).isTrue()
      assertThat(licences).hasSize(1)
      assertThat(licences?.get(0)?.licenceStatus).isEqualTo(LicenceStatus.TIMED_OUT)
      assertThat(licences?.get(0)?.licenceType).isEqualTo(LicenceType.PSS)
    }
  }

  @Test
  fun `'It filters invalid data due to mismatch between delius and nomis`() {
    val managedOffenders = listOf(
      ManagedOffenderCrn(offenderCrn = "X12346"),
      ManagedOffenderCrn(offenderCrn = "X12347"),
      ManagedOffenderCrn(offenderCrn = "X12348"),
    )

    val deliusStaffIdentifier = 3L
    whenever(communityApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)

    whenever(probationSearchApiClient.getOffendersByCrn(managedOffenders.map { it.offenderCrn })).thenReturn(
      listOf(
        createOffenderDetail(id = 1, crn = "X12346", nomsNumber = "AB1234D"),
        createOffenderDetail(id = 2, crn = "X12347"),
        createOffenderDetail(id = 3, crn = "X12348", nomsNumber = "AB1234E"),
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        createCaseloadItem("AB1234E", tenDaysFromNow),
      ),
    )

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload.first(),
      "X12348",
      "AB1234E",
      LicenceStatus.NOT_STARTED,
      LicenceType.PSS,
      "ACTIVE IN",
      tenDaysFromNow,
    )
  }

  @Test
  fun `It filters offenders who are ineligible for a licence`() {
    val deliusStaffIdentifier = 21L

    val managedOffenders = listOf(
      ManagedOffenderCrn(offenderCrn = "X12348"),
      ManagedOffenderCrn(offenderCrn = "X12349"),
      ManagedOffenderCrn(offenderCrn = "X12350"),
      ManagedOffenderCrn(offenderCrn = "X12351"),
      ManagedOffenderCrn(offenderCrn = "X12352"),
      ManagedOffenderCrn(offenderCrn = "X12353"),
      ManagedOffenderCrn(offenderCrn = "X12354"),
      ManagedOffenderCrn(offenderCrn = "X12355"),
      ManagedOffenderCrn(offenderCrn = "X12356"),
    )

    whenever(
      communityApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)

    whenever(probationSearchApiClient.getOffendersByCrn(managedOffenders.map { it.offenderCrn })).thenReturn(
      listOf(
        createOffenderDetail(1L, nomsNumber = "AB1234E", crn = "X12348"),
        createOffenderDetail(3L, nomsNumber = "AB1234F", crn = "X12349"),
        createOffenderDetail(id = 5L, nomsNumber = "AB1234G", crn = "X12350"),
        createOffenderDetail(id = 6L, nomsNumber = "AB1234L", crn = "X12351"),
        createOffenderDetail(id = 7L, nomsNumber = "AB1234M", crn = "X12352"),
        createOffenderDetail(id = 8L, nomsNumber = "AB1234N", crn = "X12353"),
        createOffenderDetail(id = 9L, nomsNumber = "AB1234P", crn = "X12354"),
        createOffenderDetail(id = 10L, nomsNumber = "AB1234Q", crn = "X12355"),
        createOffenderDetail(id = 11L, nomsNumber = "AB1234R", crn = "X12356"),
      ),
    )

    val caseloadItemE = createCaseloadItem("AB1234E", tenDaysFromNow)
    val caseloadItemF = createCaseloadItem("AB1234F", tenDaysFromNow)
    val caseloadItemG = createCaseloadItem("AB1234G", tenDaysFromNow)
    val caseloadItemH = createCaseloadItem("AB1234H", tenDaysFromNow)
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        caseloadItemE.copy(prisoner = caseloadItemE.prisoner.copy(paroleEligibilityDate = yesterday)),
        caseloadItemF.copy(prisoner = caseloadItemF.prisoner.copy(paroleEligibilityDate = tenDaysFromNow)),
        caseloadItemG.copy(prisoner = caseloadItemG.prisoner.copy(legalStatus = "DEAD")),
        caseloadItemH.copy(prisoner = caseloadItemH.prisoner.copy(indeterminateSentence = true)),
        createCaseloadItem("AB1234I", tenDaysFromNow),
        createCaseloadItem("AB1234J", tenDaysFromNow),
        createCaseloadItem("AB1234K", tenDaysFromNow, bookingId = "123"),
        createCaseloadItem("AB1234L", tenDaysFromNow, bookingId = "123"),
        // This case tests that recalls are overridden if the PRRD < the conditionalReleaseDate - so NOT_STARTED
        createCaseloadItem(
          "AB1234M",
          tenDaysFromNow,
          postRecallReleaseDate = nineDaysFromNow,
          recall = true,
        ),
        // This case tests that recalls are NOT overridden if the PRRD > the conditionalReleaseDate - so OOS_RECALL
        createCaseloadItem(
          prisonerNumber = "AB1234N",
          conditionalReleaseDate = tenDaysFromNow,
          postRecallReleaseDate = elevenDaysFromNow,
          recall = true,
        ),
        // This case tests that recalls are overridden if the PRRD is equal to the conditionalReleaseDate - so NOT_STARTED
        createCaseloadItem(
          prisonerNumber = "AB1234P",
          conditionalReleaseDate = nineDaysFromNow,
          postRecallReleaseDate = nineDaysFromNow,
          recall = true,
        ),
        // This case tests that recalls are overridden if no PRRD exists and there is only the conditionalReleaseDate - so NOT_STARTED
        createCaseloadItem("AB1234Q", nineDaysFromNow, recall = true),
        // This case tests that the case is included when the status is INACTIVE TRN
        createCaseloadItem("AB1234R", nineDaysFromNow, status = "INACTIVE TRN"),
      ),
    )

    whenever(prisonerSearchService.getIneligibilityReasons("AB1234F")).thenReturn(listOf("is eligible for parole"))
    whenever(prisonerSearchService.getIneligibilityReasons("AB1234G")).thenReturn(listOf("has incorrect legal status"))

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(7)
    verifyCase(
      case = caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.PSS,
      expectedConditionalReleaseDate = tenDaysFromNow,
    )
    verifyCase(
      case = caseload[1],
      expectedCrn = "X12351",
      expectedPrisonerNumber = "AB1234L",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.PSS,
      expectedConditionalReleaseDate = tenDaysFromNow,
    )
    verifyCase(
      case = caseload[2],
      expectedCrn = "X12352",
      expectedPrisonerNumber = "AB1234M",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.PSS,
      expectedConditionalReleaseDate = tenDaysFromNow,
      expectedPostRecallReleaseDate = nineDaysFromNow,
      expectedRecall = true,
    )
    assertThat(caseload[2].deliusRecord?.offenderDetail?.otherIds).isEqualTo(
      OtherIds(
        nomsNumber = "AB1234M",
        crn = "X12352",
      ),
    )
    verifyCase(
      case = caseload[3],
      expectedCrn = "X12353",
      expectedPrisonerNumber = "AB1234N",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.PSS,
      expectedConditionalReleaseDate = tenDaysFromNow,
      expectedPostRecallReleaseDate = elevenDaysFromNow,
      expectedRecall = true,
    )

    verifyCase(
      case = caseload[4],
      expectedCrn = "X12354",
      expectedPrisonerNumber = "AB1234P",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.PSS,
      expectedConditionalReleaseDate = nineDaysFromNow,
      expectedPostRecallReleaseDate = nineDaysFromNow,
      expectedRecall = true,
    )
    verifyCase(
      case = caseload[5],
      expectedCrn = "X12355",
      expectedPrisonerNumber = "AB1234Q",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.PSS,
      expectedConditionalReleaseDate = nineDaysFromNow,
      expectedRecall = true,
    )
    verifyCase(
      case = caseload[6],
      expectedCrn = "X12356",
      expectedPrisonerNumber = "AB1234R",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.PSS,
      expectedNomisStatus = "INACTIVE TRN",
      expectedConditionalReleaseDate = nineDaysFromNow,
    )
    assertThat(caseload[6].deliusRecord?.offenderDetail?.otherIds).isEqualTo(
      OtherIds(
        nomsNumber = "AB1234R",
        crn = "X12356",
      ),
    )
  }

  @Test
  fun `builds the staff create caseload`() {
    val deliusStaffIdentifier = 21L

    val managedOffenders = listOf(
      ManagedOffenderCrn(
        offenderCrn = "X12348",
        staff = StaffHuman(forenames = "Joe", surname = "Bloggs", code = "X1234"),
      ),
      ManagedOffenderCrn(
        offenderCrn = "X12349",
        staff = StaffHuman(forenames = "Joe", surname = "Bloggs", code = "X1234"),
      ),
      ManagedOffenderCrn(
        offenderCrn = "X12350",
        staff = StaffHuman(forenames = "Joe", surname = "Bloggs", code = "X1234"),
      ),
      ManagedOffenderCrn(
        offenderCrn = "X12351",
        staff = StaffHuman(forenames = null, surname = null, unallocated = true),
      ),
      ManagedOffenderCrn(
        offenderCrn = "X12352",
        staff = StaffHuman(forenames = "Joe", surname = "Bloggs", code = "X1234"),
      ),
    )

    whenever(
      communityApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)

    whenever(probationSearchApiClient.getOffendersByCrn(managedOffenders.map { it.offenderCrn })).thenReturn(
      listOf(
        createOffenderDetail(1L, nomsNumber = "AB1234E", crn = "X12348"),
        createOffenderDetail(3L, nomsNumber = "AB1234F", crn = "X12349"),
        createOffenderDetail(id = 5L, nomsNumber = "AB1234G", crn = "X12350"),
        createOffenderDetail(id = 6L, nomsNumber = "AB1234H", crn = "X12351"),
        createOffenderDetail(id = 7L, nomsNumber = "AB1234I", crn = "X12352"),
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        createCaseloadItem(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = tenDaysFromNow,
          licenceExpiryDate = LocalDate.of(
            2022,
            Month.DECEMBER,
            26,
          ),
        ),
        createCaseloadItem(
          prisonerNumber = "AB1234F",
          conditionalReleaseDate = tenDaysFromNow,
          status = "INACTIVE OUT",
        ),
        createCaseloadItem(
          prisonerNumber = "AB1234G",
          conditionalReleaseDate = tenDaysFromNow,
          status = "INACTIVE OUT",
        ),
        createCaseloadItem(
          prisonerNumber = "AB1234H",
          conditionalReleaseDate = tenDaysFromNow,
          topupSupervisionExpiryDate = LocalDate.of(2023, Month.JUNE, 22),
        ),
        createCaseloadItem(
          prisonerNumber = "AB1234I",
          conditionalReleaseDate = tenDaysFromNow,
          topupSupervisionExpiryDate = LocalDate.of(2023, Month.JUNE, 22),
          licenceExpiryDate = elevenDaysFromNow,
        ),
      ),
    )

    whenever(licenceService.findLicencesMatchingCriteria(any())).thenReturn(
      listOf(
        createLicenceSummary(
          kind = LicenceKind.CRD,
          nomisId = "AB1234I",
          licenceType = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.SUBMITTED,
          licenceExpiryDate = elevenDaysFromNow,
          comUsername = "sherlockholmes",
          conditionalReleaseDate = LocalDate.now(),
        ),
      ),
    )

    whenever(communityApiClient.getStaffDetailsByUsername(any())).thenReturn(
      listOf(
        User(
          username = "sherlockholmes",
          staffCode = "X54321",
          staff = StaffHuman(forenames = "Sherlock", surname = "Holmes"),
          teams = emptyList(),
          staffIdentifier = null,
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
      expectedConditionalReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X1234", name = "Joe Bloggs"),
      expectedLicenceExpiryDate = LocalDate.of(2022, Month.DECEMBER, 26),
    )

    assertThat(caseload.first().deliusRecord?.managedOffenderCrn).isEqualTo(
      ManagedOffenderCrn(
        offenderCrn = "X12348",
        staff = StaffHuman(forenames = "Joe", surname = "Bloggs", code = "X1234"),
      ),
    )

    verifyCase(
      case = caseload[1],
      expectedCrn = "X12351",
      expectedPrisonerNumber = "AB1234H",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.PSS,
      expectedConditionalReleaseDate = tenDaysFromNow,
    )
    verifyCase(
      case = caseload[2],
      expectedCrn = "X12352",
      expectedPrisonerNumber = "AB1234I",
      expectedLicenceStatus = LicenceStatus.SUBMITTED,
      expectedLicenceType = LicenceType.AP_PSS,
      expectedConditionalReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X54321",
        name = "Sherlock Holmes",
      ),
      expectedComUsername = "sherlockholmes",
      expectedLicenceExpiryDate = elevenDaysFromNow,
    )
  }

  @Test
  fun `it builds the team create caseload`() {
    val selectedTeam = "team c"

    val managedOffenders = listOf(
      ManagedOffenderCrn(
        offenderCrn = "X12348",
        staff = StaffHuman(forenames = "Joe", surname = "Bloggs", code = "X1234"),
      ),
      ManagedOffenderCrn(
        offenderCrn = "X12349",
        staff = StaffHuman(forenames = "Sherlock", surname = "Holmes", code = "X54321"),
      ),
    )

    whenever(
      communityApiClient.getManagedOffendersByTeam(selectedTeam),
    ).thenReturn(managedOffenders)

    whenever(probationSearchApiClient.getOffendersByCrn(managedOffenders.map { it.offenderCrn })).thenReturn(
      listOf(
        createOffenderDetail(1L, nomsNumber = "AB1234E", crn = "X12348"),
        createOffenderDetail(3L, nomsNumber = "AB1234F", crn = "X12349"),
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        createCaseloadItem(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = tenDaysFromNow,
          licenceExpiryDate = LocalDate.of(2022, Month.DECEMBER, 26),
          status = "ACTIVE IN",
        ),
        createCaseloadItem(prisonerNumber = "AB1234F", conditionalReleaseDate = tenDaysFromNow),
      ),
    )

    val caseload = service.getTeamCreateCaseload(listOf("team A", "team B"), listOf(selectedTeam))

    verify(communityApiClient).getManagedOffendersByTeam(selectedTeam)
    assertThat(caseload).hasSize(2)
    verifyCase(
      case = caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.AP,
      expectedConditionalReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X1234",
        name = "Joe Bloggs",
      ),
      expectedLicenceExpiryDate = LocalDate.of(2022, Month.DECEMBER, 26),
    )

    verifyCase(
      case = caseload[1],
      expectedCrn = "X12349",
      expectedPrisonerNumber = "AB1234F",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.PSS,
      expectedConditionalReleaseDate = tenDaysFromNow,
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X54321",
        name = "Sherlock Holmes",
      ),
    )
  }

  @Test
  fun `check licence status for recalls and breach of supervision on team create caseload`() {
    val selectedTeam = "team C"

    val managedOffenders = listOf(
      ManagedOffenderCrn(
        offenderCrn = "X12348",
        staff = StaffHuman(forenames = "Joe", surname = "Bloggs", code = "X1234"),
      ),
      ManagedOffenderCrn(
        offenderCrn = "X12349",
        staff = StaffHuman(forenames = "Sherlock", surname = "Holmes", code = "X54321"),
      ),
    )

    whenever(
      communityApiClient.getManagedOffendersByTeam(selectedTeam),
    ).thenReturn(managedOffenders)

    whenever(probationSearchApiClient.getOffendersByCrn(managedOffenders.map { it.offenderCrn })).thenReturn(
      listOf(
        createOffenderDetail(1L, nomsNumber = "AB1234E", crn = "X12348"),
        createOffenderDetail(3L, nomsNumber = "AB1234F", crn = "X12349"),
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        createCaseloadItem(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = tenDaysFromNow,
          releaseDate = tenDaysFromNow,
          postRecallReleaseDate = tenDaysFromNow,
          recall = true,
        ),
        createCaseloadItem(
          prisonerNumber = "AB1234F",
          conditionalReleaseDate = tenDaysFromNow,
          releaseDate = tenDaysFromNow,
          imprisonmentStatus = "BOTUS",
          recall = true,
        ),
      ),
    )

    val caseload = service.getTeamCreateCaseload(listOf("team A", "team B"), listOf("team C"))

    verify(communityApiClient).getManagedOffendersByTeam("team C")
    assertThat(caseload).hasSize(2)
    verifyCase(
      case = caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.PSS,
      expectedConditionalReleaseDate = tenDaysFromNow,
      expectedReleaseDate = tenDaysFromNow,
      expectedPostRecallReleaseDate = tenDaysFromNow,
      expectedRecall = true,
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X1234", name = "Joe Bloggs"),
    )

    verifyCase(
      case = caseload[1],
      expectedCrn = "X12349",
      expectedPrisonerNumber = "AB1234F",
      expectedLicenceStatus = LicenceStatus.NOT_STARTED,
      expectedLicenceType = LicenceType.PSS,
      expectedConditionalReleaseDate = tenDaysFromNow,
      expectedReleaseDate = tenDaysFromNow,
      expectedRecall = true,
      expectedImprisonmentStatus = "BOTUS",
      expectedProbationPractitioner = ProbationPractitioner(
        staffCode = "X54321",
        name = "Sherlock Holmes",
      ),
    )
    verify(communityApiClient).getManagedOffendersByTeam("team C")
  }

  @Test
  fun `it builds the staff vary caseload`() {
    val managedOffenders = listOf(
      // ManagedOffenderCrn(offenderCrn = "X12348", staff = ),
      ManagedOffenderCrn(
        offenderCrn = "X12348",
        staff = StaffHuman(code = "X1234", forenames = "Joe", surname = "Bloggs"),
      ),
    )

    val deliusStaffIdentifier = 3L
    whenever(communityApiClient.getManagedOffenders(deliusStaffIdentifier)).thenReturn(managedOffenders)

    whenever(
      probationSearchApiClient.getOffendersByCrn(
        managedOffenders.map
          { it.offenderCrn },
      ),
    ).thenReturn(
      listOf(
        createOffenderDetail(1L, nomsNumber = "AB1234E", crn = "X12348"),
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        createCaseloadItem(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = null,
          confirmedReleaseDate = tenDaysFromNow,
          licenceExpiryDate = LocalDate.of(
            2022,
            Month.DECEMBER,
            26,
          ),
          status = "INACTIVE OUT",
        ),
      ),
    )

    whenever(licenceService.findLicencesMatchingCriteria(any())).thenReturn(
      listOf(
        createLicenceSummary(
          kind = LicenceKind.CRD,
          nomisId = "AB1234E",
          licenceType = LicenceType.AP,
          licenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
          comUsername = "sherlockholmes",
        ),
      ),
    )

    whenever(communityApiClient.getStaffDetailsByUsername(any())).thenReturn(
      listOf(
        User(
          username = "sherlockholmes",
          staffCode = "X54321",
          staff = StaffHuman(forenames = "Sherlock", surname = "Holmes"),
          teams = emptyList(),
          staffIdentifier = null,
        ),
      ),
    )

    val caseload = service.getStaffVaryCaseload(deliusStaffIdentifier)
    assertThat(caseload).hasSize(1)
    verifyCase(
      caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedLicenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
      expectedLicenceType = LicenceType.AP,
      expectedConditionalReleaseDate = null,
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X54321", name = "Sherlock Holmes"),
      expectedConfirmedReleaseDate = tenDaysFromNow,
      expectedNomisStatus = "INACTIVE OUT",
      expectedLicenceExpiryDate = LocalDate.of(2022, Month.DECEMBER, 26),
      expectedComUsername = "sherlockholmes",
    )
  }

  @Test
  fun `it builds the staff vary caseload with Review Needed status`() {
    val deliusStaffIdentifier = 21L

    val managedOffenders = listOf(
      ManagedOffenderCrn(
        offenderCrn = "X12348",
        staff = StaffHuman(forenames = "Joe", surname = "Bloggs", code = "X1234"),
      ),
    )

    whenever(
      communityApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)
    whenever(probationSearchApiClient.getOffendersByCrn(managedOffenders.map { it.offenderCrn })).thenReturn(
      listOf(
        createOffenderDetail(1L, nomsNumber = "AB1234E", crn = "X12348"),
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        createCaseloadItem(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = null,
          confirmedReleaseDate = tenDaysFromNow,
          licenceExpiryDate = LocalDate.of(
            2022,
            Month.DECEMBER,
            26,
          ),
          status = "INACTIVE OUT",
        ),
      ),
    )

    whenever(licenceService.findLicencesMatchingCriteria(any())).thenReturn(
      listOf(
        createLicenceSummary(
          kind = LicenceKind.HARD_STOP,
          nomisId = "AB1234E",
          licenceType = LicenceType.AP,
          licenceStatus = LicenceStatus.ACTIVE,
          comUsername = "sherlockholmes",
        ),
      ),
    )

    whenever(communityApiClient.getStaffDetailsByUsername(any())).thenReturn(
      listOf(
        User(
          username = "sherlockholmes",
          staffCode = "X54321",
          staff = StaffHuman(forenames = "Sherlock", surname = "Holmes"),
          teams = emptyList(),
          staffIdentifier = null,
        ),
      ),
    )

    val caseload = service.getStaffVaryCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    // TODO : status should be REVIEW_NEEDED
    verifyCase(
      caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedNomisStatus = "INACTIVE OUT",
      expectedLicenceStatus = LicenceStatus.ACTIVE,
      expectedLicenceType = LicenceType.AP,
      expectedConfirmedReleaseDate = tenDaysFromNow,
      expectedLicenceExpiryDate = LocalDate.of(2022, Month.DECEMBER, 26),
      expectedComUsername = "sherlockholmes",
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X54321", name = "Sherlock Holmes"),
    )
  }

  @Test
  fun `builds the team vary caseload`() {
    val selectedTeam = "team C"

    val managedOffenders = listOf(
      ManagedOffenderCrn(
        offenderCrn = "X12348",
        staff = StaffHuman(forenames = "Joe", surname = "Bloggs", code = "X1234"),
      ),
      ManagedOffenderCrn(
        offenderCrn = "X12349",
        staff = StaffHuman(forenames = "Sherlock", surname = "Holmes", code = "X54321"),
      ),
    )

    whenever(
      communityApiClient.getManagedOffendersByTeam(selectedTeam),
    ).thenReturn(managedOffenders)

    whenever(probationSearchApiClient.getOffendersByCrn(managedOffenders.map { it.offenderCrn })).thenReturn(
      listOf(
        createOffenderDetail(1L, nomsNumber = "AB1234E", crn = "X12348"),
        createOffenderDetail(3L, nomsNumber = "AB1234F", crn = "X12349"),
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        createCaseloadItem(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = null,
          confirmedReleaseDate = tenDaysFromNow,
          status = "INACTIVE OUT",
        ),
        createCaseloadItem(
          prisonerNumber = "AB1234F",
          conditionalReleaseDate = null,
          confirmedReleaseDate = tenDaysFromNow,
          status = "INACTIVE OUT",
          licenceExpiryDate = LocalDate.of(2022, Month.DECEMBER, 26),
        ),
      ),
    )

    whenever(licenceService.findLicencesMatchingCriteria(any())).thenReturn(
      listOf(
        createLicenceSummary(
          kind = LicenceKind.VARIATION,
          nomisId = "AB1234E",
          licenceType = LicenceType.PSS,
          licenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
          licenceExpiryDate = elevenDaysFromNow,
          comUsername = "joebloggs",
          conditionalReleaseDate = LocalDate.now(),
        ),
        createLicenceSummary(
          kind = LicenceKind.VARIATION,
          nomisId = "AB1234F",
          licenceType = LicenceType.AP,
          licenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
          licenceExpiryDate = elevenDaysFromNow,
          comUsername = "sherlockholmes",
          conditionalReleaseDate = LocalDate.now(),
        ),
      ),
    )

    whenever(communityApiClient.getStaffDetailsByUsername(any())).thenReturn(
      listOf(
        User(
          username = "sherlockholmes",
          staffCode = "X54321",
          staff = StaffHuman(forenames = "Sherlock", surname = "Holmes"),
          teams = emptyList(),
          staffIdentifier = null,
        ),
        User(
          username = "joebloggs",
          staffCode = "X1234",
          staff = StaffHuman(forenames = "Joe", surname = "Bloggs"),
          teams = emptyList(),
          staffIdentifier = null,
        ),
      ),
    )

    val caseload = service.getTeamVaryCaseload(listOf("team A", "team B"), listOf(selectedTeam))

    verify(communityApiClient).getManagedOffendersByTeam("team C")
    assertThat(caseload).hasSize(2)
    verifyCase(
      caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedNomisStatus = "INACTIVE OUT",
      expectedConfirmedReleaseDate = tenDaysFromNow,
      expectedLicenceType = LicenceType.PSS,
      expectedLicenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
      expectedComUsername = "joebloggs",
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X1234", name = "Joe Bloggs"),
    )
    verifyCase(
      caseload[1],
      expectedCrn = "X12349",
      expectedPrisonerNumber = "AB1234F",
      expectedNomisStatus = "INACTIVE OUT",
      expectedConfirmedReleaseDate = tenDaysFromNow,
      expectedLicenceExpiryDate = LocalDate.of(2022, Month.DECEMBER, 26),
      expectedLicenceType = LicenceType.AP,
      expectedLicenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
      expectedComUsername = "sherlockholmes",
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X54321", name = "Sherlock Holmes"),
    )
  }

  @Test
  fun `builds the team vary caseload with Review Needed status`() {
    val deliusStaffIdentifier = 3L

    val managedOffenders = listOf(
      ManagedOffenderCrn(
        offenderCrn = "X12348",
        staff = StaffHuman(forenames = "Joe", surname = "Bloggs", code = "X1234"),
      ),
    )

    whenever(
      communityApiClient.getManagedOffenders(deliusStaffIdentifier),
    ).thenReturn(managedOffenders)

    whenever(probationSearchApiClient.getOffendersByCrn(managedOffenders.map { it.offenderCrn })).thenReturn(
      listOf(
        createOffenderDetail(1L, nomsNumber = "AB1234E", crn = "X12348"),
      ),
    )

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        createCaseloadItem(
          prisonerNumber = "AB1234E",
          conditionalReleaseDate = null,
          confirmedReleaseDate = tenDaysFromNow,
          licenceExpiryDate = LocalDate.of(2022, Month.DECEMBER, 26),
          status = "INACTIVE OUT",
        ),
        createCaseloadItem(
          prisonerNumber = "AB1234F",
          conditionalReleaseDate = null,
          confirmedReleaseDate = tenDaysFromNow,
          status = "INACTIVE OUT",
          licenceExpiryDate = LocalDate.of(2022, Month.DECEMBER, 26),
        ),
      ),
    )

    whenever(licenceService.findLicencesMatchingCriteria(any())).thenReturn(
      listOf(
        createLicenceSummary(
          kind = LicenceKind.HARD_STOP,
          nomisId = "AB1234E",
          licenceType = LicenceType.AP,
          licenceStatus = LicenceStatus.ACTIVE,
          licenceExpiryDate = elevenDaysFromNow,
          comUsername = "sherlockholmes",
          conditionalReleaseDate = LocalDate.now(),
          isReviewNeeded = true,
        ),
      ),
    )

    whenever(communityApiClient.getStaffDetailsByUsername(any())).thenReturn(
      listOf(
        User(
          username = "sherlockholmes",
          staffCode = "X54321",
          staff = StaffHuman(forenames = "Sherlock", surname = "Holmes"),
          teams = emptyList(),
          staffIdentifier = null,
        ),
      ),
    )

    val caseload = service.getStaffVaryCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    //  TODO: status should be REVIEW_NEEDED
    verifyCase(
      caseload[0],
      expectedCrn = "X12348",
      expectedPrisonerNumber = "AB1234E",
      expectedNomisStatus = "INACTIVE OUT",
      expectedConfirmedReleaseDate = tenDaysFromNow,
      expectedLicenceType = LicenceType.AP,
      expectedLicenceStatus = LicenceStatus.ACTIVE,
      expectedLicenceExpiryDate = LocalDate.of(2022, Month.DECEMBER, 26),
      expectedComUsername = "sherlockholmes",
      expectedProbationPractitioner = ProbationPractitioner(staffCode = "X54321", name = "Sherlock Holmes"),
    )
  }

  @Test
  fun `batches calls to the community CRN endpoint`() {
    val selectedTeam = "team C"

    val managedOffenders = Array(600) {
      ManagedOffenderCrn(
        offenderCrn = "X12348",
        staff = StaffHuman(forenames = "Joe", surname = "Bloggs", code = "X1234"),
      )
    }.asList()

    whenever(
      communityApiClient.getManagedOffendersByTeam(selectedTeam),
    ).thenReturn(managedOffenders)

    service.getTeamVaryCaseload(listOf("team A", "team B"), listOf(selectedTeam))
    verify(communityApiClient).getManagedOffendersByTeam(selectedTeam)
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
  ): CaseloadItem = CaseloadItem(
    prisoner = Prisoner(
      prisonerNumber = prisonerNumber,
      conditionalReleaseDate = conditionalReleaseDate,
      status = status,
      bookingId = bookingId,
      postRecallReleaseDate = postRecallReleaseDate,
      recall = recall,
      topupSupervisionExpiryDate = topupSupervisionExpiryDate,
      licenceExpiryDate = licenceExpiryDate,
      releaseDate = releaseDate,
      imprisonmentStatus = imprisonmentStatus,
      confirmedReleaseDate = confirmedReleaseDate,
    ),
    cvl = CvlFields(licenceType = LicenceType.PSS),
  )

  private fun verifyCase(
    case: ManagedCase,
    expectedCrn: String,
    expectedPrisonerNumber: String,
    expectedLicenceStatus: LicenceStatus,
    expectedLicenceType: LicenceType,
    expectedNomisStatus: String = "ACTIVE IN",
    expectedConditionalReleaseDate: LocalDate? = null,
    expectedPostRecallReleaseDate: LocalDate? = null,
    expectedReleaseDate: LocalDate? = null,
    expectedRecall: Boolean = false,
    expectedProbationPractitioner: ProbationPractitioner? = null,
    expectedImprisonmentStatus: String? = null,
    expectedConfirmedReleaseDate: LocalDate? = null,
    expectedLicenceExpiryDate: LocalDate? = null,
    expectedComUsername: String? = null,
  ) {
    with(case) {
      val licence = licences?.get(0)
      assertThat(deliusRecord?.managedOffenderCrn?.offenderCrn).isEqualTo(expectedCrn)
      assertThat(nomisRecord?.prisonerNumber).isEqualTo(expectedPrisonerNumber)
      assertThat(nomisRecord?.status).isEqualTo(expectedNomisStatus)
      assertThat(licence?.licenceStatus).isEqualTo(expectedLicenceStatus)
      assertThat(licence?.licenceType).isEqualTo(expectedLicenceType)
      expectedConditionalReleaseDate.let {
        assertThat(nomisRecord?.conditionalReleaseDate).isEqualTo(
          expectedConditionalReleaseDate,
        )
      }
      expectedPostRecallReleaseDate.let {
        assertThat(nomisRecord?.postRecallReleaseDate).isEqualTo(
          expectedPostRecallReleaseDate,
        )
      }
      expectedReleaseDate.let { assertThat(nomisRecord?.releaseDate).isEqualTo(expectedReleaseDate) }
      assertThat(nomisRecord?.recall).isEqualTo(expectedRecall)
      expectedProbationPractitioner.let { assertThat(probationPractitioner).isEqualTo(expectedProbationPractitioner) }
      expectedImprisonmentStatus.let { assertThat(nomisRecord?.imprisonmentStatus).isEqualTo(expectedImprisonmentStatus) }
      expectedConfirmedReleaseDate.let {
        assertThat(nomisRecord?.confirmedReleaseDate).isEqualTo(
          expectedConfirmedReleaseDate,
        )
      }
      expectedLicenceExpiryDate.let { assertThat(nomisRecord?.licenceExpiryDate).isEqualTo(expectedLicenceExpiryDate) }
      expectedComUsername.let { assertThat(licence?.comUsername).isEqualTo(expectedComUsername) }
    }
  }

  private fun createOffenderDetail(id: Long, crn: String, nomsNumber: String? = null) = OffenderDetail(
    offenderId = id,
    OtherIds(nomsNumber = nomsNumber, crn = crn),
    offenderManagers = emptyList(),
  )

  private fun createLicenceSummary(
    nomisId: String,
    licenceType: LicenceType,
    licenceStatus: LicenceStatus,
    kind: LicenceKind = LicenceKind.CRD,
    licenceExpiryDate: LocalDate? = null,
    comUsername: String? = null,
    conditionalReleaseDate: LocalDate? = LocalDate.now(),
    isReviewNeeded: Boolean = false,
  ): LicenceSummary = LicenceSummary(
    kind = kind,
    nomisId = nomisId,
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
    dateCreated = LocalDateTime.now(),
    updatedByFullName = "X Y",
    actualReleaseDate = null,
    bookingId = null,
    crn = null,
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
