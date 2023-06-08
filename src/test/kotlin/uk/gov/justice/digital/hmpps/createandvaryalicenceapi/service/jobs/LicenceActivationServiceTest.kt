import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.LicenceActivationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerHdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

class LicenceActivationServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val licenceService = mock<LicenceService>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()

  private val service = LicenceActivationService(
    licenceRepository,
    licenceService,
    prisonApiClient,
    prisonerSearchApiClient
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("smills")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    org.mockito.kotlin.reset(
      licenceRepository,
      licenceService,
      prisonApiClient,
      prisonerSearchApiClient
    )
  }

  @Test
  fun `licence activation job calls for non-HDC, non-IS91 licences to be activated on their ARD if the offender has been released`() {
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(aLicenceEntity))
    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(listOf(54321))).thenReturn(
      listOf(prisonerSearchPrisoner)
    )
    whenever(prisonApiClient.getIS91AndExtraditionBookingIds(listOf(54321))).thenReturn(emptyList())
    whenever(prisonApiClient.hdcStatuses(listOf(54321))).thenReturn(emptyList())

    service.licenceActivationJob()

    verify(licenceService, times(1)).activateLicences(listOf(aLicenceEntity))
    verify(licenceService, times(0)).inactivateLicences(anyList())
  }

  @Test
  fun `licence activation job calls for non-HDC, non-IS91 licences to be activated if their ARD is in the past and they have been released`() {
    val licence = aLicenceEntity.copy(actualReleaseDate = LocalDate.now().minusDays(10))
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(licence))
    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(listOf(54321))).thenReturn(
      listOf(
        prisonerSearchPrisoner
      )
    )
    whenever(prisonApiClient.getIS91AndExtraditionBookingIds(listOf(54321))).thenReturn(emptyList())
    whenever(prisonApiClient.hdcStatuses(listOf(54321))).thenReturn(emptyList())

    service.licenceActivationJob()

    verify(licenceService, times(1)).activateLicences(listOf(licence))
    verify(licenceService, times(0)).inactivateLicences(anyList())
  }

  @Test
  fun `licence activation job calls for non-HDC, IS91 licences to be activated on CRD`() {
    val licence = aLicenceEntity.copy(conditionalReleaseDate = LocalDate.now())
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(licence))
    // Lack of PrisonerSearchPrisoner object shows activation is happening via IS91 path
    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(listOf(54321))).thenReturn(emptyList())
    whenever(prisonApiClient.getIS91AndExtraditionBookingIds(listOf(54321))).thenReturn(listOf(54321))
    whenever(prisonApiClient.hdcStatuses(listOf(54321))).thenReturn(emptyList())

    service.licenceActivationJob()

    verify(licenceService, times(1)).activateLicences(listOf(licence))
    verify(licenceService, times(0)).inactivateLicences(anyList())
  }

  @Test
  fun `licence activation job calls for non-HDC, IS91 licences to be activated on ARD if it is within 4 days before CRD`() {
    val licence = aLicenceEntity.copy(conditionalReleaseDate = LocalDate.now().plusDays(4))
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(licence))
    // Lack of PrisonerSearchPrisoner object shows activation is happening via IS91 path
    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(listOf(54321))).thenReturn(emptyList())
    whenever(prisonApiClient.getIS91AndExtraditionBookingIds(listOf(54321))).thenReturn(listOf(54321))
    whenever(prisonApiClient.hdcStatuses(listOf(54321))).thenReturn(emptyList())

    service.licenceActivationJob()

    verify(licenceService, times(1)).activateLicences(listOf(licence))
    verify(licenceService, times(0)).inactivateLicences(anyList())
  }

  @Test
  fun `licence activation job does not call for non-HDC, IS91 licences to be activated on ARD if it is more than 4 days before CRD`() {
    val licence = aLicenceEntity.copy(conditionalReleaseDate = LocalDate.now().plusDays(5))
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(licence))
    // Lack of PrisonerSearchPrisoner object shows activation is happening via IS91 path
    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(listOf(54321))).thenReturn(emptyList())
    whenever(prisonApiClient.getIS91AndExtraditionBookingIds(listOf(54321))).thenReturn(listOf(54321))
    whenever(prisonApiClient.hdcStatuses(listOf(54321))).thenReturn(emptyList())

    service.licenceActivationJob()

    verify(licenceService, times(0)).activateLicences(anyList())
    verify(licenceService, times(0)).inactivateLicences(anyList())
  }

  @Test
  fun `licence activation job calls for HDC approved licences to be deactivated`() {
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(aLicenceEntity))
    whenever(prisonApiClient.hdcStatuses(listOf(54321))).thenReturn(listOf(anHdcStatus))

    service.licenceActivationJob()

    verify(licenceService, times(0)).activateLicences(anyList())
    verify(licenceService, times(1)).inactivateLicences(listOf(aLicenceEntity))
  }

  @Test
  fun `licence activation job calls for non-IS91 licences with HDC (not approved) to be activated on their ARD if the offender has been released`() {
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(aLicenceEntity))
    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(listOf(54321))).thenReturn(
      listOf(prisonerSearchPrisoner)
    )
    whenever(prisonApiClient.getIS91AndExtraditionBookingIds(listOf(54321))).thenReturn(emptyList())
    whenever(prisonApiClient.hdcStatuses(listOf(54321))).thenReturn(listOf(anHdcStatus.copy(approvalStatus = "INACTIVE")))

    service.licenceActivationJob()

    verify(licenceService, times(1)).activateLicences(listOf(aLicenceEntity))
    verify(licenceService, times(0)).inactivateLicences(anyList())
  }

  @Test
  fun `licence activation job does not call for non-IS91 licences to be activated if the offender has not been released`() {
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(aLicenceEntity))
    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(listOf(54321))).thenReturn(
      listOf(
        prisonerSearchPrisoner.copy(status = "ACTIVE IN")
      )
    )
    whenever(prisonApiClient.getIS91AndExtraditionBookingIds(listOf(54321))).thenReturn(emptyList())
    whenever(prisonApiClient.hdcStatuses(listOf(54321))).thenReturn(emptyList())

    service.licenceActivationJob()

    verify(licenceService, times(0)).activateLicences(anyList())
    verify(licenceService, times(0)).inactivateLicences(anyList())
  }

  @Test
  fun `licence activation job does not call for non-IS91 licences to be activated if on CRD and ARD is in the future`() {
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(
      listOf(
        aLicenceEntity.copy(
          actualReleaseDate = LocalDate.now().plusDays(1),
          conditionalReleaseDate = LocalDate.now()
        )
      )
    )
    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(listOf(54321))).thenReturn(
      listOf(
        prisonerSearchPrisoner
      )
    )
    whenever(prisonApiClient.getIS91AndExtraditionBookingIds(listOf(54321))).thenReturn(emptyList())
    whenever(prisonApiClient.hdcStatuses(listOf(54321))).thenReturn(emptyList())

    service.licenceActivationJob()

    verify(licenceService, times(0)).activateLicences(anyList())
    verify(licenceService, times(0)).inactivateLicences(anyList())
  }

  @Test
  fun `licence activation job does not call for IS91 licences to be activated if the licence has no CRD`() {
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(
      listOf(
        aLicenceEntity.copy(
          conditionalReleaseDate = null
        )
      )
    )
    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(listOf(54321))).thenReturn(emptyList())
    whenever(prisonApiClient.getIS91AndExtraditionBookingIds(listOf(54321))).thenReturn(listOf(54321))
    whenever(prisonApiClient.hdcStatuses(listOf(54321))).thenReturn(emptyList())

    service.licenceActivationJob()

    verify(licenceService, times(0)).activateLicences(anyList())
    verify(licenceService, times(0)).inactivateLicences(anyList())
  }

  @Test
  fun `licence activation job calls for activation and deactivation of different licences simultaneously`() {
    val nonHdcLicence =
      aLicenceEntity.copy(id = 2, bookingId = 54322, nomsId = "A1234AB", actualReleaseDate = LocalDate.now())
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(
      listOf(
        aLicenceEntity,
        nonHdcLicence
      )
    )
    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(listOf(54322))).thenReturn(
      listOf(
        prisonerSearchPrisoner.copy(prisonerNumber = "A1234AB", bookingId = "54322")
      )
    )
    whenever(prisonApiClient.getIS91AndExtraditionBookingIds(listOf(54321, 54322))).thenReturn(emptyList())
    whenever(prisonApiClient.hdcStatuses(listOf(54321, 54322))).thenReturn(listOf(anHdcStatus))

    service.licenceActivationJob()

    verify(licenceService, times(1)).activateLicences(listOf(nonHdcLicence))
    verify(licenceService, times(1)).inactivateLicences(listOf(aLicenceEntity))
  }

  private val aLicenceEntity = Licence(
    id = 1,
    typeCode = LicenceType.AP,
    version = "1.1",
    statusCode = LicenceStatus.APPROVED,
    nomsId = "A1234AA",
    bookingNo = "123456",
    bookingId = 54321,
    crn = "X12345",
    pnc = "2019/123445",
    cro = "12345",
    prisonCode = "MDI",
    prisonDescription = "Moorland (HMP)",
    forename = "Bob",
    surname = "Mortimer",
    dateOfBirth = LocalDate.of(1985, 12, 28),
    conditionalReleaseDate = LocalDate.of(2021, 10, 22),
    actualReleaseDate = LocalDate.now(),
    sentenceStartDate = LocalDate.of(2018, 10, 22),
    sentenceEndDate = LocalDate.of(2021, 10, 22),
    licenceStartDate = LocalDate.of(2021, 10, 22),
    licenceExpiryDate = LocalDate.of(2021, 10, 22),
    topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
    topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
    probationAreaCode = "N01",
    probationAreaDescription = "Wales",
    probationPduCode = "N01A",
    probationPduDescription = "Cardiff",
    probationLauCode = "N01A2",
    probationLauDescription = "Cardiff South",
    probationTeamCode = "NA01A2-A",
    probationTeamDescription = "Cardiff South Team A",
    dateCreated = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
    standardConditions = emptyList(),
    responsibleCom = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "smills",
      email = "testemail@probation.gov.uk",
      firstName = "X",
      lastName = "Y"
    ),
    createdBy = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "smills",
      email = "testemail@probation.gov.uk",
      firstName = "X",
      lastName = "Y"
    ),
  )

  private val anHdcStatus = PrisonerHdcStatus(
    approvalStatus = "APPROVED",
    bookingId = 54321,
    passed = true
  )

  private val prisonerSearchPrisoner = PrisonerSearchPrisoner(
    prisonerNumber = "A1234AA",
    bookingId = "54321",
    status = "INACTIVE OUT"
  )
}
