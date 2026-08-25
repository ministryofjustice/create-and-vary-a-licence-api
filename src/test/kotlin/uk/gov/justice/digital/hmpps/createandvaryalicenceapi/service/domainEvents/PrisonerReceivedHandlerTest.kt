package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PhoneDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.Prison
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.createTestMapper

class PrisonerReceivedHandlerTest {
  private val mapper: ObjectMapper = createTestMapper()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val staffRepository = mock<StaffRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()
  private val licenceRepository = mock<LicenceRepository>()

  private val handler = PrisonerReceivedHandler(
    mapper,
    prisonApiClient,
    staffRepository,
    auditEventRepository,
    licenceRepository,
    remandEnabled = false,
  )

  @BeforeEach
  fun reset() {
    reset(
      prisonApiClient,
      staffRepository,
      auditEventRepository,
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

    whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(aCom)

    handler.handleEvent(
      aPrisonerReceivedEventMessage(
        aLicence.nomsId!!,
        PRISON_OFFENDER_RECEIVED_EVENT_TYPE,
        prisonId = "ABC",
        reason = "TRANSFERRED",
      ),
    )

    val licenceCaptor = argumentCaptor<CrdLicence>()
    val auditCaptor = argumentCaptor<AuditEvent>()
    val nomsIdCaptor = argumentCaptor<String>()
    val statusesCaptor = argumentCaptor<List<LicenceStatus>>()

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceRepository, times(1)).findAllByNomsIdAndStatusCodeIn(nomsIdCaptor.capture(), statusesCaptor.capture())
    verify(prisonApiClient, times(1)).getPrisonInformation("ABC")
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    assertThat(nomsIdCaptor.firstValue).isEqualTo(aLicence.nomsId)
    assertThat(statusesCaptor.firstValue).containsExactlyInAnyOrder(
      LicenceStatus.IN_PROGRESS,
      LicenceStatus.SUBMITTED,
      LicenceStatus.REJECTED,
      LicenceStatus.APPROVED,
    )

    val updatedLicence = licenceCaptor.firstValue
    assertThat(updatedLicence.prisonCode).isEqualTo("ABC")
    assertThat(updatedLicence.prisonDescription).isEqualTo("ABC (HMP)")

    val auditEvent = auditCaptor.firstValue
    assertThat(auditEvent.licenceId).isEqualTo(aLicence.id)
    assertThat(auditEvent.summary).isEqualTo("Prison information changed for ${aLicence.forename} ${aLicence.surname} on prisoner receive event")
  }

  @Test
  fun `should process prisoner received event for remand`() {
    val aLicence = aLicence.copy(statusCode = LicenceStatus.ACTIVE)

    val handler = PrisonerReceivedHandler(
      mapper,
      prisonApiClient,
      staffRepository,
      auditEventRepository,
      licenceRepository,
      remandEnabled = true,
    )

    whenever(licenceRepository.findAllByNomsIdAndStatusCodeIn(any(), any())).thenReturn(
      listOf(
        aLicence,
      ),
    )

    whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)

    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(aCom)

    handler.handleEvent(
      aPrisonerReceivedEventMessage(
        aLicence.nomsId!!,
        PRISON_OFFENDER_RECEIVED_EVENT_TYPE,
        prisonId = "ABC",
        reason = "ADMISSION",
      ),
    )

