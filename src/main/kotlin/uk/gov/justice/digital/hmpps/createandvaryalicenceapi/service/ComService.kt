package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

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

  @Transactional
  fun updateComDetails(comDetails: UpdateComRequest): CommunityOffenderManager {
    val com = this.communityOffenderManagerRepository.findByStaffIdentifier(comDetails.staffIdentifier)
    val updatedCom = if (com !== null) {
      com.copy(
        email = comDetails.staffEmail,
        firstName = comDetails.firstName,
        lastName = comDetails.lastName,
        lastUpdatedTimestamp = LocalDateTime.now()
      )
    } else {
      CommunityOffenderManager(
        username = comDetails.staffUsername,
        staffIdentifier = comDetails.staffIdentifier,
        email = comDetails.staffEmail,
        firstName = comDetails.firstName,
        lastName = comDetails.lastName
      )
    }

    return this.communityOffenderManagerRepository.saveAndFlush(updatedCom)
  }
}
