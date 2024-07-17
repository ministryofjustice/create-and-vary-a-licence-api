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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ManagedCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonerSearchService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.StaffService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OffenderDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OtherIds
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
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

  val elevenDaysFromNow = LocalDate.now().plusDays(11)
  val tenDaysFromNow = LocalDate.now().plusDays(10)
  val nineDaysFromNow = LocalDate.now().plusDays(9)
  val yesterday = LocalDate.now().minusDays(1)

  @BeforeEach
  fun reset() {
    reset(communityApiClient, licenceService, prisonerSearchService)
  }

  @Test
  fun `Does not call Licence API when no Nomis records are found`() {
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
  fun `'it filters invalid data due to mismatch between delius and nomis`() {
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
        CaseloadItem(
          prisoner = Prisoner(
            prisonerNumber = "AB1234E",
            conditionalReleaseDate = tenDaysFromNow,
            status = "ACTIVE IN",
          ),
          cvl = CvlFields(licenceType = LicenceType.PSS),
        ),
      ),
    )
    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    assertThat(caseload).hasSize(1)
    with(caseload.first()) {
      assertThat(deliusRecord?.managedOffenderCrn?.offenderCrn).isEqualTo("X12348")
      assertThat(nomisRecord?.prisonerNumber).isEqualTo("AB1234E")
      assertThat(nomisRecord?.conditionalReleaseDate).isEqualTo(tenDaysFromNow)
      assertThat(licences?.get(0)?.licenceStatus).isEqualTo(LicenceStatus.NOT_STARTED)
      assertThat(licences?.get(0)?.licenceType).isEqualTo(LicenceType.PSS)
    }
  }

  @Test
  fun `filters offenders who are ineligible for a licence`() {
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

    whenever(caseloadService.getPrisonersByNumber(any())).thenReturn(
      listOf(
        CaseloadItem(
          prisoner = Prisoner(
            prisonerNumber = "AB1234E",
            conditionalReleaseDate = tenDaysFromNow,
            paroleEligibilityDate = yesterday,
            status = "ACTIVE IN",
          ),
          cvl = CvlFields(licenceType = LicenceType.PSS),
        ),
        CaseloadItem(
          prisoner = Prisoner(
            prisonerNumber = "AB1234F",
            paroleEligibilityDate = tenDaysFromNow,
          ),
          cvl = CvlFields(licenceType = LicenceType.PSS),
        ),
        CaseloadItem(
          prisoner = Prisoner(
            prisonerNumber = "AB1234G",
            legalStatus = "DEAD",
          ),
          cvl = CvlFields(licenceType = LicenceType.PSS),
        ),
        CaseloadItem(
          prisoner = Prisoner(
            prisonerNumber = "AB1234H",
            indeterminateSentence = true,
          ),
          cvl = CvlFields(licenceType = LicenceType.PSS),
        ),
        CaseloadItem(
          prisoner = Prisoner(
            prisonerNumber = "AB1234I",
          ),
          cvl = CvlFields(licenceType = LicenceType.PSS),
        ),
        CaseloadItem(
          prisoner = Prisoner(
            prisonerNumber = "AB1234J",
            conditionalReleaseDate = tenDaysFromNow,
          ),
          cvl = CvlFields(licenceType = LicenceType.PSS),
        ),
        CaseloadItem(
          prisoner = Prisoner(
            prisonerNumber = "AB1234K",
            conditionalReleaseDate = tenDaysFromNow,
            bookingId = "123",
          ),
          cvl = CvlFields(licenceType = LicenceType.PSS),
        ),
        CaseloadItem(
          prisoner = Prisoner(
            prisonerNumber = "AB1234L",
            conditionalReleaseDate = tenDaysFromNow,
            status = "ACTIVE IN",
          ),
          cvl = CvlFields(licenceType = LicenceType.PSS),
        ),
        // This case tests that recalls are overridden if the PRRD < the conditionalReleaseDate - so NOT_STARTED
        CaseloadItem(
          prisoner = Prisoner(
            prisonerNumber = "AB1234M",
            conditionalReleaseDate = tenDaysFromNow,
            postRecallReleaseDate = nineDaysFromNow,
            status = "ACTIVE IN",
            recall = true,
          ),
          cvl = CvlFields(licenceType = LicenceType.PSS),
        ),
        // This case tests that recalls are NOT overridden if the PRRD > the conditionalReleaseDate - so OOS_RECALL
        CaseloadItem(
          prisoner = Prisoner(
            prisonerNumber = "AB1234N",
            conditionalReleaseDate = tenDaysFromNow,
            postRecallReleaseDate = elevenDaysFromNow,
            status = "ACTIVE IN",
            recall = true,
          ),
          cvl = CvlFields(licenceType = LicenceType.PSS),
        ),
        // This case tests that recalls are overridden if the PRRD is equal to the conditionalReleaseDate - so NOT_STARTED
        CaseloadItem(
          prisoner = Prisoner(
            prisonerNumber = "AB1234P",
            conditionalReleaseDate = nineDaysFromNow,
            postRecallReleaseDate = nineDaysFromNow,
            status = "ACTIVE IN",
            recall = true,
          ),
          cvl = CvlFields(licenceType = LicenceType.PSS),
        ),
        // This case tests that recalls are overridden if no PRRD exists and there is only the conditionalReleaseDate - so NOT_STARTED
        CaseloadItem(
          prisoner = Prisoner(
            prisonerNumber = "AB1234Q",
            conditionalReleaseDate = nineDaysFromNow,
            status = "ACTIVE IN",
            recall = true,
          ),
          cvl = CvlFields(licenceType = LicenceType.PSS),
        ),
        // This case tests that the case is included when the status is INACTIVE TRN
        CaseloadItem(
          prisoner = Prisoner(
            prisonerNumber = "AB1234R",
            conditionalReleaseDate = nineDaysFromNow,
            status = "INACTIVE TRN",
          ),
          cvl = CvlFields(licenceType = LicenceType.PSS),
        ),
      ),
    )

    whenever(prisonerSearchService.getIneligibilityReasons("AB1234K")).thenReturn(listOf("Approved for HDC"))

    val caseload = service.getStaffCreateCaseload(deliusStaffIdentifier)

    val nomsNumbers = caseload.map { it.nomisRecord?.prisonerNumber }
    println(nomsNumbers)
    assertThat(caseload).hasSize(7)
//   [
//     {
//       deliusRecord: { offenderCrn: 'X12348',
//     },
//       nomisRecord: { prisonerNumber: 'AB1234E',
//       conditionalReleaseDate: tenDaysFromNow,
//     },
//       licences: [
//       { status: 'NOT_STARTED',
//         type: 'PSS',
//       },
//       ],
//     },
//     {
//       deliusRecord: { offenderCrn: 'X12351',
//     },
//       nomisRecord: { prisonerNumber: 'AB1234L',
//       conditionalReleaseDate: tenDaysFromNow,
//     },
//       licences: [
//       { status: 'NOT_STARTED',
//         type: 'PSS',
//       },
//       ],
//     },
//     {
//       deliusRecord: {
//       otherIds: { nomsNumber: 'AB1234M',
//       crn: 'X12352',
//     },
//       offenderCrn: 'X12352',
//     },
//       nomisRecord: { prisonerNumber: 'AB1234M',
//       conditionalReleaseDate: tenDaysFromNow,
//       postRecallReleaseDate: nineDaysFromNow,
//       status: 'ACTIVE IN',
//       recall: true,
//     },
//       licences: [
//       { status: 'NOT_STARTED',
//         type: 'PSS',
//       },
//       ],
//     },
//     {
//       deliusRecord: { offenderCrn: 'X12353',
//     },
//       nomisRecord: { prisonerNumber: 'AB1234N',
//       conditionalReleaseDate: tenDaysFromNow,
//       postRecallReleaseDate: elevenDaysFromNow,
//       status: 'ACTIVE IN',
//       recall: true,
//     },
//       licences: [
//       { status: 'OOS_RECALL',
//         type: 'PSS',
//       },
//       ],
//     },
//     {
//       deliusRecord: { offenderCrn: 'X12354',
//     },
//       nomisRecord: { prisonerNumber: 'AB1234P',
//       conditionalReleaseDate: nineDaysFromNow,
//       postRecallReleaseDate: nineDaysFromNow,
//       status: 'ACTIVE IN',
//       recall: true,
//     },
//       licences: [
//       { status: 'NOT_STARTED',
//         type: 'PSS',
//       },
//       ],
//     },
//     {
//       deliusRecord: { offenderCrn: 'X12355',
//     },
//       nomisRecord: { prisonerNumber: 'AB1234Q',
//       conditionalReleaseDate: nineDaysFromNow,
//       status: 'ACTIVE IN',
//       recall: true,
//     },
//       licences: [
//       { status: 'NOT_STARTED',
//         type: 'PSS',
//       },
//       ],
//     },
//     {
//       deliusRecord: {
//       offenderCrn: 'X12356',
//       otherIds: { crn: 'X12356',
//       nomsNumber: 'AB1234R',
//     },
//     },
//       licences: [
//       { status: 'NOT_STARTED',
//         type: 'PSS',
//       },
//       ],
//       nomisRecord: { conditionalReleaseDate: nineDaysFromNow,
//       prisonerNumber: 'AB1234R',
//       status: 'INACTIVE TRN',
//     },
//     },
//   ]
// )
  }
}

private fun createManagedOffenderCrn(id: Long, crn: String, nomsNumber: String? = null) = OffenderDetail(
  offenderId = id,
  OtherIds(nomsNumber = nomsNumber, crn = crn),
  offenderManagers = emptyList(),
)
