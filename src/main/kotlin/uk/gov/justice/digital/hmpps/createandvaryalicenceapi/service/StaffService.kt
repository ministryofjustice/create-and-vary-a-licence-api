package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComReviewCount
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdatePrisonUserRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.AddressResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper.AddressMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import java.time.LocalDateTime

@Service
class StaffService(
  private val staffRepository: StaffRepository,
  private val deliusApiClient: DeliusApiClient,
  private val licenceRepository: LicenceRepository,
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
    log.info(
      "Attempting to update COM details for staffIdentifier={} username={}",
      comDetails.staffIdentifier,
      comDetails.staffUsername,
    )

    val comResult = staffRepository.findCommunityOffenderManager(
      comDetails.staffIdentifier,
      comDetails.staffUsername,
    )

    if (comResult.isEmpty()) {
      log.info(
        "No existing COM found. Creating new record for staffIdentifier={} username={}",
        comDetails.staffIdentifier,
        comDetails.staffUsername,
      )
      return staffRepository.saveAndFlush(
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
        "Multiple COM records found for staffIdentifier={} username={}. Using the first match.",
        comDetails.staffIdentifier,
        comDetails.staffUsername,
      )
    }

    val com = comResult.first()

    if (isUpdate(com, comDetails)) {
      log.info(
        "Updating COM record (id={}) for staffIdentifier={} username={}",
        com.id,
        comDetails.staffIdentifier,
        comDetails.staffUsername,
      )
      with(com) {
        staffIdentifier = comDetails.staffIdentifier
        username = comDetails.staffUsername.uppercase()
        email = comDetails.staffEmail
        firstName = comDetails.firstName
        lastName = comDetails.lastName
        lastUpdatedTimestamp = LocalDateTime.now()
      }
    } else {
      log.info(
        "No changes detected for COM record (id={}) - skipping update for staffIdentifier={} username={}",
        com.id,
        comDetails.staffIdentifier,
        comDetails.staffUsername,
      )
    }
    return com
  }

  @Transactional
  fun updatePrisonUser(request: UpdatePrisonUserRequest) {
    log.info("Updating prison user with username=${request.staffUsername}")

    val found = staffRepository.findPrisonUserByUsernameIgnoreCase(request.staffUsername)
    when {
      found == null -> {
        log.info("No existing prison user found — creating new record")
        staffRepository.saveAndFlush(request.toEntity())
      }
      found.isUpdate(request) -> {
        log.info("Prison user found — applying updates")
        found.updatedWith(request)
      }
      else -> {
        log.info("No update needed for prison user with username=${request.staffUsername}")
      }
    }
  }

  fun getReviewCounts(staffIdentifier: Long): ComReviewCount {
    val com = staffRepository.findByStaffIdentifier(staffIdentifier)
      ?: error("Staff with identifier $staffIdentifier not found")

    val teamCodes = this.deliusApiClient.getTeamsCodesForUser(staffIdentifier)

    val comReviewCount = this.licenceRepository.getLicenceReviewCountForCom(com)

    val teamsReviewCount = this.licenceRepository.getLicenceReviewCountForTeams(teamCodes)

    return ComReviewCount(
      comReviewCount,
      teamsReviewCount,
    )
  }

  fun getPreferredAddress(): List<AddressResponse> {
    val username = SecurityContextHolder.getContext().authentication.name
    val staff = staffRepository.findByUsernameIgnoreCaseWithAddresses(username)
      ?: error("Staff with username $username not found")

    log.info("Retrieving preferred addresses for staff ${staff.fullName}")
    return staff.savedAppointmentAddresses.map { address -> AddressMapper.toResponse(address) }
  }

  @Transactional
  fun deleteAddressByReference(reference: String) {
    val username = SecurityContextHolder.getContext().authentication.name
    val staff = staffRepository.findByUsernameIgnoreCaseWithAddresses(username)
      ?: error("Staff with username $username not found")

    log.info("Deleting address with reference $reference for staff ${staff.fullName}")

    val addressToDelete = staff.savedAppointmentAddresses.firstOrNull { it.reference == reference }
      ?: throw EntityNotFoundException("Address with reference $reference not found for staff ${staff.fullName}")

    staff.savedAppointmentAddresses.remove(addressToDelete)
  }

  private fun PrisonUser.updatedWith(
    updatedDetails: UpdatePrisonUserRequest,
  ): PrisonUser {
    username = updatedDetails.staffUsername.uppercase()
    email = updatedDetails.staffEmail
    firstName = updatedDetails.firstName
    lastName = updatedDetails.lastName
    lastUpdatedTimestamp = LocalDateTime.now()
    return this
  }

  fun isUpdate(
    com: CommunityOffenderManager,
    comDetails: UpdateComRequest,
  ) = (comDetails.firstName != com.firstName) ||
    (comDetails.lastName != com.lastName) ||
    (comDetails.staffEmail != com.email) ||
    (!comDetails.staffUsername.equals(com.username, ignoreCase = true)) ||
    (comDetails.staffIdentifier != com.staffIdentifier)

  private fun PrisonUser.isUpdate(caDetails: UpdatePrisonUserRequest) = (caDetails.firstName != this.firstName) ||
    (caDetails.lastName != this.lastName) ||
    (caDetails.staffEmail != this.email) ||
    (!caDetails.staffUsername.equals(this.username, ignoreCase = true))
}
