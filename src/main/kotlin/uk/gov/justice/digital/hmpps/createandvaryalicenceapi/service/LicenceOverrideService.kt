package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDateTime

@Service
class LicenceOverrideService(
  private val licenceRepository: LicenceRepository,
  private val auditEventRepository: AuditEventRepository,
  private val licenceEventRepository: LicenceEventRepository
) {

  /**
   * @return Licence
   * @throws EntityNotFoundException if not licence is found for licenceId
   */
  fun getLicenceById(licenceId: Long): Licence? {
    return licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }
  }

  /**
   * Override licence status
   * @throws ValidationException if new status is already in use by another licence
   */
  fun changeStatus(licenceId: Long, newStatus: LicenceStatus, reason: String) {
    val licence = getLicenceById(licenceId)

    val username = SecurityContextHolder.getContext().authentication.name

    val licences = licenceRepository.findAllByCrnAndStatusCodeIn(licence?.crn!!, listOf(newStatus))

    if (newStatus != LicenceStatus.INACTIVE && licences.any { it.statusCode == newStatus }) {
      throw ValidationException("$newStatus is already in use for this offender on another licence")
    }

    licenceRepository.saveAndFlush(
      licence.copy(
        statusCode = newStatus,
        updatedByUsername = username,
        dateLastUpdated = LocalDateTime.now()
      )
    )

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licence.id,
        detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode.name} version ${licence.version}",
        eventTime = LocalDateTime.now(),
        eventType = AuditEventType.USER_EVENT,
        username = username,
        fullName = username,
        summary = "Licence status overridden to $newStatus for ${licence.forename} ${licence.surname}: $reason"
      )
    )

    licenceEventRepository.saveAndFlush(
      LicenceEvent(
        licenceId = licence.id,
        username = username,
        eventType = LicenceStatus.lookupLicenceEventByStatus(newStatus),
        eventDescription = reason
      )
    )
  }
}

