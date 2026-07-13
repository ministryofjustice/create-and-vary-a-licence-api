package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import com.fasterxml.jackson.core.JacksonException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.AuditService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceFactory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.Companion.IN_FLIGHT_LICENCES

@Service
class PrisonerMergedHandler(
  private val auditService: AuditService,
  private val deliusApiClient: DeliusApiClient,
  private val mapper: ObjectMapper,
  private val licenceRepository: LicenceRepository,
  private val licenceService: LicenceService,
  private val prisonApiClient: PrisonApiClient,
) : EventHandler {
  override fun handleEvent(message: String) {
    val event = try {
      mapper.readValue(message, HMPPSPrisonerMergedEvent::class.java)
    } catch (e: JacksonException) {
      log.error("Failed to parse recall inserted event message", e)
      throw e
    }

    val bookingId = event.additionalInformation.bookingId.toLong()
    val oldNomisId = event.additionalInformation.removedNomsNumber
    val newNomisId = event.additionalInformation.nomsNumber

    log.info("Processing prisoner merged event received for nomis id: ${event.additionalInformation.nomsNumber}")
    val licencesToUpdate =
      licenceRepository.findAllByNomsIdAndStatusCodeIn(oldNomisId, IN_FLIGHT_LICENCES)

    deactivateLicencesOnOldBooking(licencesToUpdate, bookingId)
    updateOffenderDetails(licencesToUpdate, newNomisId)
  }

  private fun deactivateLicencesOnOldBooking(licences: List<Licence>, newBookingId: Long) {
    val oldBookingLicences = licences.filter {
      it.bookingId != newBookingId
    }

    if (!oldBookingLicences.isEmpty()) {
      licenceService.inactivateLicences(oldBookingLicences, "Deactivating licence on old booking after prisoner merge")
    }
  }

  private fun updateOffenderDetails(licences: List<Licence>, newNomisId: String) {
    val prisonerDetails = prisonApiClient.getPrisonerDetail(newNomisId)
    val deliusRecord = deliusApiClient.getProbationCase(newNomisId)

    licences.forEach { licence ->
      val cro = LicenceFactory.getCro(deliusRecord?.croNumber, prisonerDetails.getCro())

      val changes: Map<String, Any> = mapOf(
        "type" to "Updating licence details due to prisoner merge event",
        "changes" to mapOf(
          "oldNomisId" to licence.nomsId,
          "newNomisId" to newNomisId,
          "oldForename" to licence.forename,
          "newForename" to prisonerDetails.firstName,
          "oldMiddleName" to licence.middleNames,
          "newMiddleName" to prisonerDetails.middleName,
          "oldSurname" to licence.surname,
          "newSurname" to prisonerDetails.lastName,
          "oldPrisonCode" to licence.prisonCode,
          "newPrisonCode" to prisonerDetails.agencyId,
          "oldDateOfBirth" to licence.dateOfBirth,
          "newDateOfBirth" to prisonerDetails.dateOfBirth,
          "oldCRO" to licence.cro,
          "newCRO" to cro,
          "oldPNC" to licence.pnc,
          "newPNC" to deliusRecord?.pncNumber,
        ),
      )
      licence.nomsId = newNomisId
      licence.forename = prisonerDetails.firstName
      licence.middleNames = prisonerDetails.middleName
      licence.surname = prisonerDetails.lastName
      licence.dateOfBirth = prisonerDetails.dateOfBirth
      licence.prisonCode = prisonerDetails.agencyId
      licence.cro = cro
      licence.pnc = deliusRecord?.pncNumber

      auditService.recordPrisonerMergedEvent(licence, changes)

      licenceRepository.save(licence)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(PrisonerMergedHandler::class.java)
  }
}
