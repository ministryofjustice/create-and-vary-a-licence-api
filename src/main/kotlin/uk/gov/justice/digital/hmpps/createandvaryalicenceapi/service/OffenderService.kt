package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Case
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateOffenderDetailsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateProbationTeamRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HARD_STOP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.Companion.IN_FLIGHT_LICENCES
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS

@Service
class
OffenderService(
  private val licenceRepository: LicenceRepository,
  private val auditEventRepository: AuditEventRepository,
  private val notifyService: NotifyService,
  private val releaseDateService: ReleaseDateService,
  @Value("\${notify.templates.urgentLicencePrompt}") private val urgentLicencePromptTemplateId: String,
) {

  @Transactional
  fun updateOffenderWithResponsibleCom(crn: String, newCom: CommunityOffenderManager) {
    var offenderLicences = this.licenceRepository.findAllByCrnAndStatusCodeIn(crn, IN_FLIGHT_LICENCES)

    // Update the in-flight licences for this person on probation
    offenderLicences = offenderLicences.map { it.updateResponsibleCom(responsibleCom = newCom) }
    this.licenceRepository.saveAllAndFlush(offenderLicences)

    val inprogressLicence = offenderLicences.find { it.kind != HARD_STOP && it.statusCode == IN_PROGRESS }

    if (inprogressLicence != null) {
      val releaseDate = inprogressLicence.licenceStartDate
      if (releaseDateService.isLateAllocationWarningRequired(releaseDate)) {
        val prisoner = listOf(
          Case(
            "${inprogressLicence.forename} ${inprogressLicence.surname}",
            inprogressLicence.crn!!,
            releaseDate!!,
          ),
        )
        this.notifyService.sendLicenceCreateEmail(
          urgentLicencePromptTemplateId,
          newCom.email!!,
          "${newCom.firstName} ${newCom.lastName}",
          prisoner,
        )
      }
    }

    // Create an audit event for each of the licences updated
    offenderLicences.map {
      auditEventRepository.saveAndFlush(
        AuditEvent(
          licenceId = it.id,
          username = "SYSTEM",
          fullName = "SYSTEM",
          summary = "COM updated to ${newCom.firstName} ${newCom.lastName} on licence for ${it.forename} ${it.surname}",
          detail = "ID ${it.id} type ${it.typeCode} status ${it.statusCode.name} version ${it.version}",
        ),
      )
    }
  }

  @Transactional
  fun updateProbationTeam(crn: String, request: UpdateProbationTeamRequest) {
    var offenderLicences = this.licenceRepository.findAllByCrnAndStatusCodeIn(crn, IN_FLIGHT_LICENCES)

    var probationTeamChanged = false

    // Update the in-flight licences for this person on probation
    offenderLicences = offenderLicences.map {
      if (it.probationTeamCode != request.probationTeamCode) {
        probationTeamChanged = true
      }
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
      // Create an audit event for each of the licences updated
      offenderLicences.map {
        auditEventRepository.saveAndFlush(
          AuditEvent(
            licenceId = it.id,
            username = "SYSTEM",
            fullName = "SYSTEM",
            summary = "Probation team updated to ${request.probationTeamDescription} at ${request.probationAreaDescription} on licence for ${it.forename} ${it.surname}",
            detail = "ID ${it.id} type ${it.typeCode} status ${it.statusCode.name} version ${it.version}",
          ),
        )
      }
    }
  }

  @Transactional
  fun updateOffenderDetails(nomsId: String, request: UpdateOffenderDetailsRequest) {
    val existingLicences = this.licenceRepository.findAllByNomsIdAndStatusCodeIn(nomsId, IN_FLIGHT_LICENCES)
    val licencesToChange = existingLicences.filter { it.isOffenderDetailUpdated(request) }
    if (licencesToChange.isNotEmpty()) {
      val updatedLicences = licencesToChange.map {
        it.updateOffenderDetails(
          forename = request.forename,
          middleNames = request.middleNames,
          surname = request.surname,
          dateOfBirth = request.dateOfBirth,
        )
      }
      this.licenceRepository.saveAllAndFlush(updatedLicences)
      val events = updatedLicences.map {
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
