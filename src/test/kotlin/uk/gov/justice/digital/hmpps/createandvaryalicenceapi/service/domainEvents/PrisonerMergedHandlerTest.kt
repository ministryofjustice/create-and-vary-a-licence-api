package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.AuditService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aPrisonApiPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aProbationCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.Companion.IN_FLIGHT_LICENCES
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.createTestMapper

class PrisonerMergedHandlerTest {
  private val auditService = mock<AuditService>()
  private val deliusApiClient = mock<DeliusApiClient>()
  private val mapper: ObjectMapper = createTestMapper()
  private val licenceRepository = mock<LicenceRepository>()
  private val licenceService = mock<LicenceService>()
  private val prisonApiClient = mock<PrisonApiClient>()

  val handler = PrisonerMergedHandler(
    auditService,
    deliusApiClient,
    mapper,
    licenceRepository,
    licenceService,
    prisonApiClient,
  )

  @BeforeEach
  fun reset() {
    reset(
      auditService,
      licenceRepository,
      licenceService,
      prisonApiClient,
    )
  }

  @Test
  fun `should process prisoner merged event and update offender details`() {
    val newNomisId = "A1234AA"
    val oldNomisId = "G5678XT"
    val newCRO = "23456/12A"
    val newPNC = "new PNC"
    val aLicence = createCrdLicence().copy(nomsId = oldNomisId)
    val bookingId = aLicence.bookingId!!.toString()
    val prisoner = aPrisonApiPrisoner()
    val deliusRecord = aProbationCase().copy(croNumber = newCRO, pncNumber = newPNC)

    val expectedChanges: Map<String, Any> = mapOf(
      "type" to "Updating licence details due to prisoner merge event",
      "changes" to mapOf(
        "oldNomisId" to aLicence.nomsId,
        "newNomisId" to newNomisId,
        "oldForename" to aLicence.forename,
        "newForename" to prisoner.firstName,
        "oldMiddleName" to aLicence.middleNames,
        "newMiddleName" to prisoner.middleName,
        "oldSurname" to aLicence.surname,
        "newSurname" to prisoner.lastName,
        "oldPrisonCode" to aLicence.prisonCode,
        "newPrisonCode" to prisoner.agencyId,
        "oldDateOfBirth" to aLicence.dateOfBirth,
        "newDateOfBirth" to prisoner.dateOfBirth,
        "oldCRO" to aLicence.cro,
        "newCRO" to newCRO,
        "oldPNC" to aLicence.pnc,
        "newPNC" to newPNC,

      ),
    )

    whenever(
      licenceRepository.findAllByNomsIdAndStatusCodeIn(
        oldNomisId,
        IN_FLIGHT_LICENCES,
      ),
    ).thenReturn(listOf(aLicence))

    whenever(deliusApiClient.getProbationCase(newNomisId)).thenReturn(deliusRecord)

    whenever(prisonApiClient.getPrisonerDetail(any())).thenReturn(prisoner)

    handler.handleEvent(
      aPrisonerMergedEventMessage(bookingId, newNomisId, oldNomisId),
    )

    verify(prisonApiClient).getPrisonerDetail(newNomisId)
    verify(auditService).recordPrisonerMergedEvent(any(), eq(expectedChanges))
  }

  @Test
  fun `should deactivate any licences on a previous booking`() {
    val newNomisId = "A1234AA"
    val oldNomisId = "G5678XT"
    val aLicence = createCrdLicence().copy(nomsId = oldNomisId)
    val bookingId = (aLicence.bookingId?.plus(1)).toString()
    val prisoner = aPrisonApiPrisoner()

    whenever(
      licenceRepository.findAllByNomsIdAndStatusCodeIn(
        oldNomisId,
        IN_FLIGHT_LICENCES,
      ),
    ).thenReturn(listOf(aLicence))

    whenever(prisonApiClient.getPrisonerDetail(any())).thenReturn(prisoner)

    handler.handleEvent(
      aPrisonerMergedEventMessage(bookingId, newNomisId, oldNomisId),
    )

    verify(prisonApiClient).getPrisonerDetail(newNomisId)
    verify(licenceService).inactivateLicences(
      listOf(aLicence),
      "Deactivating licence on old booking after prisoner merge",
    )
  }

  private fun aPrisonerMergedEventMessage(bookingId: String, newNomisId: String, oldNomisId: String) = mapper
    .writeValueAsString(
      HMPPSPrisonerMergedEvent(
        eventType = PRISON_OFFENDER_MERGED_EVENT_TYPE,
        additionalInformation = AdditionalInformationPrisonerMerged(
          bookingId = bookingId,
          nomsNumber = newNomisId,
          removedNomsNumber = oldNomisId,
          reason = "MERGED",
        ),
        version = 0,
        occurredAt = "2023-12-05T00:00:00Z",
        description = "prisoner merged",
      ),
    )
}
