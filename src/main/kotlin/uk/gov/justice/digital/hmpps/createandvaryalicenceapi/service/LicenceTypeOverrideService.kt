package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceTypeOverrideService.ErrorType.IS_IN_PAST
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceTypeOverrideService.ErrorType.IS_MISSING
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceTypeOverrideService.ErrorType.IS_PRESENT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.LicencePolicyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.DetailedValidationException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP_PSS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.PSS
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class LicenceTypeOverrideService(
  private val licenceRepository: LicenceRepository,
  private val auditEventRepository: AuditEventRepository,
  private val staffRepository: StaffRepository,
  private val licencePolicyService: LicencePolicyService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun changeType(licenceId: Long, newType: LicenceType, reason: String) {
    val licence = licenceRepository.findById(licenceId).orElseThrow { EntityNotFoundException("$licenceId") }
    val previousType = licence.typeCode

    if (previousType == newType) {
      log.info("Licence $licenceId already is of type: $newType")
      return
    }
    log.info("Converting Licence $licenceId from $previousType to $newType")

    getIncorrectDates(newType, licence).takeIf { it.isNotEmpty() }?.let { errors ->
      throw DetailedValidationException(
        title = "Incorrect dates for new licence type: $newType",
        errors = mapOf("fieldErrors" to errors.associate { it.dateName to it.errorType }),
      )
    }

    val username = SecurityContextHolder.getContext().authentication.name
    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    licence.typeCode = newType

    val (relevantConditions, conditionsToRemove) = licence.getAdditionalConditionsByRelevancy()
    val standardConditions = licencePolicyService.getStandardConditionsForLicence(licence)

    val licenceToPersist = licence.updateConditions(
      updatedAdditionalConditions = relevantConditions,
      updatedStandardConditions = standardConditions,
      staffMember = staffMember,
    )

    licenceRepository.saveAndFlush(licenceToPersist)

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licence.id,
        detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode} version ${licence.version}",
        eventTime = LocalDateTime.now(),
        eventType = AuditEventType.USER_EVENT,
        username = staffMember?.username ?: SYSTEM_USER,
        fullName = staffMember?.fullName ?: SYSTEM_USER,
        summary = "Licence type overridden for ${licence.forename} ${licence.surname}: from $previousType to $newType: $reason",
        changes = mutableMapOf(
          "oldType" to previousType.name,
          "newType" to newType.name,
          "deletedAdditionalConditions" to conditionsToRemove.map {
            mapOf(
              "code" to it.conditionCode,
              "version" to it.conditionVersion,
              "category" to it.conditionCategory,
              "text" to it.expandedConditionText,
            )
          },
        ),
      ),
    )
  }

  private fun Licence.getAdditionalConditionsByRelevancy() =
    this.additionalConditions.partition { this.typeCode.conditionTypes().contains(it.conditionType) }

  private fun getIncorrectDates(licenceType: LicenceType, licence: Licence): Set<IncorrectDate> {
    val led = licence.licenceExpiryDate
    val tused = licence.topupSupervisionExpiryDate

    val errors = mutableSetOf<IncorrectDate>()
    when (licenceType) {
      AP -> {
        if (led == null) errors.add(IncorrectDate("LED", IS_MISSING))
        if (tused != null) errors.add(IncorrectDate("TUSED", IS_PRESENT))
      }

      PSS -> {
        if (led != null) errors.add(IncorrectDate("LED", IS_PRESENT))
        if (tused == null) errors.add(IncorrectDate("TUSED", IS_MISSING))
        if (tused != null && tused < LocalDate.now()) errors.add(IncorrectDate("TUSED", IS_IN_PAST))
      }

      AP_PSS -> {
        if (led == null) errors.add(IncorrectDate("LED", IS_MISSING))
        if (tused == null) errors.add(IncorrectDate("TUSED", IS_MISSING))
        if (tused != null && tused < LocalDate.now()) errors.add(IncorrectDate("TUSED", IS_IN_PAST))
      }
    }
    return errors
  }

  enum class ErrorType { IS_MISSING, IS_PRESENT, IS_IN_PAST }
  data class IncorrectDate(val dateName: String, val errorType: ErrorType)
}
