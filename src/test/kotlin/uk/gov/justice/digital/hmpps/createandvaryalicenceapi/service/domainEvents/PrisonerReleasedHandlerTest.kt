package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.PrisonerReleasedHandler.AdditionalInformationPrisonerReleased
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.PrisonerReleasedHandler.HMPPSPrisonerReleasedEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.Companion.PRE_RELEASE_STATUSES
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.createTestMapper
import java.time.LocalDate

private const val PRISON_NUMBER = "A1234AA"

class PrisonerReleasedHandlerTest {
  private val mapper: ObjectMapper = createTestMapper()
  private val licenceRepository = mock<LicenceRepository>()
  private val licenceService = mock<LicenceService>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val hdcService = mock<HdcService>()

  val handler = PrisonerReleasedHandler(
    mapper,
    licenceRepository,
    licenceService,
    hdcService,
    prisonerSearchApiClient,
    handlerEnabled = true,
  )

  @BeforeEach
  fun reset() {
    reset(
      licenceRepository,
      licenceService,
      hdcService,
      prisonerSearchApiClient,
    )
  }

  @Test
  fun `does not process prisoner released event if not RELEASED`() {
    whenever {
      licenceRepository.findAllByNomsIdAndStatusCodeIn(PRISON_NUMBER, PRE_RELEASE_STATUSES.toList())
    }.thenReturn(listOf(createCrdLicence().copy(id = 1L, statusCode = APPROVED)))

    whenever {
      prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(PRISON_NUMBER))
    }.thenReturn(listOf(prisonerSearchResult().copy(homeDetentionCurfewActualDate = null)))

    handler.handleEvent(aPrisonerReleasedEvent().replace("RELEASED", "TRANSFERRED"))

    verifyNoMoreInteractions(licenceService)
  }

  @Test
  fun `should process prisoner released event and activate an approved licence`() {
    whenever {
      licenceRepository.findAllByNomsIdAndStatusCodeIn(PRISON_NUMBER, PRE_RELEASE_STATUSES.toList())
    }.thenReturn(listOf(createCrdLicence().copy(id = 1L, statusCode = APPROVED)))

    whenever {
      prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(PRISON_NUMBER))
    }.thenReturn(listOf(prisonerSearchResult().copy(homeDetentionCurfewActualDate = null)))

    handler.handleEvent(aPrisonerReleasedEvent())

    verify(licenceService).updateLicenceStatus(1L, handler.activate)
    verifyNoMoreInteractions(licenceService)
  }

  @Test
  fun `should process prisoner released event and deactivate an approved CRD licence that is eligible for HDC`() {
    whenever {
      licenceRepository.findAllByNomsIdAndStatusCodeIn(PRISON_NUMBER, PRE_RELEASE_STATUSES.toList())
    }.thenReturn(listOf(createCrdLicence().copy(id = 1L, statusCode = APPROVED)))

    whenever { hdcService.isApprovedForHdc(any(), anyOrNull()) }.thenReturn(true)

    whenever {
      prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(PRISON_NUMBER))
    }.thenReturn(listOf(prisonerSearchResult().copy(homeDetentionCurfewActualDate = LocalDate.now())))

    handler.handleEvent(aPrisonerReleasedEvent())

    verify(licenceService).updateLicenceStatus(1L, handler.inactivate)
    verifyNoMoreInteractions(licenceService)
  }

  @Test
  fun `should process prisoner released event and activate an approved HDC licence that is eligible for HDC`() {
    whenever {
      licenceRepository.findAllByNomsIdAndStatusCodeIn(PRISON_NUMBER, PRE_RELEASE_STATUSES.toList())
    }.thenReturn(listOf(createHdcLicence().copy(id = 1L, statusCode = APPROVED)))

    whenever { hdcService.isApprovedForHdc(any(), anyOrNull()) }.thenReturn(true)

    whenever {
      prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(PRISON_NUMBER))
    }.thenReturn(listOf(prisonerSearchResult().copy(homeDetentionCurfewActualDate = LocalDate.now())))

    handler.handleEvent(aPrisonerReleasedEvent())

    verify(licenceService).updateLicenceStatus(1L, handler.activate)
    verifyNoMoreInteractions(licenceService)
  }

  @Test
  fun `should process prisoner released event and inactive any non-approved licence`() {
    whenever {
      licenceRepository.findAllByNomsIdAndStatusCodeIn(PRISON_NUMBER, PRE_RELEASE_STATUSES.toList())
    }.thenReturn(
      listOf(
        createCrdLicence().copy(id = 1L, statusCode = APPROVED),
        createCrdLicence().copy(id = 2L, statusCode = IN_PROGRESS),
      ),
    )

    whenever {
      prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(PRISON_NUMBER))
    }.thenReturn(listOf(prisonerSearchResult().copy(homeDetentionCurfewActualDate = null)))

    handler.handleEvent(aPrisonerReleasedEvent())

    verify(licenceService).updateLicenceStatus(1L, handler.activate)
    verify(licenceService).inactivateLicences(
      listOf(createCrdLicence().copy(id = 2L, statusCode = IN_PROGRESS)),
      DEACTIVATION_REASON,
      deactivateInProgressVersions = true,
    )
  }

  private fun aPrisonerReleasedEvent() = mapper
    .writeValueAsString(
      HMPPSPrisonerReleasedEvent(
        eventType = PRISON_OFFENDER_RELEASED_EVENT_TYPE,
        additionalInformation = AdditionalInformationPrisonerReleased(
          nomsNumber = PRISON_NUMBER,
          reason = "RELEASED",
        ),
        version = 0,
        occurredAt = "2023-12-05T00:00:00Z",
        description = "prisoner merged",
      ),
    )
}
