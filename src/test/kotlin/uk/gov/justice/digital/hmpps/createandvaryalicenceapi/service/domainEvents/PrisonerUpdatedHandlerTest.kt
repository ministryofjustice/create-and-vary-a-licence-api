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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.OffenderService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.events.UpdateOffenderDetailsEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PhoneDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.Prison
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.createTestMapper

class PrisonerUpdatedHandlerTest {
  private val mapper: ObjectMapper = createTestMapper()
  private val offenderService = mock<OffenderService>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val licenceRepository = mock<LicenceRepository>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val staffRepository = mock<StaffRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()

  val handler = PrisonerUpdatedHandler(
    mapper,
    offenderService,
    prisonerSearchApiClient,
    licenceRepository,
    prisonApiClient,
    staffRepository,
    auditEventRepository,
    restrictedPatientsEnabled = true,
  )

  @BeforeEach
  fun reset() {
    reset(
      offenderService,
      prisonerSearchApiClient,
      licenceRepository,
      prisonApiClient,
      staffRepository,
      auditEventRepository,
    )
  }

  @Test
  fun `should process prisoner updated event`() {
    val nomsId = "A1234AA"

    val prisoner = prisonerSearchResult().copy(firstName = "ABCDEF", middleNames = null)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(nomsId))).thenReturn(listOf(prisoner))

    handler.handleEvent(
      aPrisonerUpdatedEventMessage(
        nomsId,
        COM_ALLOCATED_EVENT_TYPE,
        listOf(DiffCategory.ALERTS, DiffCategory.PERSONAL_DETAILS),
      ),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(listOf(nomsId))

    verify(offenderService).updateOffenderDetails(
      nomsId,
      UpdateOffenderDetailsEvent(
        forename = prisoner.firstName.convertToTitleCase(),
        middleNames = "",
        surname = prisoner.lastName.convertToTitleCase(),
        dateOfBirth = prisoner.dateOfBirth,
      ),
    )
  }

  @Test
  fun `should not update prisoner details when categories do not include person details`() {
    val nomsId = "A1294AC"

    handler.handleEvent(
      aPrisonerUpdatedEventMessage(
        nomsId,
        COM_ALLOCATED_EVENT_TYPE,
        listOf(DiffCategory.PHYSICAL_DETAILS, DiffCategory.INCENTIVE_LEVEL),
      ),
    )

    verifyNoInteractions(prisonerSearchApiClient)
    verifyNoInteractions(offenderService)
  }

  @Test
  fun `should process supporting prison updated event`() {
    whenever(licenceRepository.findAllByNomsIdAndStatusCodeIn(any(), any())).thenReturn(
      listOf(
        aLicence,
      ),
    )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisoner))

    whenever(prisonApiClient.getPrisonInformation(any())).thenReturn(somePrisonInformation)

    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(aCom)

    handler.handleEvent(
      aPrisonerUpdatedEventMessage(
        aPrisoner.prisonerNumber,
        PRISONER_UPDATED_EVENT_TYPE,
        listOf(DiffCategory.RESTRICTED_PATIENT),
      ),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(listOf(aLicence.nomsId!!))

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
      PrisonerUpdatedHandler(
        mapper,
        offenderService,
        prisonerSearchApiClient,
        licenceRepository,
        prisonApiClient,
        staffRepository,
        auditEventRepository,
        restrictedPatientsEnabled = false,
      )

    handlerNotEnabled.handleEvent(
      aPrisonerUpdatedEventMessage(
        aPrisoner.prisonerNumber,
        PRISONER_UPDATED_EVENT_TYPE,
        listOf(DiffCategory.RESTRICTED_PATIENT),
      ),
    )

    verifyNoInteractions(licenceRepository)
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

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(nomisId)))
      .thenReturn(listOf(nonRestrictedPatient))

    handler.handleEvent(
      aPrisonerUpdatedEventMessage(
        aPrisoner.prisonerNumber,
        PRISONER_UPDATED_EVENT_TYPE,
        listOf(DiffCategory.RESTRICTED_PATIENT),
      ),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(listOf(nomisId))
    verifyNoInteractions(licenceRepository, prisonApiClient)
  }

  @Test
  fun `should return early when no in-flight licences found`() {
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(aPrisoner))

    whenever(licenceRepository.findAllByNomsIdAndStatusCodeIn(any(), any()))
      .thenReturn(emptyList())

    handler.handleEvent(
      aPrisonerUpdatedEventMessage(
        aPrisoner.prisonerNumber,
        PRISONER_UPDATED_EVENT_TYPE,
        listOf(DiffCategory.RESTRICTED_PATIENT),
      ),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(listOf(aPrisoner.prisonerNumber))
    verify(licenceRepository).findAllByNomsIdAndStatusCodeIn(any(), any())
    verifyNoInteractions(prisonApiClient)
    verifyNoMoreInteractions(licenceRepository)
  }

  private fun aPrisonerUpdatedEventMessage(nomsId: String, eventType: String, categories: List<DiffCategory>) = mapper
    .writeValueAsString(
      HMPPSPrisonerUpdatedEvent(
        eventType = eventType,
        additionalInformation = AdditionalInformationPrisonerUpdated(
          nomsNumber = nomsId,
          categoriesChanged = categories,
        ),
        version = 0,
        occurredAt = "2023-12-05T00:00:00Z",
        description = "prisoner updated",
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
