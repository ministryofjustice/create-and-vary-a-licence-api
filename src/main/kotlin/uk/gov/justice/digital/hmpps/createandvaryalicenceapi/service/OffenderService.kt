package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.VariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Case
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateOffenderDetailsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.events.UpdateProbationTeamEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.TIME_SERVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.VARIATION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.Companion.IN_FLIGHT_LICENCES
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import java.time.LocalDateTime

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

  @Transactional
  fun updateResponsibleCom(crn: String, newCom: CommunityOffenderManager) {
    log.info(
      "Updating responsible COM for CRN={} to {} {} (email={})",
      crn,
      newCom.username,
      newCom.staffIdentifier,
      newCom.email,
    )

    val offenderLicences = licenceRepository.findAllByCrnAndStatusCodeIn(crn, IN_FLIGHT_LICENCES)

    val previousCom = offenderLicences.firstOrNull()?.responsibleCom

    if (previousCom?.id == newCom.id) {
      log.info("Same staff record so no update required for CRN={}, staffCode: {}", crn, newCom.staffCode)
      return
    }

    offenderLicences.forEach {
      log.info("Updating COM assignment for licence: {}, staffCode: {}", it.id, newCom.staffCode)
      it.responsibleCom = newCom
      it.dateLastUpdated = LocalDateTime.now()
    }

    licenceRepository.saveAllAndFlush(offenderLicences)

    notifyComIfLateAllocation(offenderLicences, crn)

    notifyComIfFirstAllocation(offenderLicences, crn, previousCom)

    val username = SecurityContextHolder.getContext().authentication?.name ?: SYSTEM_USER
    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    offenderLicences.forEach {
      auditService.recordAuditEventComUpdated(it, previousCom, newCom, staffMember)
    }

    log.info("Completed update of responsible COM for CRN={}", crn)
  }

  private fun notifyComIfLateAllocation(licences: List<Licence>, crn: String) {
    val inProgressLicence = licences.find { !it.kind.isCreatedByPrison() && it.statusCode == IN_PROGRESS }

    if (inProgressLicence == null || inProgressLicence.responsibleCom == null) {
      log.info("No relevant in-progress licence found for CRN={}", crn)
      return
    }

    val newCom = inProgressLicence.responsibleCom!!
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

      notifyService.sendLicenceCreateEmail(
        urgentLicencePromptTemplateId,
        newCom.email!!,
        "${newCom.firstName} ${newCom.lastName}",
        listOf(
          Case(
            "${inProgressLicence.forename} ${inProgressLicence.surname}",
            inProgressLicence.crn!!,
            releaseDate!!,
            kind = inProgressLicence.kind,
          ),
        ),
      )

      log.info("Late allocation email sent to {} for CRN={} and licenceId={}", newCom.email, crn, inProgressLicence.id)
    }
  }

  private fun notifyComIfFirstAllocation(licences: List<Licence>, crn: String, previousCom: CommunityOffenderManager?) {
    val licence = licences.find { it.kind == TIME_SERVED || it.kind == VARIATION }

    if (licence == null || licence.responsibleCom == null) {
      log.info("No time served or variation licences without a COM found for CRN={}", crn)
      return
    }

    log.info(
      "Found {} licence (id={}) for CRN={} - checking if a PP was previously allocated",
      licence.kind,
      licence.id,
      crn,
    )
    val newCom = licence.responsibleCom!!

    // Only send if there was no COM allocated previously, and now there is one
    if (previousCom == null) {
      log.info("No previous PP allocated for CRN={}", crn)

      val shouldSendEmail = when {
        licence.kind == TIME_SERVED -> true
        licence.kind == VARIATION -> {
          val variationLicence = licence as VariationLicence
          val originalLicence = findOriginalLicenceForVariation(variationLicence)
          originalLicence.kind == TIME_SERVED
        }

        else -> false
      }

      if (shouldSendEmail) {
        notifyService.sendInitialComAllocationEmail(
          newCom.email!!,
          "${newCom.firstName} ${newCom.lastName}",
          "${licence.forename} ${licence.surname}",
          licence.crn!!,
          licence.id.toString(),
        )
      }
    }
  }

  @Transactional
  fun updateProbationTeam(crn: String, event: UpdateProbationTeamEvent) {
    log.debug("Request : {}", event)
    val offenderLicences = this.licenceRepository.findAllByCrnAndStatusCodeIn(crn, IN_FLIGHT_LICENCES)

    var probationTeamChanged = false

    // Update the in-flight licences for this person on probation
    offenderLicences.forEach {
      if (it.probationTeamCode != event.probationTeamCode) {
        probationTeamChanged = true
      }
      val username = SecurityContextHolder.getContext().authentication?.name ?: SYSTEM_USER
      val staffMember = staffRepository.findByUsernameIgnoreCase(username)
      auditService.recordAuditEventProbationTeamUpdated(it, event, staffMember)

      it.updateProbationTeam(
        probationAreaCode = event.probationAreaCode,
        probationAreaDescription = event.probationAreaDescription,
        probationPduCode = event.probationPduCode,
        probationPduDescription = event.probationPduDescription,
        probationLauCode = event.probationLauCode,
        probationLauDescription = event.probationLauDescription,
        probationTeamCode = event.probationTeamCode,
        probationTeamDescription = event.probationTeamDescription,
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
      val events = licencesToChange.map {
        val changes = mapOf(
          "type" to "Updated offender details",
          "changes" to mapOf(
            "oldForename" to (it.forename ?: ""),
            "newForename" to request.forename,
            "oldMiddleNames" to (it.middleNames ?: ""),
            "newMiddleNames" to request.middleNames,
            "oldSurname" to (it.surname ?: ""),
            "newSurname" to request.surname,
            "oldDob" to (it.dateOfBirth ?: ""),
            "newDob" to request.dateOfBirth,
          ),
        )

        AuditEvent(
          licenceId = it.id,
          username = "SYSTEM",
          fullName = "SYSTEM",
          summary = "Offender details updated to forename: ${request.forename}, middleNames: ${request.middleNames}, surname: ${request.surname}, date of birth: ${request.dateOfBirth}",
          detail = "ID ${it.id} type ${it.typeCode} status ${it.statusCode.name} version ${it.version}",
          changes = changes,
        )
      }

      licencesToChange.forEach {
        it.updateOffenderDetails(
          forename = request.forename,
          middleNames = request.middleNames,
          surname = request.surname,
          dateOfBirth = request.dateOfBirth,
        )
      }

      licenceRepository.saveAllAndFlush(licencesToChange)
      auditEventRepository.saveAllAndFlush(events)
    }
  }

  private fun Licence.isOffenderDetailUpdated(request: UpdateOffenderDetailsRequest): Boolean = (
    this.forename != request.forename ||
      this.middleNames != request.middleNames ||
      this.surname != request.surname ||
      this.dateOfBirth != request.dateOfBirth
    )

  private fun findOriginalLicenceForVariation(licence: VariationLicence): Licence {
    var currentLicence = licence

    while (currentLicence.variationOfId != null) {
      val parentLicence = licenceRepository.findById(currentLicence.variationOfId!!)
        .orElseThrow { EntityNotFoundException("$currentLicence.variationOfId") }

      when {
        parentLicence.kind == VARIATION -> currentLicence = parentLicence as VariationLicence
        else -> return parentLicence
      }
    }
    error("Original licence not found for licenceId=${licence.id}")
  }
}
