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
        createManagedOffenderCrn(id = 1, crn = "X12346", nomsNumber = "AB1234D"),
        createManagedOffenderCrn(id = 2, crn = "X12347"),
        createManagedOffenderCrn(id = 3, crn = "X12348", nomsNumber = "AB1234E"),
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
        createManagedOffenderCrn(1L, nomsNumber = "AB1234E", crn = "X12348"),
        createManagedOffenderCrn(3L, nomsNumber = "AB1234F", crn = "X12349"),
        createManagedOffenderCrn(id = 5L, nomsNumber = "AB1234G", crn = "X12350"),
        createManagedOffenderCrn(id = 6L, nomsNumber = "AB1234L", crn = "X12351"),
        createManagedOffenderCrn(id = 7L, nomsNumber = "AB1234M", crn = "X12352"),
        createManagedOffenderCrn(id = 8L, nomsNumber = "AB1234N", crn = "X12353"),
        createManagedOffenderCrn(id = 9L, nomsNumber = "AB1234P", crn = "X12354"),
        createManagedOffenderCrn(id = 10L, nomsNumber = "AB1234Q", crn = "X12355"),
        createManagedOffenderCrn(id = 11L, nomsNumber = "AB1234R", crn = "X12356"),
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
          "AB1234N",
          tenDaysFromNow,
          postRecallReleaseDate = elevenDaysFromNow,
          recall = true,
        ),
        // This case tests that recalls are overridden if the PRRD is equal to the conditionalReleaseDate - so NOT_STARTED
        createCaseloadItem(
          "AB1234P",
          nineDaysFromNow,
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
      caseload[0],
      "X12348",
      "AB1234E",
      LicenceStatus.NOT_STARTED,
      LicenceType.PSS,
      "ACTIVE IN",
      tenDaysFromNow,
    )
    verifyCase(
      caseload[1],
      "X12351",
      "AB1234L",
      LicenceStatus.NOT_STARTED,
      LicenceType.PSS,
      "ACTIVE IN",
      tenDaysFromNow,
    )
    verifyCase(
      caseload[2],
      "X12352",
      "AB1234M",
      LicenceStatus.NOT_STARTED,
      LicenceType.PSS,
      "ACTIVE IN",
      tenDaysFromNow,
    )
    assertThat(caseload[2].deliusRecord?.offenderDetail?.otherIds).isEqualTo(
      OtherIds(
        nomsNumber = "AB1234M",
        crn = "X12352",
      ),
    )
    assertThat(caseload[2].nomisRecord?.postRecallReleaseDate).isEqualTo(nineDaysFromNow)
    assertThat(caseload[2].nomisRecord?.recall).isTrue()
    verifyCase(
      caseload[3],
      "X12353",
      "AB1234N",
      LicenceStatus.NOT_STARTED,
      LicenceType.PSS,
      "ACTIVE IN",
      tenDaysFromNow,
    )
    assertThat(caseload[3].nomisRecord?.postRecallReleaseDate).isEqualTo(elevenDaysFromNow)
    assertThat(caseload[3].nomisRecord?.recall).isTrue()
    verifyCase(
      caseload[4],
      "X12354",
      "AB1234P",
      LicenceStatus.NOT_STARTED,
      LicenceType.PSS,
      "ACTIVE IN",
      nineDaysFromNow,
    )
    assertThat(caseload[4].nomisRecord?.postRecallReleaseDate).isEqualTo(nineDaysFromNow)
    assertThat(caseload[4].nomisRecord?.recall).isTrue()
    verifyCase(
      caseload[5],
      "X12355",
      "AB1234Q",
      LicenceStatus.NOT_STARTED,
      LicenceType.PSS,
      "ACTIVE IN",
      nineDaysFromNow,
    )
    assertThat(caseload[5].nomisRecord?.recall).isTrue()
    verifyCase(
      caseload[6],
      "X12356",
      "AB1234R",
      LicenceStatus.NOT_STARTED,
      LicenceType.PSS,
      "INACTIVE TRN",
      nineDaysFromNow,
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
        createManagedOffenderCrn(1L, nomsNumber = "AB1234E", crn = "X12348"),
        createManagedOffenderCrn(3L, nomsNumber = "AB1234F", crn = "X12349"),
        createManagedOffenderCrn(id = 5L, nomsNumber = "AB1234G", crn = "X12350"),
        createManagedOffenderCrn(id = 6L, nomsNumber = "AB1234H", crn = "X12351"),
        createManagedOffenderCrn(id = 7L, nomsNumber = "AB1234I", crn = "X12352"),
      ),
    )

    val caseloadItemE = createCaseloadItem("AB1234E", tenDaysFromNow)
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        caseloadItemE.copy(
          prisoner = caseloadItemE.prisoner.copy(
            licenceExpiryDate = LocalDate.of(
              2022,
              Month.DECEMBER,
              26,
            ),
          ),
        ),
        createCaseloadItem("AB1234F", tenDaysFromNow, status = "INACTIVE OUT"),
        createCaseloadItem("AB1234G", tenDaysFromNow, status = "INACTIVE OUT"),
        createCaseloadItem("AB1234H", tenDaysFromNow, topupSupervisionExpiryDate = LocalDate.of(2023, Month.JUNE, 22)),
        createCaseloadItem("AB1234I", tenDaysFromNow, topupSupervisionExpiryDate = LocalDate.of(2023, Month.JUNE, 22)),
      ),
    )

    whenever(licenceService.findLicencesMatchingCriteria(any())).thenReturn(
      listOf(
        LicenceSummary(
          kind = LicenceKind.CRD,
          nomisId = "AB1234I",
          licenceId = 1,
          licenceType = LicenceType.AP_PSS,
          licenceStatus = LicenceStatus.SUBMITTED,
          comUsername = "sherlockholmes",
          isReviewNeeded = false,
          isDueForEarlyRelease = false,
          isInHardStopPeriod = false,
          isDueToBeReleasedInTheNextTwoWorkingDays = false,
          conditionalReleaseDate = LocalDate.now(),
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
      caseload[0],
      "X12348",
      "AB1234E",
      LicenceStatus.NOT_STARTED,
      LicenceType.AP,
      "ACTIVE IN",
      tenDaysFromNow,
    )

    assertThat(caseload.first().deliusRecord?.managedOffenderCrn).isEqualTo(
      ManagedOffenderCrn(
        offenderCrn = "X12348",
        staff = StaffHuman(forenames = "Joe", surname = "Bloggs", code = "X1234"),
      ),
    )
    assertThat(caseload.first().probationPractitioner?.staffCode).isEqualTo("X1234")

    verifyCase(
      caseload[1],
      "X12351",
      "AB1234H",
      LicenceStatus.NOT_STARTED,
      LicenceType.PSS,
      "ACTIVE IN",
      tenDaysFromNow,
    )
    verifyCase(
      caseload[2],
      "X12352",
      "AB1234I",
      LicenceStatus.SUBMITTED,
      LicenceType.AP_PSS,
      "ACTIVE IN",
      tenDaysFromNow,
    )
    assertThat(caseload[2].probationPractitioner).isEqualTo(
      ProbationPractitioner(
        staffCode = "X54321",
        name = "Sherlock Holmes",
      ),
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
        createManagedOffenderCrn(1L, nomsNumber = "AB1234E", crn = "X12348"),
        createManagedOffenderCrn(3L, nomsNumber = "AB1234F", crn = "X12349"),
      ),
    )
    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        CaseloadItem(
          prisoner = Prisoner(
            prisonerNumber = "AB1234E",
            conditionalReleaseDate = tenDaysFromNow,
            licenceExpiryDate = LocalDate.of(2022, Month.DECEMBER, 26),
            status = "ACTIVE IN",
          ),
          cvl = CvlFields(licenceType = LicenceType.PSS),
        ),
        CaseloadItem(
          prisoner = Prisoner(
            prisonerNumber = "AB1234F",
            conditionalReleaseDate = tenDaysFromNow,
            status = "ACTIVE IN",
          ),
          cvl = CvlFields(licenceType = LicenceType.PSS),
        ),
      ),
    )

    val caseload = service.getTeamCreateCaseload(listOf("team A", "team B"), listOf(selectedTeam))

    verify(communityApiClient).getManagedOffendersByTeam(selectedTeam)
    assertThat(caseload).hasSize(2)
    verifyCase(caseload[0], "X12348", "AB1234E", LicenceStatus.NOT_STARTED, LicenceType.AP, "ACTIVE IN", tenDaysFromNow)
    assertThat(caseload[0].probationPractitioner).isEqualTo(
      ProbationPractitioner(
        staffCode = "X1234",
        name = "Joe Bloggs",
      ),
    )

    verifyCase(
      caseload[1],
      "X12349",
      "AB1234F",
      LicenceStatus.NOT_STARTED,
      LicenceType.PSS,
      "ACTIVE IN",
      tenDaysFromNow,
    )
    assertThat(caseload[1].probationPractitioner).isEqualTo(
      ProbationPractitioner(
        staffCode = "X54321",
        name = "Sherlock Holmes",
      ),
    )
  }

  private fun verifyCase(
    case: ManagedCase,
    crn: String,
    prisonerNumber: String,
    licenceStatus: LicenceStatus,
    licenceType: LicenceType,
    nomisStatus: String,
    conditionalReleaseDate: LocalDate? = null,
  ) {
    with(case) {
      assertThat(deliusRecord?.managedOffenderCrn?.offenderCrn).isEqualTo(crn)
      assertThat(nomisRecord?.prisonerNumber).isEqualTo(prisonerNumber)
      assertThat(nomisRecord?.status).isEqualTo(nomisStatus)
      assertThat(licences?.get(0)?.licenceStatus).isEqualTo(licenceStatus)
      assertThat(licences?.get(0)?.licenceType).isEqualTo(licenceType)
      if (conditionalReleaseDate != null) {
        assertThat(nomisRecord?.conditionalReleaseDate).isEqualTo(conditionalReleaseDate)
      }
    }
  }

  private fun createCaseloadItem(
    prisonerNumber: String,
    conditionalReleaseDate: LocalDate?,
    status: String = "ACTIVE IN",
    postRecallReleaseDate: LocalDate? = null,
    recall: Boolean = false,
    bookingId: String? = null,
    topupSupervisionExpiryDate: LocalDate? = null,
  ): CaseloadItem = CaseloadItem(
    prisoner = Prisoner(
      prisonerNumber = prisonerNumber,
      conditionalReleaseDate = conditionalReleaseDate,
      status = status,
      bookingId = bookingId,
      postRecallReleaseDate = postRecallReleaseDate,
      recall = recall,
      topupSupervisionExpiryDate = topupSupervisionExpiryDate,
    ),
    cvl = CvlFields(licenceType = LicenceType.PSS),
  )

  private fun createManagedOffenderCrn(id: Long, crn: String, nomsNumber: String? = null) = OffenderDetail(
    offenderId = id,
    OtherIds(nomsNumber = nomsNumber, crn = crn),
    offenderManagers = emptyList(),
  )
}
