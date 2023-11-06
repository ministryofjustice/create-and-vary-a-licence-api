package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder.setContext
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.IS91DeterminationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.LicenceActivationService.Companion.IS91_LICENCE_ACTIVATION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.LicenceActivationService.Companion.LICENCE_ACTIVATION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.LicenceActivationService.Companion.LICENCE_DEACTIVATION
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
  private val iS91DeterminationService = mock<IS91DeterminationService>()

  private val service = LicenceActivationService(
    licenceRepository,
    licenceService,
    prisonApiClient,
    prisonerSearchApiClient,
    iS91DeterminationService,
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("smills")
    whenever(securityContext.authentication).thenReturn(authentication)
    setContext(securityContext)

    reset(
      licenceRepository,
      licenceService,
      prisonApiClient,
      prisonerSearchApiClient,
      iS91DeterminationService,
    )
  }

  @Test
  fun `licence activation job should return if there are no APPROVED licences`() {
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf())

    service.licenceActivationJob()

    verify(licenceService, times(0)).activateLicences(emptyList(), "")
    verify(licenceService, times(0)).activateLicences(emptyList(), "")
    verify(licenceService, times(0)).inactivateLicences(emptyList(), "")
    verify(prisonApiClient, times(0)).getHdcStatuses(emptyList())
    verify(prisonerSearchApiClient, times(0)).searchPrisonersByBookingIds(emptyList())
  }

  @Test
  fun `licence activation job calls for non-HDC, non-IS91 licences to be activated on their ARD if the offender has been released`() {
    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(setOf(aLicenceEntity.bookingId!!)))
      .thenReturn(listOf(aPrisonerSearchPrisoner))
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(aLicenceEntity))
    whenever(iS91DeterminationService.getIS91AndExtraditionBookingIds(listOf(aPrisonerSearchPrisoner)))
      .thenReturn(emptyList())

    service.licenceActivationJob()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(listOf(aLicenceEntity), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
  }

  @Test
  fun `licence activation job calls for non-HDC, non-IS91 licences to be activated if their ARD is in the past and they have been released`() {
    val licence = aLicenceEntity.copy(actualReleaseDate = LocalDate.now().minusDays(10))

    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(licence))
    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(setOf(licence.bookingId!!)))
      .thenReturn(listOf(aPrisonerSearchPrisoner))
    whenever(iS91DeterminationService.getIS91AndExtraditionBookingIds(listOf(aPrisonerSearchPrisoner)))
      .thenReturn(emptyList())

    service.licenceActivationJob()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(listOf(licence), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
  }

  @Test
  fun `licence activation job calls for non-HDC, IS91 licences to be activated on CRD`() {
    val licence = aLicenceEntity.copy(conditionalReleaseDate = LocalDate.now())

    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(licence))
    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(setOf(licence.bookingId!!)))
      .thenReturn(listOf(aPrisonerSearchPrisoner))
    whenever(iS91DeterminationService.getIS91AndExtraditionBookingIds(listOf(aPrisonerSearchPrisoner)))
      .thenReturn(listOf(licence.bookingId!!))

    service.licenceActivationJob()

    verify(licenceService, times(1)).activateLicences(listOf(licence), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(emptyList(), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
  }

  @Test
  fun `licence activation job calls for non-HDC, IS91 licences to be activated on ARD if it is within 4 days before CRD`() {
    val licence = aLicenceEntity.copy(conditionalReleaseDate = LocalDate.now().plusDays(4))

    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(licence))
    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(setOf(licence.bookingId!!)))
      .thenReturn(listOf(aPrisonerSearchPrisoner))
    whenever(iS91DeterminationService.getIS91AndExtraditionBookingIds(listOf(aPrisonerSearchPrisoner)))
      .thenReturn(listOf(licence.bookingId!!))

    service.licenceActivationJob()

    verify(licenceService, times(1)).activateLicences(listOf(licence), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(emptyList(), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
  }

  @Test
  fun `licence activation job does not call for non-HDC, IS91 licences to be activated on ARD if it is more than 4 days before CRD`() {
    val licence = aLicenceEntity.copy(conditionalReleaseDate = LocalDate.now().plusDays(5))

    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(licence))
    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(setOf(licence.bookingId!!)))
      .thenReturn(listOf(aPrisonerSearchPrisoner))
    whenever(iS91DeterminationService.getIS91AndExtraditionBookingIds(listOf(aPrisonerSearchPrisoner)))
      .thenReturn(listOf(licence.bookingId!!))

    service.licenceActivationJob()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(emptyList(), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
  }

  @Test
  fun `licence activation job calls for HDC approved licences to be deactivated`() {
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(hdcLicence))
    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(setOf(hdcLicence.bookingId!!)))
      .thenReturn(listOf(hdcPrisoner))
    whenever(prisonApiClient.getHdcStatuses(listOf(hdcLicence.bookingId!!))).thenReturn(listOf(anHdcStatus))

    service.licenceActivationJob()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(emptyList(), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(listOf(hdcLicence), LICENCE_DEACTIVATION)
  }

  @Test
  fun `licence activation job call for HDC approved licences to be activated if no HDCED`() {
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(hdcLicence))
    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(setOf(hdcLicence.bookingId!!)))
      .thenReturn(listOf(hdcPrisoner.copy(homeDetentionCurfewEligibilityDate = null)))
    whenever(prisonApiClient.getHdcStatuses(listOf(hdcLicence.bookingId!!))).thenReturn(listOf(anHdcStatus))

    service.licenceActivationJob()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(listOf(hdcLicence), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
  }

  @Test
  fun `licence activation job calls for non-IS91 licences with HDC (not approved) to be activated on their ARD if the offender has been released`() {
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(hdcLicence))

    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(setOf(aLicenceEntity.bookingId!!)))
      .thenReturn(listOf(aPrisonerSearchPrisoner))

    whenever(iS91DeterminationService.getIS91AndExtraditionBookingIds(listOf(aPrisonerSearchPrisoner))).thenReturn(
      emptyList(),
    )
    whenever(prisonApiClient.getHdcStatuses(listOf(aLicenceEntity.bookingId!!))).thenReturn(
      listOf(
        anHdcStatus.copy(
          approvalStatus = "INACTIVE",
        ),
      ),
    )

    service.licenceActivationJob()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(listOf(aLicenceEntity), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
  }

  @Test
  fun `licence activation job does not call for non-IS91 licences to be activated if the offender has not been released`() {
    val unreleasedPrisoner = aPrisonerSearchPrisoner.copy(status = "ACTIVE IN")

    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(aLicenceEntity))
    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(setOf(aLicenceEntity.bookingId!!)))
      .thenReturn(listOf(aPrisonerSearchPrisoner.copy(status = "ACTIVE IN")))
    whenever(iS91DeterminationService.getIS91AndExtraditionBookingIds(listOf(unreleasedPrisoner))).thenReturn(emptyList())

    service.licenceActivationJob()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(emptyList(), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
  }

  @Test
  fun `licence activation job does not call for non-IS91 licences to be activated if on CRD and ARD is in the future`() {
    val licence = aLicenceEntity.copy(
      actualReleaseDate = LocalDate.now().plusDays(1),
      conditionalReleaseDate = LocalDate.now(),
    )

    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(licence))
    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(setOf(licence.bookingId!!)))
      .thenReturn(listOf(aPrisonerSearchPrisoner))
    whenever(iS91DeterminationService.getIS91AndExtraditionBookingIds(listOf(aPrisonerSearchPrisoner))).thenReturn(
      emptyList(),
    )

    service.licenceActivationJob()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(emptyList(), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(
      emptyList(),
      LICENCE_DEACTIVATION,
    )
  }

  @Test
  fun `licence activation job does not call for IS91 licences to be activated if the licence has no CRD`() {
    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(setOf(nonHdcLicence.bookingId!!))).thenReturn(
      listOf(
        nonHdcPrisoner,
      ),
    )
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(
      listOf(
        aLicenceEntity.copy(
          conditionalReleaseDate = null,
        ),
      ),
    )

    whenever(iS91DeterminationService.getIS91AndExtraditionBookingIds(listOf(nonHdcPrisoner))).thenReturn(listOf(54321))
    whenever(prisonApiClient.getHdcStatuses(listOf(nonHdcLicence.bookingId!!))).thenReturn(emptyList())

    service.licenceActivationJob()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(emptyList(), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
  }

  @Test
  fun `licence activation job calls for activation and deactivation of different licences simultaneously`() {
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate())
      .thenReturn(listOf(hdcLicence, nonHdcLicence))

    whenever(
      prisonerSearchApiClient.searchPrisonersByBookingIds(
        setOf(
          hdcLicence.bookingId!!,
          nonHdcLicence.bookingId!!,
        ),
      ),
    )
      .thenReturn(listOf(hdcPrisoner, nonHdcPrisoner))
    whenever(iS91DeterminationService.getIS91AndExtraditionBookingIds(listOf(nonHdcPrisoner)))
      .thenReturn(emptyList())
    whenever(prisonApiClient.getHdcStatuses(listOf(hdcLicence.bookingId!!))).thenReturn(listOf(anHdcStatus))

    service.licenceActivationJob()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(listOf(nonHdcLicence), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(listOf(hdcLicence), LICENCE_DEACTIVATION)
  }

  @Test
  fun `licence activation job logs when a non-HDC, non-IS91 offender isn't found in the prisoner offender search call and does not prevent activation of other licences`() {
    val licenceWithOffender = aLicenceEntity.copy(bookingId = 54322, nomsId = "A1234AB")

    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(
      listOf(
        aLicenceEntity,
        licenceWithOffender,
      ),
    )
    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(setOf(54321, 54322))).thenReturn(
      listOf(
        aPrisonerSearchPrisoner.copy(bookingId = "54322", prisonerNumber = "A1234AB"),
      ),
    )

    service.licenceActivationJob()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(listOf(licenceWithOffender), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
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
      lastName = "Y",
    ),
    createdBy = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "smills",
      email = "testemail@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    ),
  )

  private val anHdcStatus = PrisonerHdcStatus(
    approvalStatus = "APPROVED",
    bookingId = 54321,
    passed = true,
  )

  private val aPrisonerSearchPrisoner = PrisonerSearchPrisoner(
    "A1234AA",
    "54321",
    "ACTIVE IN",
    mostSeriousOffence = "Robbery",
    LocalDate.now().plusYears(1),
    LocalDate.now().plusYears(1),
    null,
    LocalDate.now().plusDays(1),
    LocalDate.now().plusDays(1),
    LocalDate.now().plusDays(1),
    null,
    null,
    null,
    "SENTENCED",
    false,
  )

  val hdcLicence = aLicenceEntity
  val nonHdcLicence =
    aLicenceEntity.copy(id = 2, bookingId = 54322, nomsId = "A1234AB", actualReleaseDate = LocalDate.now())

  val hdcPrisoner = aPrisonerSearchPrisoner.copy(
    prisonerNumber = hdcLicence.nomsId!!,
    bookingId = hdcLicence.bookingId.toString(),
    homeDetentionCurfewEligibilityDate = LocalDate.now(),
  )

  val nonHdcPrisoner = aPrisonerSearchPrisoner.copy(
    prisonerNumber = nonHdcLicence.nomsId!!,
    bookingId = nonHdcLicence.bookingId.toString(),
  )
}
