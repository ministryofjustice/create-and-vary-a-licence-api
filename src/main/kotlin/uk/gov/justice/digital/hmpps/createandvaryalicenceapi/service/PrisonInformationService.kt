package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Staff
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.Prison
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType.SYSTEM_EVENT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType.USER_EVENT

@Service
class PrisonInformationService(
  val prisonApiClient: PrisonApiClient,
  val staffRepository: StaffRepository,
  val auditEventRepository: AuditEventRepository,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  enum class UpdateType(val messageBuilder: (licence: Licence) -> String) {
    MOVEMENT(
      { "Prison information changed for ${it.forename} ${it.surname} on prisoner receive event" },
    ),
    SUPPORTING_PRISON_UPDATE(
      { "Supporting prison information changed for ${it.forename} ${it.surname}" },
    ),
  }

  @Transactional
  fun updatePrisonInformation(updateType: UpdateType, licences: List<Licence>, prisonCode: String) {
    val prison = prisonApiClient.getPrisonInformation(prisonCode)
    updateLicences(updateType, licences, prison)
  }

  private fun updateLicences(updateType: UpdateType, licences: List<Licence>, prison: Prison) {
    licences.forEach { licence ->
      val previousPrisonCode = licence.prisonCode
      if (previousPrisonCode == prison.prisonId) {
        log.info("Prison code for licence id ${licence.id} is already ${prison.prisonId}, skipping prison update")
        return@forEach
      }
      log.info("Updating prison code for licence id ${licence.id}")

      val staff = getStaffRecord()

      licence.updatePrisonInfo(
        prisonCode = prison.prisonId,
        prisonDescription = prison.description,
        prisonTelephone = prison.getPrisonContactNumber(),
        staffMember = staff,
      )

      auditEventRepository.saveAndFlush(
        AuditEvent(
          licenceId = licence.id,
          username = staff?.username ?: SYSTEM_USER,
          fullName = staff?.fullName ?: SYSTEM_USER,
          eventType = if (staff != null) USER_EVENT else SYSTEM_EVENT,
          summary = updateType.messageBuilder(licence),
          detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode} version ${licence.version}",
          changes = mapOf(
            "field" to "prisonCode",
            "previousValue" to (previousPrisonCode ?: ""),
            "newValue" to (licence.prisonCode ?: ""),
          ),
        ),
      )
      log.info("Updated prison code for licence id ${licence.id} from $previousPrisonCode to ${licence.prisonCode}")
    }
  }

  private fun getStaffRecord(): Staff? {
    val username = SecurityContextHolder.getContext().authentication?.name

    if (username.isNullOrBlank()) {
      return null
    }
    return staffRepository.findByUsernameIgnoreCase(username)
  }
}
