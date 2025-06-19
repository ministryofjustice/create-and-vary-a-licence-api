package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateProbationTeamRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.OffenderService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.StaffService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient

@Service
class ComAllocatedHandler(
  private val deliusApiClient: DeliusApiClient,
  private val offenderService: OffenderService,
  private val objectMapper: ObjectMapper,
  private val staffService: StaffService,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun handleEvent(message: String) {
    val event = objectMapper.readValue(message, HMPPSDomainEvent::class.java)
    val crn = event.personReference.crn()
    if (crn != null) {
      processComAllocation(crn)
    }
  }

  fun processComAllocation(crn: String) {
    log.info("processing COM allocation for CRN $crn")
    val offenderManager = deliusApiClient.getOffenderManager(crn)
    if (offenderManager == null) {
      log.warn("Could not find offender manager for CRN $crn")
      return
    }

    // If the COM does not have a username, they are assumed to be ineligible for use of this service. (e.g. the "unallocated" staff members)
    log.info("responsible offender manager code for crn $crn is ${offenderManager.code}")
    if (offenderManager.username == null) {
      log.warn("offender manager with code ${offenderManager.code} does not have a username")
      return
    }

    // Assign the com role to the user if they do not have it already
    deliusApiClient.assignDeliusRole(offenderManager.username.trim().uppercase())

    val newCom = staffService.updateComDetails(
      UpdateComRequest(
        staffIdentifier = offenderManager.id,
        staffUsername = offenderManager.username,
        staffEmail = offenderManager.email,
        firstName = offenderManager.name.forename,
        lastName = offenderManager.name.forename,
      ),
    )

    offenderService.updateOffenderWithResponsibleCom(crn, newCom)

    offenderService.updateProbationTeam(
      crn,
      UpdateProbationTeamRequest(
        probationAreaCode = offenderManager.provider.code,
        probationAreaDescription = offenderManager.provider.description,
        probationPduCode = offenderManager.team.borough.code,
        probationPduDescription = offenderManager.team.borough.description,
        probationLauCode = offenderManager.team.district.code,
        probationLauDescription = offenderManager.team.district.description,
        probationTeamCode = offenderManager.team.code,
        probationTeamDescription = offenderManager.team.description,
      ),
    )
  }
}
