package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prisonEvents

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.DeactivateLicenceAndVariationsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aPrisonApiPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.UpdateSentenceDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.DateChangeLicenceDeativationReason
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import java.time.LocalDate
import java.time.LocalDateTime

class SentenceDatesChangedHandlerTest {
  private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
  private val licenceRepository = mock<LicenceRepository>()
  private val licenceService = mock<LicenceService>()
  private val prisonService = mock<PrisonService>()
  private val updateSentenceDateService = mock<UpdateSentenceDateService>()

  private val sentenceDatesChangedHandler =
    SentenceDatesChangedHandler(
      objectMapper,
      licenceRepository,
      licenceService,
      prisonService,
      updateSentenceDateService,
    )

  private val bookingId = 73892L
  private val message: String = objectMapper.writeValueAsString(
    SentenceDatesChangedEvent(
      eventDatetime = LocalDateTime.now(),
      bookingId = bookingId,
      sentenceCalculationId = 1L,
    ),
  )
  val prisoner = prisonerSearchResult()
  val nomisId = prisoner.prisonerNumber
  val activeLicence = createCrdLicence().copy(statusCode = ACTIVE)
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
      DeactivateLicenceAndVariationsRequest(reason = DateChangeLicenceDeativationReason.RESENTENCED),
    )
    verify(licenceService, never()).deactivateLicenceAndVariations(
      activeLicence.id,
      DeactivateLicenceAndVariationsRequest(reason = DateChangeLicenceDeativationReason.RECALLED),
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
      DeactivateLicenceAndVariationsRequest(reason = DateChangeLicenceDeativationReason.RECALLED),
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
}
