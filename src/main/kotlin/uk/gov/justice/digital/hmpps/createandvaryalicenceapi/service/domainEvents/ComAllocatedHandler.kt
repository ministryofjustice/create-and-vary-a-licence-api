package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

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
  private val staffService: StaffService,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun processComAllocation(crn: String?) {
    if (crn == null) return

    val offenderManager = deliusApiClient.getOffenderManager(crn)
    log.info("responsible offender manager code for crn $crn is ${offenderManager?.code}")

    // If the COM does not have a username, they are assumed to be ineligible for use of this service. (e.g. the "unallocated" staff members)
    if (offenderManager?.username == null) return

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
