package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonInformationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonInformationService.UpdateType.MOVEMENT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PhoneDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.Prison
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.createTestMapper

class PrisonerReceivedHandlerTest {
  private val mapper: ObjectMapper = createTestMapper()
  private val prisonInformationService = mock<PrisonInformationService>()
  private val licenceRepository = mock<LicenceRepository>()

  private val handler = PrisonerReceivedHandler(
    mapper,
    prisonInformationService,
    licenceRepository,
    remandEnabled = true,
  )

  @BeforeEach
  fun reset() {
    reset(
      prisonInformationService,
      licenceRepository,
    )
  }

  @Test
  fun `should process prisoner received event`() {
    whenever(licenceRepository.findAllByNomsIdAndStatusCodeIn(any(), any())).thenReturn(
      listOf(
        aLicence.copy(),
      ),
    )

    handler.handleEvent(
      aPrisonerReceivedEventMessage(
        aLicence.nomsId!!,
        PRISON_OFFENDER_RECEIVED_EVENT_TYPE,
        prisonId = "ABC",
        reason = "TRANSFERRED",
      ),
    )

    argumentCaptor<List<Licence>> {
      verify(prisonInformationService, times(1)).updatePrisonInformation(eq(MOVEMENT), capture(), eq("ABC"))

      assertThat(firstValue.size).isEqualTo(1)
      assertThat(firstValue.first().id).isEqualTo(aLicence.id)
      assertThat(firstValue.first().nomsId).isEqualTo(aLicence.nomsId)
    }
  }

  @Test
  fun `should process prisoner received event for remand`() {
    val aLicence = aLicence.copy(statusCode = LicenceStatus.ACTIVE)

    whenever(licenceRepository.findAllByNomsIdAndStatusCodeIn(any(), any())).thenReturn(
      listOf(
        aLicence,
      ),
    )

    handler.handleEvent(
      aPrisonerReceivedEventMessage(
        aLicence.nomsId!!,
        PRISON_OFFENDER_RECEIVED_EVENT_TYPE,
        prisonId = "ABC",
        reason = "ADMISSION",
      ),
    )

    verify(prisonInformationService, times(1)).updatePrisonInformation(eq(MOVEMENT), any<List<Licence>>(), eq("ABC"))
  }

  @Test
  fun `should not process prisoner received when remand is false`() {
    val handler = PrisonerReceivedHandler(
      mapper,
      prisonInformationService,
      licenceRepository,
      remandEnabled = false,
    )

    handler.handleEvent(
      aPrisonerReceivedEventMessage(
        aLicence.nomsId!!,
        PRISON_OFFENDER_RECEIVED_EVENT_TYPE,
        prisonId = "ABC",
        reason = "REASON",
      ),
    )

    verifyNoInteractions(licenceRepository)
    verifyNoInteractions(prisonInformationService)
  }

  @Test
  fun `should not process prisoner received event when reason is not valid`() {
    handler.handleEvent(
      aPrisonerReceivedEventMessage(
        aLicence.nomsId!!,
        PRISON_OFFENDER_RECEIVED_EVENT_TYPE,
        prisonId = "ABC",
        reason = "REASON",
      ),
    )

    verifyNoInteractions(licenceRepository)
    verifyNoInteractions(prisonInformationService)
  }

  @Test
  fun `should return early when no licences found`() {
    whenever(licenceRepository.findAllByNomsIdAndStatusCodeIn(any(), any())).thenReturn(emptyList())

    handler.handleEvent(
      aPrisonerReceivedEventMessage(
        aLicence.nomsId!!,
        PRISON_OFFENDER_RECEIVED_EVENT_TYPE,
        prisonId = "ABC",
        reason = "TRANSFERRED",
      ),
    )

    verify(licenceRepository).findAllByNomsIdAndStatusCodeIn(any(), any())

    verifyNoMoreInteractions(licenceRepository)
    verifyNoInteractions(prisonInformationService)
  }

  private fun aPrisonerReceivedEventMessage(nomsId: String, eventType: String, prisonId: String, reason: String) = mapper.writeValueAsString(
    HMPPSDomainEvent(
      eventType = eventType,
      additionalInformation = mapOf(
        "currentLocation" to "IN_PRISON",
        "currentPrisonStatus" to "UNDER_PRISON_CARE",
        "details" to "ACTIVE IN: ABC-D",
        "nomisMovementReasonCode" to "D",
        "nomsNumber" to nomsId,
        "prisonId" to prisonId,
        "reason" to reason,
      ),
      version = 0,
      occurredAt = "2026-08-24T00:00:00Z",
      description = "A prisoner has been received into prison",
      personReference = PersonReference(identifiers = listOf(Identifiers("NOMS", nomsId))),
    ),
  )

  private companion object {
    val aLicence = createCrdLicence()

    val somePrisonInformation = Prison(
      prisonId = "ABC",
      description = "ABC (HMP)",
      phoneDetails = listOf(
        PhoneDetail(
          phoneId = 1,
          number = "0123 456 7890",
          type = "BUS",
          ext = null,
        ),
        PhoneDetail(
          phoneId = 2,
          number = "0800 123 4567",
          type = "FAX",
          ext = null,
        ),
      ),
    )

    val aCom = CommunityOffenderManager(
      id = 1,
      staffIdentifier = 2000,
      staffCode = "test-code-1",
      username = "tcom1",
      email = "testemail1@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )
  }
}
