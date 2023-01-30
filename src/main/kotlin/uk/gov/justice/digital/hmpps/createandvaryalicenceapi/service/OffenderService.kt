package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateProbationTeamRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Service
class
OffenderService(
  private val licenceRepository: LicenceRepository,
  private val auditEventRepository: AuditEventRepository,
) {

  @Transactional
  fun updateOffenderWithResponsibleCom(crn: String, newCom: CommunityOffenderManager) {
    val inFlightLicenceStatuses = listOf(
      LicenceStatus.IN_PROGRESS,
      LicenceStatus.SUBMITTED,
      LicenceStatus.APPROVED,
      LicenceStatus.VARIATION_IN_PROGRESS,
      LicenceStatus.VARIATION_SUBMITTED,
      LicenceStatus.VARIATION_APPROVED,
      LicenceStatus.VARIATION_REJECTED,
      LicenceStatus.ACTIVE
    )
    var offenderLicences = this.licenceRepository.findAllByCrnAndStatusCodeIn(crn, inFlightLicenceStatuses)

    // Update the in-flight licences for this person on probation
    offenderLicences = offenderLicences.map { it.copy(responsibleCom = newCom) }
    this.licenceRepository.saveAllAndFlush(offenderLicences)

    // Create an audit event for each of the licences updated
    offenderLicences.map {
      auditEventRepository.saveAndFlush(
        AuditEvent(
          licenceId = it.id,
          username = "SYSTEM",
          fullName = "SYSTEM",
          summary = "COM updated to ${newCom.firstName} ${newCom.lastName} on licence for ${it.forename} ${it.surname}",
          detail = "ID ${it.id} type ${it.typeCode} status ${it.statusCode.name} version ${it.version}",
        )
      )
    }
  }

  @Transactional
  fun updateProbationTeam(crn: String, request: UpdateProbationTeamRequest) {
    val inFlightLicenceStatuses = listOf(
      LicenceStatus.IN_PROGRESS,
      LicenceStatus.SUBMITTED,
      LicenceStatus.APPROVED,
      LicenceStatus.VARIATION_IN_PROGRESS,
      LicenceStatus.VARIATION_SUBMITTED,
      LicenceStatus.VARIATION_APPROVED,
      LicenceStatus.VARIATION_REJECTED,
      LicenceStatus.ACTIVE
    )
    var offenderLicences = this.licenceRepository.findAllByCrnAndStatusCodeIn(crn, inFlightLicenceStatuses)

    var probationRegionChanged = false

    // Update the in-flight licences for this person on probation
    offenderLicences = offenderLicences.map {
      if (it.probationTeamCode !== request.probationTeamCode) {
        probationRegionChanged = true
      }
      it.copy(
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

    if (probationRegionChanged) {
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
          )
        )
      }
    }
  }
}
