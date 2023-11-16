package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonerForRelease
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateOffenderDetailsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateProbationTeamRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate

@Service
class
OffenderService(
  private val licenceRepository: LicenceRepository,
  private val auditEventRepository: AuditEventRepository,
  private val notifyService: NotifyService,
  private val bankHolidayService: BankHolidayService,
  private val clock: Clock,
  @Value("\${notify.templates.urgentLicencePrompt}") private val urgentLicencePromptTemplateId: String,
  @Value("\${maxNumberOfWorkingDaysToTriggerAllocationWarningEmail}") private val maxNumberOfWorkingDaysToTriggerAllocationWarningEmail: Int,
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
      LicenceStatus.ACTIVE,
    )
    var offenderLicences = this.licenceRepository.findAllByCrnAndStatusCodeIn(crn, inFlightLicenceStatuses)

    // Update the in-flight licences for this person on probation
    offenderLicences = offenderLicences.map { it.copy(responsibleCom = newCom) }
    this.licenceRepository.saveAllAndFlush(offenderLicences)

    if (offenderLicences.any {
      it.statusCode === LicenceStatus.NOT_STARTED || it.statusCode === LicenceStatus.IN_PROGRESS
    }
    ) {
      val releaseDate = offenderLicences[0].actualReleaseDate ?: offenderLicences[0].conditionalReleaseDate
      if (isXWorkingDaysBefore(maxNumberOfWorkingDaysToTriggerAllocationWarningEmail, releaseDate)) {
        val prisoner = listOf(
          PrisonerForRelease(
            "${offenderLicences[0].forename} ${offenderLicences[0].surname}",
            offenderLicences[0].crn!!,
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

  fun isXWorkingDaysBefore(days: Int, releaseDate: LocalDate?): Boolean {
    if (releaseDate === null) return false
    val dateBeforeXWorkingDays = getEarliestReleaseDate(days, releaseDate)
    return LocalDate.now(clock).isEqual(dateBeforeXWorkingDays) || LocalDate.now(clock).isAfter(dateBeforeXWorkingDays)
  }

  private fun isEligibleForEarlyRelease(releaseDate: LocalDate?): Boolean {
    val dayOfWeek = releaseDate?.dayOfWeek
    if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
      return true
    }
    val listOfBankHolidays: List<LocalDate> = bankHolidayService.getBankHolidaysForEnglandAndWales()
    return listOfBankHolidays.contains(releaseDate)
  }

  private fun getEarliestReleaseDate(days: Int, releaseDate: LocalDate) =
    generateSequence(releaseDate) { it.minusDays(1) }
      .filterNot { isEligibleForEarlyRelease(it) }
      .take(days)
      .last()

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
      LicenceStatus.ACTIVE,
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
          ),
        )
      }
    }
  }

  @Transactional
  fun updateOffenderDetails(nomsId: String, request: UpdateOffenderDetailsRequest) {
    val inFlightLicenceStatuses = listOf(
      LicenceStatus.IN_PROGRESS,
      LicenceStatus.SUBMITTED,
      LicenceStatus.APPROVED,
      LicenceStatus.VARIATION_IN_PROGRESS,
      LicenceStatus.VARIATION_SUBMITTED,
      LicenceStatus.VARIATION_APPROVED,
      LicenceStatus.VARIATION_REJECTED,
      LicenceStatus.ACTIVE,
    )
    val existingLicences = this.licenceRepository.findAllByNomsIdAndStatusCodeIn(nomsId, inFlightLicenceStatuses)
    val licencesToChange = existingLicences.filter {
      it.isOffenderDetailUpdated(request)
    }
    if (licencesToChange.isNotEmpty()) {
      val updatedLicences = licencesToChange.map {
        it.copy(
          forename = request.forename,
          middleNames = request.middleNames,
          surname = request.surname,
          dateOfBirth = request.dateOfBirth,
        )
      }
      this.licenceRepository.saveAllAndFlush(updatedLicences)
      updatedLicences.map {
        auditEventRepository.saveAndFlush(
          AuditEvent(
            licenceId = it.id,
            username = "SYSTEM",
            fullName = "SYSTEM",
            summary = "Offender details updated to forename: ${request.forename}, middleNames: ${request.middleNames}, surname: ${request.surname}, date of birth: ${request.dateOfBirth}",
            detail = "ID ${it.id} type ${it.typeCode} status ${it.statusCode.name} version ${it.version}",
          ),
        )
      }
    }
  }

  private fun Licence.isOffenderDetailUpdated(request: UpdateOffenderDetailsRequest): Boolean {
    return (
      this.forename !== request.forename ||
        this.middleNames !== request.middleNames ||
        this.surname !== request.surname ||
        this.dateOfBirth !== request.dateOfBirth
      )
  }
}