    val licenceCaptor = argumentCaptor<CrdLicence>()
    val auditCaptor = argumentCaptor<AuditEvent>()
    val statusesCaptor = argumentCaptor<List<LicenceStatus>>()

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceRepository, times(1)).findAllByNomsIdAndStatusCodeIn(any(), statusesCaptor.capture())
    verify(prisonApiClient, times(1)).getPrisonInformation(any())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    val updatedLicence = licenceCaptor.firstValue
    assertThat(updatedLicence.prisonCode).isEqualTo("ABC")
    assertThat(updatedLicence.prisonDescription).isEqualTo("ABC (HMP)")

    val auditEvent = auditCaptor.firstValue
    assertThat(auditEvent.licenceId).isEqualTo(aLicence.id)
    assertThat(auditEvent.summary).isEqualTo("Prison information changed for ${aLicence.forename} ${aLicence.surname} on prisoner receive event")

    val statuses = statusesCaptor.firstValue
    assertThat(statuses).contains(LicenceStatus.ACTIVE)
    assertThat(statuses).doesNotContain(LicenceStatus.TIMED_OUT)
  }

  @Test
  fun `should update and audit only licences with changed prison code when multiple licences are returned`() {
    val licenceToUpdate = aLicence.copy(prisonCode = "MDI", prisonDescription = "Moorland (HMP)")
    val licenceToSkip = aLicence.copy(prisonCode = "ABC", prisonDescription = "ABC (HMP)")

    whenever(licenceRepository.findAllByNomsIdAndStatusCodeIn(any(), any())).thenReturn(
      listOf(licenceToUpdate, licenceToSkip),
    )
    whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(aCom)

    handler.handleEvent(
      aPrisonerReceivedEventMessage(
        aLicence.nomsId!!,
        PRISON_OFFENDER_RECEIVED_EVENT_TYPE,
        prisonId = "ABC",
        reason = "TRANSFERRED",
      ),
    )

    val licenceCaptor = argumentCaptor<CrdLicence>()
    val auditCaptor = argumentCaptor<AuditEvent>()

    verify(prisonApiClient, times(1)).getPrisonInformation("ABC")
    verify(staffRepository, times(1)).findByUsernameIgnoreCase(any())
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceRepository, times(1)).findAllByNomsIdAndStatusCodeIn(any(), any())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    assertThat(licenceCaptor.firstValue.prisonCode).isEqualTo("ABC")
    assertThat(auditCaptor.firstValue.licenceId).isEqualTo(licenceToUpdate.id)
  }

  @Test
  fun `should query valid statuses excluding ACTIVE when remand is disabled`() {
    whenever(licenceRepository.findAllByNomsIdAndStatusCodeIn(any(), any()))
      .thenReturn(emptyList())

    handler.handleEvent(
      aPrisonerReceivedEventMessage(
        aLicence.nomsId!!,
        PRISON_OFFENDER_RECEIVED_EVENT_TYPE,
        prisonId = "ABC",
        reason = "TRANSFERRED",
      ),
    )

    val statusesCaptor = argumentCaptor<List<LicenceStatus>>()
    verify(licenceRepository).findAllByNomsIdAndStatusCodeIn(any(), statusesCaptor.capture())

    assertThat(statusesCaptor.firstValue).containsExactlyInAnyOrder(
      LicenceStatus.IN_PROGRESS,
      LicenceStatus.SUBMITTED,
      LicenceStatus.REJECTED,
      LicenceStatus.APPROVED,
    )
    assertThat(statusesCaptor.firstValue).doesNotContain(
      LicenceStatus.ACTIVE,
    )

    verifyNoInteractions(auditEventRepository)
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
    verifyNoInteractions(auditEventRepository)
  }

  @Test
  fun `should not process prisoner received event when prison code is the same as the licence`() {
    whenever(licenceRepository.findAllByNomsIdAndStatusCodeIn(any(), any())).thenReturn(
      listOf(
        aLicence.copy(),
      ),
    )

    whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(
      somePrisonInformation.copy(
        prisonId = "MDI",
        description = "Moorland (HMP)",
      ),
    )

    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(aCom)

    handler.handleEvent(
      aPrisonerReceivedEventMessage(
        aLicence.nomsId!!,
        PRISON_OFFENDER_RECEIVED_EVENT_TYPE,
        prisonId = "MDI",
        reason = "TRANSFERRED",
      ),
    )

    verify(licenceRepository).findAllByNomsIdAndStatusCodeIn(any(), any())
    verify(prisonApiClient, times(1)).getPrisonInformation(any())
    verifyNoMoreInteractions(licenceRepository)
    verifyNoMoreInteractions(prisonApiClient)
    verifyNoInteractions(staffRepository)
    verifyNoInteractions(auditEventRepository)
  }

  @Test
  fun `should return early when no licences found`() {
    whenever(licenceRepository.findAllByNomsIdAndStatusCodeIn(any(), any()))
      .thenReturn(emptyList())

    handler.handleEvent(
      aPrisonerReceivedEventMessage(
        aLicence.nomsId!!,
        PRISON_OFFENDER_RECEIVED_EVENT_TYPE,
        prisonId = "ABC",
        reason = "TRANSFERRED",
      ),
    )

    verify(licenceRepository).findAllByNomsIdAndStatusCodeIn(any(), any())
    verifyNoInteractions(prisonApiClient)
    verifyNoMoreInteractions(licenceRepository)
    verifyNoInteractions(auditEventRepository)
  }

  @Test
  fun `should not process prisoner received event when reason is ADMISSION and remand is disabled`() {
    handler.handleEvent(
      aPrisonerReceivedEventMessage(
        aLicence.nomsId!!,
        PRISON_OFFENDER_RECEIVED_EVENT_TYPE,
        prisonId = "ABC",
        reason = "ADMISSION",
      ),
    )

    verifyNoInteractions(licenceRepository)
    verifyNoInteractions(prisonApiClient)
    verifyNoInteractions(staffRepository)
    verifyNoInteractions(auditEventRepository)
  }

  private fun aPrisonerReceivedEventMessage(nomsId: String, eventType: String, prisonId: String, reason: String) = mapper
    .writeValueAsString(
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
