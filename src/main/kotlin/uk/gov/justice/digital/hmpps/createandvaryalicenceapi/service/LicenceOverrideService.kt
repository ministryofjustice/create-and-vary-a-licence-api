package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.OverrideLicenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
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
) {

  companion object {
    private val log = LoggerFactory.getLogger(LicenceOverrideService::class.java)
  }

  /**
   * @return Licence
   * @throws EntityNotFoundException if not licence is found for licenceId
   */
  @Transactional
  fun getLicenceById(licenceId: Long): Licence? {
    return licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }
  }

  /**
   * Override licence status
   * @throws ValidationException if new status is already in use by another licence
   */
  @Transactional
  fun changeStatus(licenceId: Long, newStatus: LicenceStatus, reason: String) {
    val licence = getLicenceById(licenceId)

    val username = SecurityContextHolder.getContext().authentication.name

    val licences = licenceRepository.findAllByCrnAndStatusCodeIn(licence?.crn!!, listOf(newStatus))

    var licenceActivatedDate: LocalDateTime? = null

    if (newStatus != LicenceStatus.INACTIVE && licences.any { it.statusCode == newStatus }) {
      throw ValidationException("$newStatus is already in use for this offender on another licence")
    }

    if (newStatus == LicenceStatus.ACTIVE) {
      licenceActivatedDate = LocalDateTime.now()
    }

    licenceRepository.saveAndFlush(
      licence.overrideStatus(
        statusCode = newStatus,
        updatedByUsername = username,
        licenceActivatedDate = licenceActivatedDate,
      ),
    )

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licence.id,
        detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode.name} version ${licence.version}",
        eventTime = LocalDateTime.now(),
        eventType = AuditEventType.USER_EVENT,
        username = username,
        fullName = username,
        summary = "Licence status overridden to $newStatus for ${licence.forename} ${licence.surname}: $reason",
      ),
    )

    licenceEventRepository.saveAndFlush(
      LicenceEvent(
        licenceId = licence.id,
        username = username,
        eventType = LicenceStatus.lookupLicenceEventByStatus(newStatus),
        eventDescription = reason,
      ),
    )

    domainEventsService.recordDomainEvent(licence, newStatus)
  }

  @Transactional
  fun changeDates(licenceId: Long, request: OverrideLicenceDatesRequest) {
    val licence = licenceRepository.findById(licenceId).orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name

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
      updatedByUsername = username,
    )

    licenceRepository.saveAndFlush(updatedLicenceEntity)

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licence.id,
        detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode} version ${licence.version}",
        eventTime = LocalDateTime.now(),
        eventType = AuditEventType.USER_EVENT,
        username = username,
        fullName = username,
        summary = "Sentence dates overridden for ${licence.forename} ${licence.surname}: ${request.reason}",
      ),
    )
  }
}
