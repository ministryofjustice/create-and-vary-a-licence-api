package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prisonEvents

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.DeactivateLicenceAndVariationsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aPrisonApiPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.UpdateSentenceDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceDeactivationReason
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.createTestMapper
import java.time.LocalDate
import java.time.LocalDateTime

class SentenceDatesChangedHandlerTest {
  private val mapper: ObjectMapper = createTestMapper()
  private val licenceRepository = mock<LicenceRepository>()
  private val licenceService = mock<LicenceService>()
  private val prisonService = mock<PrisonService>()
  private val updateSentenceDateService = mock<UpdateSentenceDateService>()

  private val sentenceDatesChangedHandler =
    SentenceDatesChangedHandler(
      mapper,
      licenceRepository,
      licenceService,
      prisonService,
      updateSentenceDateService,
    )

  private val bookingId = 73892L
  private val message: String = mapper.writeValueAsString(
    SentenceDatesChangedEvent(
      eventDatetime = LocalDateTime.now(),
      bookingId = bookingId,
      sentenceCalculationId = 1L,
    ),
  )
  val prisoner = prisonerSearchResult()
  val nomisId = prisoner.prisonerNumber
  val activeLicence = createCrdLicence().copy(statusCode = ACTIVE, bookingId = bookingId)
  val prisonApiPrisoner = aPrisonApiPrisoner()

  @BeforeEach
  fun setup() {
    reset(licenceService, prisonService, updateSentenceDateService)
  }

  @Test
  fun `should deactivate an active licence if an offender has been resentenced`() {
    whenever(prisonService.getPrisonerDetail(nomisId)).thenReturn(prisonApiPrisoner)
    whenever(prisonService.searchPrisonersByBookingIds(listOf(bookingId))).thenReturn(listOf(prisoner))
    whenever(prisonService.getPrisonerLatestSentenceStartDate(bookingId)).thenReturn(
      activeLicence.licenceStartDate?.plusDays(
        1,
      ),
    )

    whenever(
      licenceRepository.findAllByNomsIdAndStatusCodeIn(
        nomisId,
        listOf(
          ACTIVE,
        ),
      ),
    ).thenReturn(listOf(activeLicence))

    sentenceDatesChangedHandler.handleEvent(message)

    verify(licenceService).deactivateLicenceAndVariations(
      activeLicence.id,
      DeactivateLicenceAndVariationsRequest(reason = LicenceDeactivationReason.RESENTENCED),
    )
    verify(licenceService, never()).deactivateLicenceAndVariations(
      activeLicence.id,
      DeactivateLicenceAndVariationsRequest(reason = LicenceDeactivationReason.RECALLED),
    )
  }

  @Test
  fun `should deactivate the active licence and any variations if an offender has been recalled`() {
    whenever(prisonService.searchPrisonersByBookingIds(listOf(bookingId))).thenReturn(listOf(prisoner))
    val prisonApiPrisonerFuturePrrd = prisonApiPrisoner.copy(
      sentenceDetail = prisonApiPrisoner.sentenceDetail.copy(
        postRecallReleaseDate = LocalDate.now().plusDays(1),
      ),
    )
    whenever(
      licenceRepository.findAllByNomsIdAndStatusCodeIn(
        prisoner.prisonerNumber,
        listOf(
          ACTIVE,
        ),
      ),
    ).thenReturn(listOf(activeLicence))

    whenever(prisonService.getPrisonerDetail(nomisId)).thenReturn(prisonApiPrisonerFuturePrrd)

    sentenceDatesChangedHandler.handleEvent(message)

    verify(licenceService).deactivateLicenceAndVariations(
      activeLicence.id,
      DeactivateLicenceAndVariationsRequest(reason = LicenceDeactivationReason.RECALLED),
    )
  }

  @Test
  fun `should update the sentence dates on any non active licence`() {
    whenever(prisonService.searchPrisonersByBookingIds(listOf(bookingId))).thenReturn(listOf(prisoner))
    val prisonApiPrisonerFuturePrrd = prisonApiPrisoner.copy(
      sentenceDetail = prisonApiPrisoner.sentenceDetail.copy(
        postRecallReleaseDate = LocalDate.now().plusDays(1),
      ),
    )
    whenever(
      licenceService.findLicencesMatchingCriteria(
        LicenceQueryObject(
          nomsIds = listOf(nomisId),
          statusCodes = listOf(
            ACTIVE,
          ),
        ),
      ),
    ).thenReturn(emptyList())
    val inProgressLicence = createCrdLicence()
    whenever(
      licenceRepository.findAllByNomsIdAndStatusCodeIn(
        nomisId,
        listOf(
          LicenceStatus.IN_PROGRESS,
          LicenceStatus.SUBMITTED,
          LicenceStatus.REJECTED,
          LicenceStatus.APPROVED,
          LicenceStatus.TIMED_OUT,
        ),
      ),
    ).thenReturn(listOf(inProgressLicence))

    whenever(prisonService.getPrisonerDetail(nomisId)).thenReturn(prisonApiPrisonerFuturePrrd)

    sentenceDatesChangedHandler.handleEvent(message)

    verify(updateSentenceDateService).updateSentenceDates(inProgressLicence.id)
  }

