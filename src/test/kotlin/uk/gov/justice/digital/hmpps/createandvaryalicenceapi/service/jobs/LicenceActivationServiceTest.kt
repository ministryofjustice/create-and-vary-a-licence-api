package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder.setContext
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.AuditService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.IS91DeterminationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TelemetryService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.hdcPrisonerStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcStatuses
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.LicenceActivationService.Companion.IS91_LICENCE_ACTIVATION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.LicenceActivationService.Companion.LICENCE_ACTIVATION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.LicenceActivationService.Companion.LICENCE_DEACTIVATION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.LicenceActivationService.Companion.REMAND_LICENCE_ACTIVATION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.CourtEventOutcome
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

class LicenceActivationServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val licenceService = mock<LicenceService>()
  private val hdcService = mock<HdcService>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val iS91DeterminationService = mock<IS91DeterminationService>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val auditService = mock<AuditService>()
  private val telemetryService = mock<TelemetryService>()

  private var service = LicenceActivationService(
    licenceRepository,
    licenceService,
    hdcService,
    prisonerSearchApiClient,
    prisonApiClient,
    auditService,
    telemetryService,
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("tcom")
    whenever(securityContext.authentication).thenReturn(authentication)
    setContext(securityContext)

    reset(
      licenceRepository,
      licenceService,
      hdcService,
      prisonerSearchApiClient,
      iS91DeterminationService,
      telemetryService,
    )
  }

  @Test
  fun `licence activation job should return if there are no APPROVED licences`() {
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(emptyList())

    service.licenceActivation()

    verifyNoInteractions(licenceService)
    verifyNoInteractions(hdcService)
    verifyNoInteractions(prisonerSearchApiClient)
    verifyNoInteractions(telemetryService)
  }

  @Test
  fun `licence activation job calls for non-HDC, non-IS91 licences to be activated on their release date if the offender has been released`() {
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aLicenceEntity.nomsId!!)))
      .thenReturn(listOf(aPrisonerSearchPrisoner))
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(aLicenceEntity))
    whenever(prisonApiClient.getCourtEventOutcomes(any(), any(), any()))
      .thenReturn(emptyList())
    whenever(
      hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any()),
    ).thenReturn(HdcStatuses(emptyList()))

    service.licenceActivation()

    verify(licenceService).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService).activateLicences(listOf(aLicenceEntity), LICENCE_ACTIVATION)
    verify(licenceService).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
    verify(telemetryService).recordActivateLicencesJobEvent(0, 0, 1, 0)
  }

  @Test
  fun `licence activation job calls for non-HDC, non-IS91 cases to be activated if their LSD is in the past and they have been released`() {
    val licence = aLicenceEntity.copy(licenceStartDate = LocalDate.now().minusDays(10))

    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(licence))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(licence.nomsId!!)))
      .thenReturn(listOf(aPrisonerSearchPrisoner))
    whenever(prisonApiClient.getCourtEventOutcomes(any(), any(), any()))
      .thenReturn(emptyList())
    whenever(hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any())).thenReturn(
      HdcStatuses(emptyList()),
    )

    service.licenceActivation()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(listOf(licence), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
    verify(telemetryService).recordActivateLicencesJobEvent(0, 0, 1, 0)
  }

  @Test
  fun `licence activation job calls for non-HDC, IS91 licences to be activated on Licence Start Date`() {
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(
      listOf(
        aLicenceEntity.copy(
          licenceStartDate = LocalDate.now(),
        ),
      ),
    )
    val prisoners = listOf(aPrisonerSearchPrisoner)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aLicenceEntity.nomsId!!)))
      .thenReturn(prisoners)
    whenever(prisonApiClient.getCourtEventOutcomes(any(), any(), any()))
      .thenReturn(listOf(aIS91CourtEventOutcome))
    whenever(
      hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any()),
    ).thenReturn(HdcStatuses(emptyList()))

    service.licenceActivation()

    verify(licenceService, times(1)).activateLicences(listOf(aLicenceEntity), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(emptyList(), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
    verify(telemetryService).recordActivateLicencesJobEvent(1, 0, 0, 0)
  }

  @Test
  fun `licence activation job calls for non-HDC, IS91 licences to be activated if Licence Start Date is in the past`() {
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(
      listOf(
        aLicenceEntity.copy(
          licenceStartDate = LocalDate.now().minusDays(10),
        ),
      ),
    )
    val prisoners = listOf(aPrisonerSearchPrisoner)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aLicenceEntity.nomsId!!)))
      .thenReturn(prisoners)
    whenever(prisonApiClient.getCourtEventOutcomes(any(), any(), any()))
      .thenReturn(listOf(aIS91CourtEventOutcome))
    whenever(
      hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any()),
    ).thenReturn(HdcStatuses(emptyList()))

    service.licenceActivation()

    verify(licenceService, times(1)).activateLicences(listOf(aLicenceEntity), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(emptyList(), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
    verify(telemetryService).recordActivateLicencesJobEvent(1, 0, 0, 0)
  }

  @Test
  fun `licence activation job calls for non-HDC, IS91 licences to be activated if Licence Start Date is in the past where their most serious offence is ILLEGAL IMMIGRANT DETAINEE`() {
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(
      listOf(
        aLicenceEntity.copy(
          licenceStartDate = LocalDate.now().minusDays(10),
        ),
      ),
    )
    val prisoners = listOf(
      aPrisonerSearchPrisoner.copy(
        mostSeriousOffence = "ILLEGAL IMMIGRANT/DETAINEE",
      ),
    )
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aLicenceEntity.nomsId!!)))
      .thenReturn(prisoners)
    whenever(prisonApiClient.getCourtEventOutcomes(any(), any(), any()))
      .thenReturn(emptyList())
    whenever(
      hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any()),
    ).thenReturn(HdcStatuses(emptyList()))

    service.licenceActivation()

    verify(licenceService, times(1)).activateLicences(listOf(aLicenceEntity), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(emptyList(), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
    verify(telemetryService).recordActivateLicencesJobEvent(1, 0, 0, 0)
  }

  @Test
  fun `licence activation job calls for HDC approved case to be deactivated when they are not HDC licences`() {
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(aLicenceEntity))
    val prisoners = listOf(
      aPrisonerSearchPrisoner.copy(homeDetentionCurfewEligibilityDate = LocalDate.now()),
    )
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aLicenceEntity.nomsId!!)))
      .thenReturn(prisoners)
    whenever(hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any())).thenReturn(
      HdcStatuses(
        listOf(
          hdcPrisonerStatus().copy(bookingId = aLicenceEntity.bookingId!!, approvalStatus = "APPROVED"),
        ),
      ),
    )
    whenever { prisonApiClient.getCourtEventOutcomes(any(), any(), any()) }.thenReturn(emptyList())

    service.licenceActivation()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(emptyList(), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(listOf(aLicenceEntity), LICENCE_DEACTIVATION)
    verify(telemetryService).recordActivateLicencesJobEvent(0, 0, 0, 1)
  }

  @Test
  fun `licence activation job call for HDC approved CRD licence to be activated if no HDCED`() {
    val prisoners = listOf(aPrisonerSearchPrisoner.copy(homeDetentionCurfewEligibilityDate = null))
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(aLicenceEntity))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aLicenceEntity.nomsId!!)))
      .thenReturn(prisoners)
    whenever(hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any())).thenReturn(
      HdcStatuses(emptyList()),
    )
    whenever { prisonApiClient.getCourtEventOutcomes(any(), any(), any()) }.thenReturn(emptyList())

    service.licenceActivation()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(listOf(aLicenceEntity), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
    verify(telemetryService).recordActivateLicencesJobEvent(0, 0, 1, 0)
  }

  @Test
  fun `licence activation job calls for non-IS91 cases with HDC (not approved) to be activated on their LSD if the offender has been released`() {
    val prisoners = listOf(aPrisonerSearchPrisoner)

    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(aLicenceEntity))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aLicenceEntity.nomsId!!)))
      .thenReturn(prisoners)
    whenever(prisonApiClient.getCourtEventOutcomes(any(), any(), any())).thenReturn(
      emptyList(),
    )
    whenever { prisonApiClient.getCourtEventOutcomes(any(), any(), any()) }.thenReturn(emptyList())
    whenever(hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any())).thenReturn(
      HdcStatuses(emptyList()),
    )

    service.licenceActivation()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(listOf(aLicenceEntity), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
    verify(telemetryService).recordActivateLicencesJobEvent(0, 0, 1, 0)
  }

  @Test
  fun `licence activation job does not call for non-IS91 licences to be activated if the offender has not been released`() {
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(aLicenceEntity))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aLicenceEntity.nomsId!!)))
      .thenReturn(listOf(aPrisonerSearchPrisoner.copy(status = "ACTIVE IN")))
    whenever(prisonApiClient.getCourtEventOutcomes(any(), any(), any()))
      .thenReturn(emptyList())
    whenever { prisonApiClient.getCourtEventOutcomes(any(), any(), any()) }.thenReturn(emptyList())
    whenever(hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any())).thenReturn(
      HdcStatuses(emptyList()),
    )

    service.licenceActivation()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(emptyList(), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
    verify(telemetryService).recordActivateLicencesJobEvent(0, 0, 0, 0)
  }

  @Test
  fun `licence activation job does not call for non-IS91 licences to be activated if the licence has no LSD`() {
    val prisoners = listOf(nonHdcPrisoner)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aLicenceEntity.nomsId!!))).thenReturn(
      prisoners,
    )
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(
      listOf(
        nonHdcLicence.copy(
          licenceStartDate = null,
        ),
      ),
    )

    whenever(prisonApiClient.getCourtEventOutcomes(any(), any(), any()))
      .thenReturn(emptyList())
    whenever { prisonApiClient.getCourtEventOutcomes(any(), any(), any()) }.thenReturn(emptyList())
    whenever(hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any())).thenReturn(
      HdcStatuses(emptyList()),
    )

    service.licenceActivation()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(emptyList(), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
    verify(telemetryService).recordActivateLicencesJobEvent(0, 0, 0, 0)
  }

  @Test
  fun `licence activation job does not call for IS91 licences to be activated if the licence has no LSD`() {
    val prisoners = listOf(
      nonHdcPrisoner,
    )
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(nonHdcLicence.nomsId!!))).thenReturn(
      prisoners,
    )
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(
      listOf(
        nonHdcLicence.copy(
          licenceStartDate = null,
        ),
      ),
    )

    whenever(prisonApiClient.getCourtEventOutcomes(any(), any(), any()))
      .thenReturn(listOf(aIS91CourtEventOutcome))
    whenever { prisonApiClient.getCourtEventOutcomes(any(), any(), any()) }.thenReturn(emptyList())
    whenever(hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any())).thenReturn(
      HdcStatuses(emptyList()),
    )

    service.licenceActivation()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(emptyList(), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
    verify(telemetryService).recordActivateLicencesJobEvent(0, 0, 0, 0)
  }

  @Test
  fun `licence activation job does not call for IS91 licences to be activated if the Licence Start Date is in the future`() {
    val prisoners = listOf(
      nonHdcPrisoner,
    )
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(nonHdcLicence.nomsId!!))).thenReturn(
      prisoners,
    )
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(
      listOf(
        nonHdcLicence.copy(
          licenceStartDate = LocalDate.now().plusDays(10),
        ),
      ),
    )

    whenever(prisonApiClient.getCourtEventOutcomes(any(), any(), any()))
      .thenReturn(listOf(aIS91CourtEventOutcome))
    whenever(hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any())).thenReturn(
      HdcStatuses(emptyList()),
    )

    service.licenceActivation()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(emptyList(), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
    verify(telemetryService).recordActivateLicencesJobEvent(0, 0, 0, 0)
  }

  @Test
  fun `licence activation job calls for activation and deactivation of different licences simultaneously`() {
    val crdPrisoner = aPrisonerSearchPrisoner.copy(
      prisonerNumber = "A1234BC",
      bookingId = "12345",
      homeDetentionCurfewEligibilityDate = LocalDate.now(),
    )
    val crdLicence =
      aLicenceEntity.copy(nomsId = crdPrisoner.prisonerNumber, bookingId = crdPrisoner.bookingId!!.toLong())

    val hdcPrisoner = aPrisonerSearchPrisoner.copy(
      prisonerNumber = "B1234CD",
      bookingId = "54321",
      homeDetentionCurfewEligibilityDate = LocalDate.now(),
    )
    val hdcLicence = hdcLicence.copy(nomsId = hdcPrisoner.prisonerNumber, bookingId = hdcPrisoner.bookingId!!.toLong())

    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate())
      .thenReturn(listOf(crdLicence, hdcLicence))

    whenever(
      prisonerSearchApiClient.searchPrisonersByNomisIds(
        listOf(crdLicence.nomsId!!, hdcLicence.nomsId!!),
      ),
    ).thenReturn(listOf(crdPrisoner, hdcPrisoner))
    whenever(prisonApiClient.getCourtEventOutcomes(any(), any(), any()))
      .thenReturn(emptyList())
    whenever(hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any())).thenReturn(
      HdcStatuses(
        listOf(
          hdcPrisonerStatus().copy(bookingId = hdcLicence.bookingId!!, approvalStatus = "APPROVED"),
          hdcPrisonerStatus().copy(bookingId = crdLicence.bookingId!!, approvalStatus = "APPROVED"),
        ),
      ),
    )

    service.licenceActivation()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(listOf(hdcLicence), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(listOf(crdLicence), LICENCE_DEACTIVATION)
    verify(telemetryService).recordActivateLicencesJobEvent(0, 0, 1, 1)
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

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf("A1234AA", "A1234AB"))).thenReturn(
      listOf(
        aPrisonerSearchPrisoner.copy(bookingId = "54322", prisonerNumber = "A1234AB"),
      ),
    )

    whenever(hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any())).thenReturn(
      HdcStatuses(emptyList()),
    )
    whenever { prisonApiClient.getCourtEventOutcomes(any(), any(), any()) }.thenReturn(emptyList())

    service.licenceActivation()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(listOf(licenceWithOffender), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
    verify(telemetryService).recordActivateLicencesJobEvent(0, 0, 1, 0)
  }

  @Test
  fun `licence activation job calls for HDC licences to activate`() {
    val prisoners = listOf(hdcPrisoner)

    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(hdcLicence))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(hdcLicence.nomsId!!)))
      .thenReturn(prisoners)
    whenever(hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any())).thenReturn(
      HdcStatuses(
        listOf(
          hdcPrisonerStatus().copy(bookingId = hdcLicence.bookingId!!, approvalStatus = "APPROVED"),
        ),
      ),
    )
    whenever { prisonApiClient.getCourtEventOutcomes(any(), any(), any()) }.thenReturn(emptyList())

    service.licenceActivation()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(listOf(hdcLicence), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
    verify(telemetryService).recordActivateLicencesJobEvent(0, 0, 1, 0)
  }

  @Test
  fun `licence activation job ignores HDC licences not approved for HDC `() {
    val anotherHdcLicence = hdcLicence.copy(nomsId = "C1234DE", bookingId = 22222)

    val anotherHdcPrisoner =
      hdcPrisoner.copy(prisonerNumber = "C1234DE", bookingId = anotherHdcLicence.bookingId.toString())

    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(
      listOf(
        hdcLicence,
        anotherHdcLicence,
      ),
    )
    val prisoners = listOf(hdcPrisoner, anotherHdcPrisoner)
    whenever(
      prisonerSearchApiClient.searchPrisonersByNomisIds(
        listOf(
          hdcLicence.nomsId!!,
          anotherHdcLicence.nomsId!!,
        ),
      ),
    )
      .thenReturn(prisoners)

    whenever(hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any())).thenReturn(
      HdcStatuses(
        listOf(
          hdcPrisonerStatus().copy(bookingId = hdcLicence.bookingId!!, approvalStatus = "APPROVED"),
        ),
      ),
    )
    whenever { prisonApiClient.getCourtEventOutcomes(any(), any(), any()) }.thenReturn(emptyList())

    service.licenceActivation()

    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(listOf(hdcLicence), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
    verify(telemetryService).recordActivateLicencesJobEvent(0, 0, 1, 0)
  }

  @Test
  fun `licence activation job activates licences where prisoner is on remand on licence start date`() {
    service = LicenceActivationService(
      licenceRepository,
      licenceService,
      hdcService,
      prisonerSearchApiClient,
      prisonApiClient,
      auditService,
      telemetryService,
      remandEnabled = true,
    )
    val remandLicence = nonHdcLicence.copy(licenceStartDate = LocalDate.now().minusDays(1))
    val remandPrisoner = nonHdcPrisoner

    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(remandLicence))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(remandLicence.nomsId!!)))
      .thenReturn(listOf(remandPrisoner))
    whenever(prisonApiClient.getCourtEventOutcomes(any(), any(), any()))
      .thenReturn(listOf(aRemandCourtEventOutcome.copy(bookingId = remandLicence.bookingId!!)))
    whenever(hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any()))
      .thenReturn(HdcStatuses(emptyList()))

    service.licenceActivation()

    verify(licenceService, times(1)).activateLicences(listOf(remandLicence), REMAND_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(emptyList(), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
    verify(telemetryService).recordActivateLicencesJobEvent(0, 1, 0, 0)
  }

  @Test
  fun `licence activation job activates both IS91 and remand licences where there are court outcomes for these licences`() {
    service = LicenceActivationService(
      licenceRepository,
      licenceService,
      hdcService,
      prisonerSearchApiClient,
      prisonApiClient,
      auditService,
      telemetryService,
      remandEnabled = true,
    )

    val is91Licence =
      nonHdcLicence.copy(licenceStartDate = LocalDate.now().minusDays(1), bookingId = 123456, nomsId = "A1234BC")
    val is91Prisoner =
      nonHdcPrisoner.copy(bookingId = is91Licence.bookingId.toString(), prisonerNumber = is91Licence.nomsId!!)

    val remandLicence =
      nonHdcLicence.copy(licenceStartDate = LocalDate.now().minusDays(1), bookingId = 789012, nomsId = "B1234CD")
    val remandPrisoner =
      nonHdcPrisoner.copy(bookingId = remandLicence.bookingId.toString(), prisonerNumber = remandLicence.nomsId!!)

    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(
      listOf(
        is91Licence,
        remandLicence,
      ),
    )
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(is91Licence.nomsId!!, remandLicence.nomsId!!)))
      .thenReturn(listOf(is91Prisoner, remandPrisoner))
    whenever(prisonApiClient.getCourtEventOutcomes(any(), any(), any()))
      .thenReturn(
        listOf(
          aIS91CourtEventOutcome.copy(bookingId = is91Licence.bookingId!!),
          aRemandCourtEventOutcome.copy(bookingId = remandLicence.bookingId!!),
        ),
      )
    whenever(hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any()))
      .thenReturn(HdcStatuses(emptyList()))

    service.licenceActivation()

    verify(licenceService, times(1)).activateLicences(listOf(remandLicence), REMAND_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(listOf(is91Licence), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(emptyList(), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
    verify(telemetryService).recordActivateLicencesJobEvent(1, 1, 0, 0)
  }

  @Test
  fun `licence activation job does not activate licences where prisoner is on remand with a release date in the future`() {
    service = LicenceActivationService(
      licenceRepository,
      licenceService,
      hdcService,
      prisonerSearchApiClient,
      prisonApiClient,
      auditService,
      telemetryService,
      remandEnabled = true,
    )
    val remandLicence = nonHdcLicence.copy(licenceStartDate = LocalDate.now().plusDays(1))

    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(remandLicence))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(remandLicence.nomsId!!)))
      .thenReturn(listOf(nonHdcPrisoner))
    whenever(prisonApiClient.getCourtEventOutcomes(any(), any(), any()))
      .thenReturn(listOf(aRemandCourtEventOutcome))
    whenever(hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any()))
      .thenReturn(HdcStatuses(emptyList()))

    service.licenceActivation()

    verify(licenceService, times(1)).activateLicences(emptyList(), REMAND_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService, times(1)).activateLicences(emptyList(), LICENCE_ACTIVATION)
    verify(licenceService, times(1)).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
    verify(telemetryService).recordActivateLicencesJobEvent(0, 0, 0, 0)
  }

  @Test
  fun `licence activation job does not activate licences where prisoner is on remand with no licence start date`() {
    service = LicenceActivationService(
      licenceRepository,
      licenceService,
      hdcService,
      prisonerSearchApiClient,
      prisonApiClient,
      auditService,
      telemetryService,
      remandEnabled = true,
    )
    val remandLicence = nonHdcLicence.copy(licenceStartDate = null)

    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(remandLicence))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(remandLicence.nomsId!!)))
      .thenReturn(listOf(nonHdcPrisoner))
    whenever(prisonApiClient.getCourtEventOutcomes(any(), any(), any()))
      .thenReturn(listOf(aRemandCourtEventOutcome))
    whenever(hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any()))
      .thenReturn(HdcStatuses(emptyList()))

    service.licenceActivation()

    verify(licenceService).activateLicences(emptyList(), REMAND_LICENCE_ACTIVATION)
    verify(licenceService).activateLicences(emptyList(), IS91_LICENCE_ACTIVATION)
    verify(licenceService).activateLicences(emptyList(), LICENCE_ACTIVATION)
    verify(licenceService).inactivateLicences(emptyList(), LICENCE_DEACTIVATION)
    verify(telemetryService).recordActivateLicencesJobEvent(0, 0, 0, 0)
  }

  @Test
  fun `licence activation job does not update booking or record an audit event when booking id has not changed`() {
    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(aLicenceEntity))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(aLicenceEntity.nomsId!!)))
      .thenReturn(listOf(aPrisonerSearchPrisoner))
    whenever(prisonApiClient.getCourtEventOutcomes(any(), any(), any()))
      .thenReturn(emptyList())
    whenever(hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any()))
      .thenReturn(HdcStatuses(emptyList()))

    service.licenceActivation()

    verify(licenceRepository, times(0)).save(any())
    verifyNoInteractions(auditService)
    verify(telemetryService).recordActivateLicencesJobEvent(0, 0, 1, 0)
  }

  @Test
  fun `licence activation job updates booking id and booking number and records an audit event when the booking id has changed`() {
    val licence = aLicenceEntity.copy(bookingId = 54321, bookingNo = "12345A")
    val prisoner = aPrisonerSearchPrisoner.copy(bookingId = "78901", bookNumber = "67890B")

    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(licence))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(licence.nomsId!!)))
      .thenReturn(listOf(prisoner))
    whenever(prisonApiClient.getCourtEventOutcomes(any(), any(), any()))
      .thenReturn(emptyList())
    whenever(hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any()))
      .thenReturn(HdcStatuses(emptyList()))

    service.licenceActivation()

    val licenceCaptor = argumentCaptor<Licence>()
    verify(licenceRepository, times(1)).save(licenceCaptor.capture())
    assertThat(licenceCaptor.firstValue.bookingId).isEqualTo(78901L)
    assertThat(licenceCaptor.firstValue.bookingNo).isEqualTo("67890B")

    verify(auditService).recordAuditEventBookingChanged(
      licence = licence,
      oldBookingId = 54321L,
      newBookingId = 78901L,
      oldBookingNo = "12345A",
      newBookingNo = "67890B",
    )
    verify(telemetryService).recordActivateLicencesJobEvent(0, 0, 1, 0)
  }

  @Test
  fun `licence activation job only updates the booking for licences whose booking id has changed when multiple licences are present`() {
    val unchangedLicence = aLicenceEntity
    val changedLicence = nonHdcLicence.copy(bookingId = 54321, bookingNo = "12345A")
    val changedPrisoner = nonHdcPrisoner.copy(bookingId = "78901", bookNumber = "67890B")

    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate())
      .thenReturn(listOf(unchangedLicence, changedLicence))
    whenever(
      prisonerSearchApiClient.searchPrisonersByNomisIds(
        listOf(unchangedLicence.nomsId!!, changedLicence.nomsId!!),
      ),
    ).thenReturn(listOf(aPrisonerSearchPrisoner, changedPrisoner))
    whenever(prisonApiClient.getCourtEventOutcomes(any(), any(), any()))
      .thenReturn(emptyList())
    whenever(hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any()))
      .thenReturn(HdcStatuses(emptyList()))

    service.licenceActivation()

    val licenceCaptor = argumentCaptor<Licence>()
    verify(licenceRepository, times(1)).save(licenceCaptor.capture())
    assertThat(licenceCaptor.firstValue.id).isEqualTo(changedLicence.id)
    assertThat(licenceCaptor.firstValue.bookingId).isEqualTo(78901L)
    assertThat(licenceCaptor.firstValue.bookingNo).isEqualTo("67890B")

    verify(auditService).recordAuditEventBookingChanged(
      licence = changedLicence,
      oldBookingId = 54321L,
      newBookingId = 78901L,
      oldBookingNo = "12345A",
      newBookingNo = "67890B",
    )
    verify(telemetryService).recordActivateLicencesJobEvent(0, 0, 2, 0)
  }

  @Test
  fun `licence activation job does not update the booking when the licence is not yet due for activation even though its booking id has changed`() {
    val licence = aLicenceEntity.copy(
      licenceStartDate = LocalDate.now().plusDays(5),
      bookingId = 54321,
      bookingNo = "12345A",
    )
    val prisoner = aPrisonerSearchPrisoner.copy(bookingId = "78901", bookNumber = "67890B")

    whenever(licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()).thenReturn(listOf(licence))
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(licence.nomsId!!)))
      .thenReturn(listOf(prisoner))
    whenever(prisonApiClient.getCourtEventOutcomes(any(), any(), any()))
      .thenReturn(emptyList())
    whenever(hdcService.getHdcStatus<LicenceWithPrisoner>(any(), any(), any()))
      .thenReturn(HdcStatuses(emptyList()))

    service.licenceActivation()

    verify(licenceRepository, times(0)).save(any())
    verify(auditService, times(0)).recordAuditEventBookingChanged(any(), any(), any(), any(), any())
    verify(telemetryService).recordActivateLicencesJobEvent(0, 0, 0, 0)
  }

  private val aLicenceEntity = createCrdLicence().copy(
    statusCode = LicenceStatus.APPROVED,
    dateOfBirth = LocalDate.of(1985, 12, 28),
    conditionalReleaseDate = null,
    actualReleaseDate = null,
    sentenceStartDate = LocalDate.of(2018, 10, 22),
    sentenceEndDate = LocalDate.of(2021, 10, 22),
    licenceStartDate = LocalDate.now(),
    licenceExpiryDate = LocalDate.of(2021, 10, 22),
    topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
    topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
  )

  private val aPrisonerSearchPrisoner = PrisonerSearchPrisoner(
    prisonerNumber = "A1234AA",
    bookingId = "54321",
    status = "INACTIVE OUT",
    mostSeriousOffence = "Robbery",
    licenceExpiryDate = LocalDate.parse("2024-09-14"),
    topupSupervisionExpiryDate = LocalDate.parse("2024-09-14"),
    homeDetentionCurfewEligibilityDate = null,
    releaseDate = LocalDate.parse("2023-09-14"),
    confirmedReleaseDate = LocalDate.parse("2023-09-14"),
    conditionalReleaseDate = LocalDate.parse("2023-09-14"),
    paroleEligibilityDate = null,
    actualParoleDate = null,
    postRecallReleaseDate = null,
    legalStatus = "SENTENCED",
    indeterminateSentence = false,
    recall = false,
    prisonId = "ABC",
    locationDescription = "HMP Moorland",
    bookNumber = "12345A",
    firstName = "Jane",
    middleNames = null,
    lastName = "Doe",
    dateOfBirth = LocalDate.parse("1985-01-01"),
    conditionalReleaseDateOverrideDate = null,
    sentenceStartDate = LocalDate.parse("2023-09-14"),
    sentenceExpiryDate = LocalDate.parse("2024-09-14"),
    topupSupervisionStartDate = null,
    croNumber = null,
  )

  val hdcLicence = createHdcLicence().copy(
    bookingId = 12345,
    statusCode = LicenceStatus.APPROVED,
  )

  val nonHdcLicence =
    aLicenceEntity.copy(id = 2, bookingId = 54322, nomsId = "A1234AB")

  val hdcPrisoner = aPrisonerSearchPrisoner.copy(
    prisonerNumber = hdcLicence.nomsId!!,
    bookingId = hdcLicence.bookingId.toString(),
    homeDetentionCurfewEligibilityDate = LocalDate.now(),
  )

  val nonHdcPrisoner = aPrisonerSearchPrisoner.copy(
    prisonerNumber = nonHdcLicence.nomsId!!,
    bookingId = nonHdcLicence.bookingId.toString(),
  )

  val aIS91CourtEventOutcome = CourtEventOutcome(
    bookingId = 54321,
    eventId = 123456,
    outcomeReasonCode = "5500",
  )

  val aRemandCourtEventOutcome = CourtEventOutcome(
    bookingId = 54321,
    eventId = 654321,
    outcomeReasonCode = "4531",
  )
}
