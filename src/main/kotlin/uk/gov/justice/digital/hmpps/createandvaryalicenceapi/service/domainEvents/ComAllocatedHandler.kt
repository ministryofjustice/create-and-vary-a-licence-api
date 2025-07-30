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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.WorkLoadApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.mapper.OffenderManagerMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.OffenderManager

@Service
class ComAllocatedHandler(
  private val deliusApiClient: DeliusApiClient,
  private val workLoadApiClient: WorkLoadApiClient,
  private val offenderService: OffenderService,
  private val objectMapper: ObjectMapper,
  private val staffService: StaffService,
  private val offenderManagerMapper: OffenderManagerMapper,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun handleEvent(message: String) {
    log.info("Received COM allocation event")

    val event = try {
      objectMapper.readValue(message, HMPPSDomainEvent::class.java)
    } catch (e: Exception) {
      log.error("Failed to parse event message", e)
      return
    }

    val crn = event.personReference.crn()

    val offenderManager = getOffenderManager(crn, event)
    if (isValidOffenderManager(offenderManager, crn)) {
      processComAllocation(offenderManager!!)
    }
  }

  fun processComAllocation(offenderManager: OffenderManager) {
    log.info("Processing COM allocation for CRN ${offenderManager.crn} code is ${offenderManager.code}")

    deliusApiClient.assignDeliusRole(offenderManager.username!!)

    val newCom = staffService.updateComDetails(
      UpdateComRequest(
        staffIdentifier = offenderManager.staffIdentifier,
        staffUsername = offenderManager.username,
        staffEmail = offenderManager.email,
        firstName = offenderManager.forename,
        lastName = offenderManager.surname,
      ),
    )

    offenderService.updateOffenderWithResponsibleCom(offenderManager.crn, newCom)

    offenderService.updateProbationTeam(
      offenderManager.crn,
      UpdateProbationTeamRequest(
        probationAreaCode = offenderManager.providerCode,
        probationAreaDescription = offenderManager.providerDescription,
        probationPduCode = offenderManager.boroughCode,
        probationPduDescription = offenderManager.boroughDescription,
        probationLauCode = offenderManager.districtCode,
        probationLauDescription = offenderManager.districtDescription,
        probationTeamCode = offenderManager.teamCode,
        probationTeamDescription = offenderManager.teamDescription,
      ),
    )
    log.info("Successfully completed COM allocation for CRN ${offenderManager.crn}")
  }

  fun isValidOffenderManager(offenderManager: OffenderManager?, crn: String): Boolean {
    if (offenderManager == null) {
      log.warn("No offender manager found for CRN $crn")
      return false
    }
    if (offenderManager.username.isNullOrBlank()) {
      log.warn("Offender manager with code ${offenderManager.code} for CRN $crn has no username — skipping allocation")
      return false
    }
    return true
  }

  private fun getOffenderManager(crn: String, event: HMPPSDomainEvent): OffenderManager? {
    val isDeliusEvent = event.detailUrl.isNullOrBlank()
    return if (isDeliusEvent) {
      getOffenderManagerForDeliusEvent(crn)
    } else {
      getOffenderManagerForApop(event.detailUrl!!)
    }
  }

  fun getOffenderManagerForDeliusEvent(crn: String): OffenderManager? {
    log.info("Getting offender manager from Delius for CRN: {}", crn)

    val response = deliusApiClient.getOffenderManager(crn)
    return if (response != null) {
      offenderManagerMapper.mapFrom(response)
    } else {
      log.warn("No Delius data found for CRN: {}", crn)
      null
    }
  }

  private fun getOffenderManagerForApop(detailUrl: String): OffenderManager {
    val personUuid = getPersonUuid(detailUrl)
    log.info("Getting offender manager from APOP for personUuid: {}", personUuid)

    val workLoadAllocationResponse = workLoadApiClient.getStaffDetails(personUuid)!!
    val staffDetails = deliusApiClient.getStaffByCode(workLoadAllocationResponse.staffCode)!!

    log.debug("Staff details found for staffCode: {}", workLoadAllocationResponse.staffCode)
    return offenderManagerMapper.mapFrom(staffDetails, workLoadAllocationResponse)
  }

  fun getPersonUuid(detailUrl: String): String = detailUrl.substringAfterLast("/")
}
