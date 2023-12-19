package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonCaseAdministrator
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Staff
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdatePrisonCaseAdminRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import java.time.LocalDateTime

@Service
class StaffService(
  private val staffRepository: StaffRepository,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Check if record already exists. If so, check if any of the details have changed before performing an update.
   * Check if the username and staffId do not match. This should not happen, unless a Delius account is updated to point
   * at another linked account using the staffId. In this scenario, we should update the existing record to reflect
   * the new username and or staffId.
   */
  @Transactional
  fun updateComDetails(comDetails: UpdateComRequest): CommunityOffenderManager {
    val comResult = this.staffRepository.findByStaffIdentifierOrUsernameIgnoreCase(
      comDetails.staffIdentifier,
      comDetails.staffUsername,
    )

    if (comResult.isNullOrEmpty()) {
      return this.staffRepository.saveAndFlush(
        CommunityOffenderManager(
          username = comDetails.staffUsername.uppercase(),
          staffIdentifier = comDetails.staffIdentifier,
          email = comDetails.staffEmail,
          firstName = comDetails.firstName,
          lastName = comDetails.lastName,
        ),
      )
    }

    if (comResult.count() > 1) {
      log.warn(
        "More then one COM record found for staffId {} username {}",
        comDetails.staffIdentifier,
        comDetails.staffUsername,
      )
    }

    val com = comResult.first()

    // only update entity if data is different
    if (com.isUpdate(comDetails)) {
      return this.staffRepository.saveAndFlush(
        com.copy(
          staffIdentifier = comDetails.staffIdentifier,
          username = comDetails.staffUsername.uppercase(),
          email = comDetails.staffEmail,
          firstName = comDetails.firstName,
          lastName = comDetails.lastName,
          lastUpdatedTimestamp = LocalDateTime.now(),
        ),
      )
    }

    return com
  }

  @Transactional
  fun updatePrisonCaseAdmin(request: UpdatePrisonCaseAdminRequest): Staff {
    val found = this.staffRepository.findPrisonCaseAdministratorByUsernameIgnoreCase(request.staffUsername)

    return when {
      found == null -> this.staffRepository.saveAndFlush(request.toEntity())
      found.isUpdate(request) -> this.staffRepository.saveAndFlush(found.updatedWith(request))
      else -> found
    }
  }

  private fun PrisonCaseAdministrator.updatedWith(
    updatedDetails: UpdatePrisonCaseAdminRequest,
  ) = copy(
    username = updatedDetails.staffUsername.uppercase(),
    email = updatedDetails.staffEmail,
    firstName = updatedDetails.firstName,
    lastName = updatedDetails.lastName,
    lastUpdatedTimestamp = LocalDateTime.now(),
  )

  private fun CommunityOffenderManager.isUpdate(comDetails: UpdateComRequest) =
    (comDetails.firstName != this.firstName) ||
      (comDetails.lastName != this.lastName) ||
      (comDetails.staffEmail != this.email) ||
      (!comDetails.staffUsername.equals(this.username, ignoreCase = true)) ||
      (comDetails.staffIdentifier != this.staffIdentifier)

  private fun PrisonCaseAdministrator.isUpdate(caDetails: UpdatePrisonCaseAdminRequest) =
    (caDetails.firstName != this.firstName) ||
      (caDetails.lastName != this.lastName) ||
      (caDetails.staffEmail != this.email) ||
      (!caDetails.staffUsername.equals(this.username, ignoreCase = true))
}