  @Test
  fun `should delegate CRD update to licence service when active HDC licence has a new CRD`() {
    val activeHdcLicence = createHdcLicence().copy(statusCode = ACTIVE, bookingId = bookingId, nomsId = nomisId)
    val newCrd = LocalDate.now().plusDays(30)
    val prisonApiPrisonerNewCrd = prisonApiPrisoner.copy(
      sentenceDetail = prisonApiPrisoner.sentenceDetail.copy(conditionalReleaseDate = newCrd),
    )

    whenever(prisonService.searchPrisonersByBookingIds(listOf(bookingId))).thenReturn(listOf(prisoner))
    whenever(prisonService.getPrisonerDetail(nomisId)).thenReturn(prisonApiPrisonerNewCrd)
    whenever(prisonService.getPrisonerLatestSentenceStartDate(bookingId)).thenReturn(null)
    whenever(
      licenceRepository.findAllByNomsIdAndStatusCodeIn(nomisId, listOf(ACTIVE)),
    ).thenReturn(listOf(activeHdcLicence))

    sentenceDatesChangedHandler.handleEvent(message)

    verify(licenceService).updateCrdForHdcLicences(activeHdcLicence.id, newCrd)
  }

  @Test
  fun `should not delegate CRD update for a standard CRD licence`() {
    whenever(prisonService.getPrisonerDetail(nomisId)).thenReturn(prisonApiPrisoner)
    whenever(prisonService.searchPrisonersByBookingIds(listOf(bookingId))).thenReturn(listOf(prisoner))
    whenever(prisonService.getPrisonerLatestSentenceStartDate(bookingId)).thenReturn(null)
    whenever(
      licenceRepository.findAllByNomsIdAndStatusCodeIn(nomisId, listOf(ACTIVE)),
    ).thenReturn(listOf(activeLicence))

    sentenceDatesChangedHandler.handleEvent(message)

    verify(licenceService, never()).updateCrdForHdcLicences(any(), any(), any())
  }

  @Test
  fun `should delegate CRD update to licence service for a pre-release HDC licence and not sync via updateSentenceDateService`() {
    whenever(prisonService.searchPrisonersByBookingIds(listOf(bookingId))).thenReturn(listOf(prisoner))
    val newCrd = LocalDate.now().plusDays(30)
    val prisonApiPrisonerNewCrd = prisonApiPrisoner.copy(
      sentenceDetail = prisonApiPrisoner.sentenceDetail.copy(conditionalReleaseDate = newCrd),
    )
    whenever(
      licenceService.findLicencesMatchingCriteria(
        LicenceQueryObject(
          nomsIds = listOf(nomisId),
          statusCodes = listOf(
            ACTIVE,
          ),
        ),
      ),
    ).thenReturn(emptyList())
    val preReleaseHdcLicence = createHdcLicence(id = 2).copy(nomsId = nomisId)
    val preReleaseNonHdcLicence = createCrdLicence()
    whenever(
      licenceRepository.findAllByNomsIdAndStatusCodeIn(
        nomisId,
        listOf(
          LicenceStatus.IN_PROGRESS,
          LicenceStatus.SUBMITTED,
          LicenceStatus.REJECTED,
          LicenceStatus.APPROVED,
          LicenceStatus.TIMED_OUT,
        ),
      ),
    ).thenReturn(listOf(preReleaseHdcLicence, preReleaseNonHdcLicence))

    whenever(prisonService.getPrisonerDetail(nomisId)).thenReturn(prisonApiPrisonerNewCrd)

    sentenceDatesChangedHandler.handleEvent(message)

    verify(licenceService).updateCrdForHdcLicences(preReleaseHdcLicence.id, newCrd, includeVariations = false)
    verify(updateSentenceDateService, never()).updateSentenceDates(preReleaseHdcLicence.id)
    verify(updateSentenceDateService).updateSentenceDates(preReleaseNonHdcLicence.id)
  }

  @Test
  fun `should delegate CRD update to licence service for a SUBMITTED pre-release HDC licence`() {
    whenever(prisonService.searchPrisonersByBookingIds(listOf(bookingId))).thenReturn(listOf(prisoner))
    val newCrd = LocalDate.now().plusDays(30)
    val prisonApiPrisonerNewCrd = prisonApiPrisoner.copy(
      sentenceDetail = prisonApiPrisoner.sentenceDetail.copy(conditionalReleaseDate = newCrd),
    )
    whenever(
      licenceService.findLicencesMatchingCriteria(
        LicenceQueryObject(
          nomsIds = listOf(nomisId),
          statusCodes = listOf(
            ACTIVE,
          ),
        ),
      ),
    ).thenReturn(emptyList())
    val submittedHdcLicence = createHdcLicence(id = 3).copy(nomsId = nomisId, statusCode = LicenceStatus.SUBMITTED)
    whenever(
      licenceRepository.findAllByNomsIdAndStatusCodeIn(
        nomisId,
        listOf(
          LicenceStatus.IN_PROGRESS,
          LicenceStatus.SUBMITTED,
          LicenceStatus.REJECTED,
          LicenceStatus.APPROVED,
          LicenceStatus.TIMED_OUT,
        ),
      ),
    ).thenReturn(listOf(submittedHdcLicence))

    whenever(prisonService.getPrisonerDetail(nomisId)).thenReturn(prisonApiPrisonerNewCrd)

    sentenceDatesChangedHandler.handleEvent(message)

    verify(licenceService).updateCrdForHdcLicences(submittedHdcLicence.id, newCrd, includeVariations = false)
    verify(updateSentenceDateService, never()).updateSentenceDates(submittedHdcLicence.id)
  }
}
