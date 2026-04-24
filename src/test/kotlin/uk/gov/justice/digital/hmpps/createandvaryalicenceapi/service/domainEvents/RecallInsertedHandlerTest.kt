package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import tools.jackson.module.kotlin.jacksonObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.DeactivateLicenceAndVariationsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.BookingSentenceAndRecallTypes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.RecallType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceAndRecallType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceRecallType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceDeactivationReason
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE

class RecallInsertedHandlerTest {

  private val mapper = jacksonObjectMapper()
  private val licenceRepository = mock<LicenceRepository>()
  private val licenceService = mock<LicenceService>()
  private val prisonService = mock<PrisonService>()

  private val handler = RecallInsertedHandler(mapper, licenceRepository, licenceService, prisonService, true)

  private val nomisId = "A1234AA"
  private val prisonerSearchResult = prisonerSearchResult()
  private val bookingId = prisonerSearchResult.bookingId!!.toLong()

  @BeforeEach
  fun reset() {
    reset(licenceRepository, licenceService, prisonService)
  }

  @Test
  fun `should deactivate an active licence linked to an offender that has a fixed term recall sentence`() {
    val licence = createCrdLicence()

    whenever(prisonService.searchPrisonersByNomisIds(listOf(nomisId))).thenReturn(listOf(prisonerSearchResult))
    whenever(prisonService.getRecallType(bookingId)).thenReturn(RecallType.FIXED_TERM)
    whenever(
      licenceRepository.findAllByNomsIdAndStatusCodeIn(
        nomisId,
        listOf(
          ACTIVE,
        ),
      ),
    ).thenReturn(listOf(licence))

    handler.handleEvent(mapper.writeValueAsString(aRecallInsertedEvent()))

    verify(
      licenceService,
    ).deactivateLicenceAndVariations(
      licence.id,
      DeactivateLicenceAndVariationsRequest(LicenceDeactivationReason.FIXED_TERM),
    )
  }

  @Test
  fun `should deactivate an active licence linked to an offender that has a standard recall sentence`() {
    val licence = createCrdLicence()

    whenever(prisonService.searchPrisonersByNomisIds(listOf(nomisId))).thenReturn(listOf(prisonerSearchResult))
    whenever(prisonService.getRecallType(bookingId)).thenReturn(RecallType.STANDARD)
    whenever(
      licenceRepository.findAllByNomsIdAndStatusCodeIn(
        nomisId,
        listOf(
          ACTIVE,
        ),
      ),
    ).thenReturn(listOf(licence))

    handler.handleEvent(mapper.writeValueAsString(aRecallInsertedEvent()))

    verify(
      licenceService,
    ).deactivateLicenceAndVariations(
      licence.id,
      DeactivateLicenceAndVariationsRequest(LicenceDeactivationReason.STANDARD_RECALL),
    )
  }

  @Test
  fun `does nothing is the offender does not have an active licence`() {
    whenever(prisonService.searchPrisonersByNomisIds(listOf(nomisId))).thenReturn(listOf(prisonerSearchResult))
    whenever(prisonService.getSentenceAndRecallTypes(bookingId)).thenReturn(fixedTermRecallSentenceAndRecalls(bookingId))
    whenever(
      licenceRepository.findAllByNomsIdAndStatusCodeIn(
        nomisId,
        listOf(
          ACTIVE,
        ),
      ),
    ).thenReturn(emptyList())

    handler.handleEvent(mapper.writeValueAsString(aRecallInsertedEvent()))

    verifyNoInteractions(
      licenceService,
    )
  }

  @Test
  fun `does nothing if an offender has a standard recall sentence but standard recalls aren't enabled`() {
    val licence = createCrdLicence()
    val recallsDisabledHandler = RecallInsertedHandler(mapper, licenceRepository, licenceService, prisonService, false)

    whenever(prisonService.searchPrisonersByNomisIds(listOf(nomisId))).thenReturn(listOf(prisonerSearchResult))
    whenever(prisonService.getSentenceAndRecallTypes(bookingId)).thenReturn(standardRecallSentenceAndRecalls(bookingId))
    whenever(
      licenceRepository.findAllByNomsIdAndStatusCodeIn(
        nomisId,
        listOf(
          ACTIVE,
        ),
      ),
    ).thenReturn(listOf(licence))

    recallsDisabledHandler.handleEvent(mapper.writeValueAsString(aRecallInsertedEvent()))

    verifyNoInteractions(
      licenceService,
    )
  }

  fun aRecallInsertedEvent() = HMPPSDomainEvent(
    eventType = RECALL_INSERTED_EVENT_TYPE,
    additionalInformation = mapOf(
      "source" to "NOMIS",
      "recallId" to "dfd1e5c2-318c-4f56-b4c8-2d236696e52c",
      "sentenceIds" to "[c2a7159c-383a-4a98-9f00-7c410b6e1900]",
    ),
    detailUrl = "https://remand-and-sentencing-api-dev.hmpps.service.justice.gov.uk/recall/dfd1e5c2-318c-4f56-b4c8-2d236696e52c",
    version = 1,
    occurredAt = "2026-03-27T09:27:38.6679417Z",
    description = "Recall inserted",
    personReference = PersonReference(
      identifiers = listOf(Identifiers("NOMS", "A1234AA")),
    ),
  )

  private fun fixedTermRecallSentenceAndRecalls(bookingId: Long) = BookingSentenceAndRecallTypes(
    bookingId,
    listOf(
      SentenceAndRecallType(
        "FTR_56ORA",
        SentenceRecallType("FIXED_TERM_RECALL_56", isStandardRecall = false, isFixedTermRecall = true),
      ),
    ),
  )

  private fun standardRecallSentenceAndRecalls(bookingId: Long) = BookingSentenceAndRecallTypes(
    bookingId,
    listOf(
      SentenceAndRecallType(
        "FTR_56ORA",
        SentenceRecallType("STANDARD_RECALL_255", isStandardRecall = true, isFixedTermRecall = false),
      ),
    ),
  )
}
