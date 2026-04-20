package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import tools.jackson.module.kotlin.jacksonObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createPrrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.RecallType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.EligibleKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT

class RecallUpdatedHandlerTest {
  private val mapper = jacksonObjectMapper()
  private val licenceRepository = mock<LicenceRepository>()
  private val licenceService = mock<LicenceService>()
  private val prisonService = mock<PrisonService>()

  private val nomisId = "A1234AA"
  private val prisonerSearchResult = prisonerSearchResult()
  private val bookingId = prisonerSearchResult.bookingId!!.toLong()

  private val handler = RecallUpdatedHandler(mapper, licenceRepository, licenceService, prisonService, true)

  @BeforeEach
  fun reset() {
    reset(licenceRepository, licenceService, prisonService)
  }

  @Test
  fun `should change the eligible kind of a fixed term recall licence when the offender has a new standard recall sentence`() {
    val licence = createPrrdLicence()

    whenever(prisonService.searchPrisonersByNomisIds(listOf(nomisId))).thenReturn(listOf(prisonerSearchResult))
    whenever(prisonService.getRecallType(bookingId)).thenReturn(RecallType.STANDARD)
    whenever(
      licenceRepository.findAllByBookingIdAndStatusCodeInAndKindIn(
        bookingId,
        listOf(IN_PROGRESS, SUBMITTED, APPROVED, TIMED_OUT),
        listOf(LicenceKind.PRRD),
      ),
    ).thenReturn(listOf(licence))

    handler.handleEvent(aRecallUpdatedEventMessage())

    verify(licenceService).updateLicenceKind(
      licence,
      LicenceKind.PRRD,
      EligibleKind.STANDARD,
    )
  }

  @Test
  fun `should not process standard recalls if they are not enabled`() {
    val standardRecallNotEnabledHandler =
      RecallUpdatedHandler(mapper, licenceRepository, licenceService, prisonService, false)

    val licence = createPrrdLicence()

    whenever(prisonService.searchPrisonersByNomisIds(listOf(nomisId))).thenReturn(listOf(prisonerSearchResult))
    whenever(prisonService.getRecallType(bookingId)).thenReturn(RecallType.STANDARD)
    whenever(
      licenceRepository.findAllByBookingIdAndStatusCodeInAndKindIn(
        bookingId,
        listOf(IN_PROGRESS, SUBMITTED, APPROVED, TIMED_OUT),
        listOf(LicenceKind.PRRD),
      ),
    ).thenReturn(listOf(licence))

    standardRecallNotEnabledHandler.handleEvent(aRecallUpdatedEventMessage())

    verifyNoInteractions(licenceService)
  }

  fun aRecallUpdatedEventMessage(): String = mapper.writeValueAsString(
    HMPPSDomainEvent(
      eventType = RECALL_UPDATED_EVENT_TYPE,
      additionalInformation = mapOf(
        "source" to "NOMIS",
        "recallId" to "87770250-bf64-4b25-aeb0-146d2185be99",
        "previousRecallId" to "d32d9642-5b10-4638-9852-9c5c90234c86",
        "sentenceIds" to "[c2a7159c-383a-4a98-9f00-7c410b6e1900]",
        "previousSentenceIds" to "[fd0bff49-aa61-4564-aff5-9f3ba222b9df]",
      ),
      detailUrl = "https://remand-and-sentencing-api-dev.hmpps.service.justice.gov.uk/recall/87770250-bf64-4b25-aeb0-146d2185be99",
      version = 1,
      occurredAt = "2026-03-27T09:27:38.6679417Z",
      description = "Recall updated",
      personReference = PersonReference(
        identifiers = listOf(Identifiers("NOMS", "A1234AA")),
      ),
    ),
  )
}
