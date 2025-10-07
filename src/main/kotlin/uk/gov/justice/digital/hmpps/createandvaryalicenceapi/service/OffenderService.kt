package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Case
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateOffenderDetailsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateProbationTeamRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HARD_STOP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.Companion.IN_FLIGHT_LICENCES
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.TimeServedConsiderations

@Service
class
OffenderService(
  private val licenceRepository: LicenceRepository,
  private val auditEventRepository: AuditEventRepository,
  private val auditService: AuditService,
  private val notifyService: NotifyService,
  private val releaseDateService: ReleaseDateService,
  private val staffRepository: StaffRepository,
  @param:Value("\${notify.templates.urgentLicencePrompt}") private val urgentLicencePromptTemplateId: String,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @TimeServedConsiderations("When updating responsible COM for a CRN, should anything additional happen if the COM was null to begin with, is it licence dependent?")
  @Transactional
  fun updateOffenderWithResponsibleCom(
    crn: String,
    existingCom: CommunityOffenderManager?,
    newCom: CommunityOffenderManager,
  ) {
    log.info(
      "Updating responsible COM for CRN={} to {} {} (email={})",
      crn,
      newCom.username,
      newCom.staffIdentifier,
      newCom.email,
    )

    val offenderLicences = licenceRepository.findAllByCrnAndStatusCodeIn(crn, IN_FLIGHT_LICENCES)

    offenderLicences.forEach { it.responsibleCom = newCom }
    licenceRepository.saveAllAndFlush(offenderLicences)

    val inProgressLicence = offenderLicences.find { it.kind != HARD_STOP && it.statusCode == IN_PROGRESS }

    if (inProgressLicence != null) {
      log.info(
        "Found in-progress licence (id={}) for CRN={} - checking for late allocation warning.",
        inProgressLicence.id,
        crn,
      )

      val releaseDate = inProgressLicence.licenceStartDate
      if (releaseDateService.isLateAllocationWarningRequired(releaseDate)) {
        log.warn(
          "Late allocation warning triggered for CRN={} licenceId={} with releaseDate={}",
          crn,
          inProgressLicence.id,
          releaseDate,
        )

        val prisoner = listOf(
          Case(
            "${inProgressLicence.forename} ${inProgressLicence.surname}",
            inProgressLicence.crn!!,
            releaseDate!!,
          ),
        )

        notifyService.sendLicenceCreateEmail(
          urgentLicencePromptTemplateId,
          newCom.email!!,
          "${newCom.firstName} ${newCom.lastName}",
          prisoner,
        )

        log.info(
          "Late allocation email sent to {} for CRN={} and licenceId={}",
          newCom.email,
          crn,
          inProgressLicence.id,
        )
      }
    } else {
      log.info("No in-progress licence found for CRN={}", crn)
    }

    val username = SecurityContextHolder.getContext().authentication?.name ?: SYSTEM_USER
    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    offenderLicences.forEach {
      auditService.recordAuditEventComUpdated(it, existingCom, newCom, staffMember)
    }

    log.info("Completed update of responsible COM for CRN={}", crn)
  }

  @Transactional
  fun updateProbationTeam(crn: String, request: UpdateProbationTeamRequest) {
    log.debug("Request : {}", request)
    val offenderLicences = this.licenceRepository.findAllByCrnAndStatusCodeIn(crn, IN_FLIGHT_LICENCES)

    var probationTeamChanged = false

    // Update the in-flight licences for this person on probation
    offenderLicences.forEach {
      if (it.probationTeamCode != request.probationTeamCode) {
        probationTeamChanged = true
      }
      val username = SecurityContextHolder.getContext().authentication?.name ?: SYSTEM_USER
      val staffMember = staffRepository.findByUsernameIgnoreCase(username)
      auditService.recordAuditEventProbationTeamUpdated(it, request, staffMember)

      it.updateProbationTeam(
        probationAreaCode = request.probationAreaCode,
        probationAreaDescription = request.probationAreaDescription,
        probationPduCode = request.probationPduCode,
        probationPduDescription = request.probationPduDescription,
        probationLauCode = request.probationLauCode,
        probationLauDescription = request.probationLauDescription,
        probationTeamCode = request.probationTeamCode,
        probationTeamDescription = request.probationTeamDescription,
      )
    }

    if (probationTeamChanged) {
      this.licenceRepository.saveAllAndFlush(offenderLicences)
    }
  }

  @Transactional
  fun updateOffenderDetails(nomsId: String, request: UpdateOffenderDetailsRequest) {
    val existingLicences = this.licenceRepository.findAllByNomsIdAndStatusCodeIn(nomsId, IN_FLIGHT_LICENCES)
    val licencesToChange = existingLicences.filter { it.isOffenderDetailUpdated(request) }
    if (licencesToChange.isNotEmpty()) {
      licencesToChange.forEach {
        it.updateOffenderDetails(
          forename = request.forename,
          middleNames = request.middleNames,
          surname = request.surname,
          dateOfBirth = request.dateOfBirth,
        )
      }
      this.licenceRepository.saveAllAndFlush(licencesToChange)
      val events = licencesToChange.map {
        AuditEvent(
          licenceId = it.id,
          username = "SYSTEM",
          fullName = "SYSTEM",
          summary = "Offender details updated to forename: ${request.forename}, middleNames: ${request.middleNames}, surname: ${request.surname}, date of birth: ${request.dateOfBirth}",
          detail = "ID ${it.id} type ${it.typeCode} status ${it.statusCode.name} version ${it.version}",
        )
      }
      auditEventRepository.saveAllAndFlush(events)
    }
  }

  private fun Licence.isOffenderDetailUpdated(request: UpdateOffenderDetailsRequest): Boolean = (
    this.forename != request.forename ||
      this.middleNames != request.middleNames ||
      this.surname != request.surname ||
      this.dateOfBirth != request.dateOfBirth
    )
}
