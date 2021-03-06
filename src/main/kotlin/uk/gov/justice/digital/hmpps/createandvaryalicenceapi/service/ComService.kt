package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.CommunityOffenderManagerRepository
import java.time.LocalDateTime

@Service
class ComService(
  private val communityOffenderManagerRepository: CommunityOffenderManagerRepository,
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
    val comResult = this.communityOffenderManagerRepository.findByStaffIdentifierOrUsername(
      comDetails.staffIdentifier, comDetails.staffUsername
    )

    if (comResult == null || comResult.isEmpty()) {
      return this.communityOffenderManagerRepository.saveAndFlush(
        CommunityOffenderManager(
          username = comDetails.staffUsername,
          staffIdentifier = comDetails.staffIdentifier,
          email = comDetails.staffEmail,
          firstName = comDetails.firstName,
          lastName = comDetails.lastName
        )
      )
    }

    if (comResult.count() > 1) {
      log.warn(
        "More then one COM record found for staffId {} username {}",
        comDetails.staffIdentifier,
        comDetails.staffUsername
      )
    }

    val com = comResult.first()

    // only update entity if data is different
    if (
      (comDetails.firstName != com.firstName) ||
      (comDetails.lastName != com.lastName) ||
      (comDetails.staffEmail != com.email) ||
      (comDetails.staffUsername != com.username) ||
      (comDetails.staffIdentifier != com.staffIdentifier)
    ) {
      return this.communityOffenderManagerRepository.saveAndFlush(
        com.copy(
          staffIdentifier = comDetails.staffIdentifier,
          username = comDetails.staffUsername,
          email = comDetails.staffEmail,
          firstName = comDetails.firstName,
          lastName = comDetails.lastName,
          lastUpdatedTimestamp = LocalDateTime.now()
        )
      )
    }

    return com
  }
}
