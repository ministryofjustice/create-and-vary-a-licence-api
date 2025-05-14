package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.support

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.OverrideLicenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.OverrideLicencePrisonerDetailsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDateTime

@Service
class LicenceOverrideService(
  private val licenceRepository: LicenceRepository,
  private val auditEventRepository: AuditEventRepository,
  private val licenceEventRepository: LicenceEventRepository,
  private val domainEventsService: DomainEventsService,
  private val staffRepository: StaffRepository,
  private val licenceService: LicenceService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Override licence status
   * @throws jakarta.validation.ValidationException if new status is already in use by another licence
   */
  @Transactional
  fun changeStatus(licenceId: Long, newStatus: LicenceStatus, reason: String) {
    val licence = licenceRepository.findById(licenceId).orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name

    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    val licences = licenceRepository.findAllByCrnAndStatusCodeIn(licence?.crn!!, listOf(newStatus))

    var licenceActivatedDate: LocalDateTime? = null

    if (newStatus != LicenceStatus.INACTIVE && licences.any { it.statusCode == newStatus }) {
      throw ValidationException("$newStatus is already in use for this offender on another licence")
    }

    if (newStatus == LicenceStatus.ACTIVE) {
      licenceActivatedDate = LocalDateTime.now()
      licenceService.inactivateTimedOutLicenceVersions(
        listOf(licence),
        "Deactivating timed out licence as the licence status was overridden to active",
      )
    }

    licenceRepository.saveAndFlush(
      licence.overrideStatus(
        statusCode = newStatus,
        staffMember = staffMember,
        licenceActivatedDate = licenceActivatedDate,
      ),
    )

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licence.id,
        detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode.name} version ${licence.version}",
        eventTime = LocalDateTime.now(),
        eventType = AuditEventType.USER_EVENT,
        username = staffMember?.username ?: Licence.Companion.SYSTEM_USER,
        fullName = staffMember?.fullName ?: Licence.Companion.SYSTEM_USER,
        summary = "Licence status overridden to $newStatus for ${licence.forename} ${licence.surname}: $reason",
      ),
    )

    licenceEventRepository.saveAndFlush(
      LicenceEvent(
        licenceId = licence.id,
        username = username,
        eventType = LicenceStatus.Companion.lookupLicenceEventByStatus(newStatus),
        eventDescription = reason,
      ),
    )

    domainEventsService.recordDomainEvent(licence, newStatus)
  }

  @Transactional
  fun changeDates(licenceId: Long, request: OverrideLicenceDatesRequest) {
    val licence = licenceRepository.findById(licenceId).orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name

    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    log.info(
      buildString {
        append("Current licence dates - ID $licenceId ")
        append("CRD ${licence.conditionalReleaseDate} ")
        append("ARD ${licence.actualReleaseDate} ")
        append("SSD ${licence.sentenceStartDate} ")
        append("SED ${licence.sentenceEndDate} ")
        append("LSD ${licence.licenceStartDate} ")
        append("LED ${licence.licenceExpiryDate} ")
        append("TUSSD ${licence.topupSupervisionStartDate} ")
        append("TUSED ${licence.topupSupervisionExpiryDate}")
        append("PRRD ${licence.postRecallReleaseDate}")
        append("PRRD ${licence.postRecallReleaseDate}")
        if (licence is HdcLicence) {
          append("HDCAD ${licence.homeDetentionCurfewActualDate}")
          append("HDCEndDate ${licence.homeDetentionCurfewEndDate}")
        }
      },
    )

    log.info(
      buildString {
        append("Updated dates - ID $licenceId ")
        append("CRD ${request.conditionalReleaseDate} ")
        append("ARD ${request.actualReleaseDate} ")
        append("SSD ${request.sentenceStartDate} ")
        append("SED ${request.sentenceEndDate} ")
        append("LSD ${request.licenceStartDate} ")
        append("LED ${request.licenceExpiryDate} ")
        append("TUSSD ${request.topupSupervisionStartDate} ")
        append("TUSED ${request.topupSupervisionExpiryDate}")
        append("PRRD ${request.postRecallReleaseDate}")
        if (licence is HdcLicence) {
          append("HDCAD ${request.homeDetentionCurfewActualDate}")
          append("HDCEndDate ${request.homeDetentionCurfewEndDate}")
        }
      },
    )

    val updatedLicenceEntity = licence.updateLicenceDates(
      conditionalReleaseDate = request.conditionalReleaseDate,
      actualReleaseDate = request.actualReleaseDate,
      sentenceStartDate = request.sentenceStartDate,
      sentenceEndDate = request.sentenceEndDate,
      licenceStartDate = request.licenceStartDate,
      licenceExpiryDate = request.licenceExpiryDate,
      topupSupervisionStartDate = request.topupSupervisionStartDate,
      topupSupervisionExpiryDate = request.topupSupervisionExpiryDate,
      postRecallReleaseDate = request.postRecallReleaseDate,
      homeDetentionCurfewActualDate = request.homeDetentionCurfewActualDate,
      homeDetentionCurfewEndDate = request.homeDetentionCurfewEndDate,

      staffMember = staffMember,
    )

    licenceRepository.saveAndFlush(updatedLicenceEntity)

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licence.id,
        detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode} version ${licence.version}",
        eventTime = LocalDateTime.now(),
        eventType = AuditEventType.USER_EVENT,
        username = staffMember?.username ?: Licence.Companion.SYSTEM_USER,
        fullName = staffMember?.fullName ?: Licence.Companion.SYSTEM_USER,
        summary = "Sentence dates overridden for ${licence.forename} ${licence.surname}: ${request.reason}",
      ),
    )
  }

  @Transactional
  fun changePrisonerDetails(licenceId: Long, request: OverrideLicencePrisonerDetailsRequest) {
    val licence: Licence = licenceRepository.findById(licenceId).orElseThrow { EntityNotFoundException("$licenceId") }
    val username = SecurityContextHolder.getContext().authentication.name

    licence.updatePrisonerDetails(
      forename = request.forename,
      middleNames = request.middleNames,
      surname = request.surname,
      dateOfBirth = request.dateOfBirth,
    )

    licenceRepository.saveAndFlush(licence)

    val changes: Map<String, String> = mapOf(
      "forename" to request.forename,
      "middleNames" to request.middleNames.orEmpty(),
      "surname" to request.surname,
      "dateOfBirth" to request.dateOfBirth.toString(),
      "reason" to request.reason,
    )

    val staffMember = staffRepository.findByUsernameIgnoreCase(username)
    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licence.id,
        detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode} version ${licence.version}",
        eventTime = LocalDateTime.now(),
        eventType = AuditEventType.USER_EVENT,
        username = staffMember?.username ?: Licence.SYSTEM_USER,
        fullName = staffMember?.fullName ?: Licence.SYSTEM_USER,
        summary = "Prisoner details overridden for licence with ID ${licence.id}: ${request.reason}",
        changes = changes,
      ),
    )
  }
}
