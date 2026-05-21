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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PhoneDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.Prison
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.createTestMapper

class SupportingPrisonUpdatedHandlerTest {
  private val mapper: ObjectMapper = createTestMapper()
  private val prisonService = mock<PrisonService>()
  private val licenceRepository = mock<LicenceRepository>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val staffRepository = mock<StaffRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()

  val handler = SupportingPrisonUpdatedHandler(
    mapper,
    prisonService,
    licenceRepository,
    prisonApiClient,
    staffRepository,
    auditEventRepository,
    restrictedPatientsEnabled = true,
  )

  @BeforeEach
  fun reset() {
    reset(prisonService, licenceRepository, prisonApiClient, staffRepository, auditEventRepository)
  }

  @Test
  fun `should process supporting prison updated event`() {
    whenever(licenceRepository.findAllByNomsIdAndStatusCodeIn(any(), any())).thenReturn(
      listOf(
        aLicence,
      ),
    )

    whenever(prisonService.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisoner))

    whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)

    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(aCom)

    handler.handleEvent(
      aSupportingPrisonUpdatedEventMessage(),
    )

    verify(prisonService).searchPrisonersByNomisIds(listOf(aLicence.nomsId!!))

    val licenceCaptor = argumentCaptor<CrdLicence>()
    val auditCaptor = argumentCaptor<AuditEvent>()

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    val updatedLicence = licenceCaptor.firstValue
    assertThat(updatedLicence.prisonCode).isEqualTo("ABC")
    assertThat(updatedLicence.prisonDescription).isEqualTo("ABC (HMP)")

    val auditEvent = auditCaptor.firstValue
    assertThat(auditEvent.licenceId).isEqualTo(aLicence.id)
    assertThat(auditEvent.summary).isEqualTo("Supporting prison information changed for ${aLicence.forename} ${aLicence.surname}")
  }

  @Test
  fun `should not process supporting prison changes if they are not enabled`() {
    val handlerNotEnabled =
      SupportingPrisonUpdatedHandler(
        mapper,
        prisonService,
        licenceRepository,
        prisonApiClient,
        staffRepository,
        auditEventRepository,
        restrictedPatientsEnabled = false,
      )

    handlerNotEnabled.handleEvent(
      aSupportingPrisonUpdatedEventMessage(),
    )

    verifyNoInteractions(licenceRepository)
  }

  @Test
  fun `should return early when nomis id is null or blank`() {
    val eventWithNoNomisId = mapper.writeValueAsString(
      HMPPSDomainEvent(
        eventType = SUPPORTING_PRISON_UPDATED_EVENT_TYPE,
        additionalInformation = mapOf("prisonerNumber" to "A1234BC"),
        detailUrl = "https://example.com",
        version = 1,
        occurredAt = "2026-05-18T00:00:00.0000000Z",
        description = "Supporting prison updated",
        personReference = PersonReference(identifiers = emptyList()),
      ),
    )

    handler.handleEvent(eventWithNoNomisId)

    verifyNoInteractions(licenceRepository, prisonService)
  }

  @Test
  fun `should return early when prisoner is not a restricted patient`() {
    val nomisId = "A1234BC"

    val nonRestrictedPatient = prisonerSearchResult().copy(
      prisonerNumber = nomisId,
      restrictedPatient = false,
      status = "ACTIVE IN",
      prisonId = "MAIN123",
      supportingPrisonId = null,
    )

    whenever(prisonService.searchPrisonersByNomisIds(listOf(nomisId)))
      .thenReturn(listOf(nonRestrictedPatient))

    handler.handleEvent(aSupportingPrisonUpdatedEventMessage())

    verify(prisonService).searchPrisonersByNomisIds(listOf(nomisId))
    verifyNoInteractions(licenceRepository, prisonApiClient)
  }

  @Test
  fun `should return early when no in-flight licences found`() {
    whenever(prisonService.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisoner))

    whenever(licenceRepository.findAllByNomsIdAndStatusCodeIn(any(), any()))
      .thenReturn(emptyList())

    val eventMessage = mapper.writeValueAsString(
      HMPPSDomainEvent(
        eventType = SUPPORTING_PRISON_UPDATED_EVENT_TYPE,
        additionalInformation = mapOf("prisonerNumber" to aPrisoner.prisonerNumber),
        detailUrl = "https://example.com",
        version = 1,
        occurredAt = "2026-05-18T00:00:00.0000000Z",
        description = "Supporting prison updated",
        personReference = PersonReference(identifiers = listOf(Identifiers("NOMS", aPrisoner.prisonerNumber))),
      ),
    )

    handler.handleEvent(eventMessage)

    verify(prisonService).searchPrisonersByNomisIds(listOf(aPrisoner.prisonerNumber))
    verify(licenceRepository).findAllByNomsIdAndStatusCodeIn(any(), any())
    verifyNoInteractions(prisonApiClient)
    verifyNoMoreInteractions(licenceRepository)
  }

  private fun aSupportingPrisonUpdatedEventMessage(): String = mapper.writeValueAsString(
    HMPPSDomainEvent(
      eventType = SUPPORTING_PRISON_UPDATED_EVENT_TYPE,
      additionalInformation = mapOf(
        "prisonerNumber" to "A1234BC",
      ),
      version = 1,
      occurredAt = "2026-05-18T00:00:00.0000000Z",
      description = "Supporting prisoner changed for restricted patient",
      personReference = PersonReference(
        identifiers = listOf(Identifiers("NOMS", "A1234BC")),
      ),
    ),
  )

  private companion object {
    val aLicence = createCrdLicence().copy(nomsId = "A1234BC")

    val aPrisoner = prisonerSearchResult().copy(prisonerNumber = "A1234BC", restrictedPatient = true, prisonId = "OUT", status = "INACTIVE OUT", supportingPrisonId = "MDI")

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
