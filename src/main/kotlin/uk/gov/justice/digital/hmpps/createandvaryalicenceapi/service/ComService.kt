package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ProbationUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.CommunityOffenderManagerRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchResponseResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.ProbationSearchSortByRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.SearchDirection
import java.time.LocalDateTime

@Service
class ComService(
  private val communityOffenderManagerRepository: CommunityOffenderManagerRepository,
  private val licenceRepository: LicenceRepository,
  private val communityApiClient: CommunityApiClient,
  private val probationSearchApiClient: ProbationSearchApiClient,
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
    val comResult = this.communityOffenderManagerRepository.findByStaffIdentifierOrUsernameIgnoreCase(
      comDetails.staffIdentifier,
      comDetails.staffUsername,
    )

    if (comResult == null || comResult.isEmpty()) {
      return this.communityOffenderManagerRepository.saveAndFlush(
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
    if (
      (comDetails.firstName != com.firstName) ||
      (comDetails.lastName != com.lastName) ||
      (comDetails.staffEmail != com.email) ||
      (!comDetails.staffUsername.equals(com.username, ignoreCase = true)) ||
      (comDetails.staffIdentifier != com.staffIdentifier)
    ) {
      return this.communityOffenderManagerRepository.saveAndFlush(
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

  fun searchForOffenderOnStaffCaseload(body: ProbationUserSearchRequest): ProbationSearchResult {
    val probationSearchApiSortBy = body.sortBy.map {
      ProbationSearchSortByRequest(
        it.field.probationSearchApiSortType,
        if (it.direction == SearchDirection.ASC) "asc" else "desc",
      )
    }

    var entityProbationSearchResult: List<ProbationSearchResponseResult>

    if (body.query.isEmpty()) {
      entityProbationSearchResult = emptyList()
    } else {
      entityProbationSearchResult = probationSearchApiClient.searchLicenceCaseloadByTeam(
        body.query,
        communityApiClient.getTeamsCodesForUser(body.staffIdentifier),
        probationSearchApiSortBy,
      )
    }

    val enrichedProbationSearchResults = entityProbationSearchResult.mapNotNull {
      val licences =
        licenceRepository.findAllByCrnAndStatusCodeIn(it.identifiers.crn, LicenceStatus.IN_FLIGHT_LICENCES)

      // If an empty list has been returned, there are no relevant licences relating to search for the offender
      if (licences.isEmpty()) {
        null
      } else {
        val nonActiveLicenceStatuses = LicenceStatus.IN_FLIGHT_LICENCES - LicenceStatus.ACTIVE

        val currentLicence =
          if (licences.size > 1) licences.find { licence -> licence.statusCode in nonActiveLicenceStatuses } else licences.first()

        it.transformToModelFoundProbationRecord(currentLicence)
      }
    }

    val onProbationCount = enrichedProbationSearchResults.count { it.isOnProbation == true }
    val inPrisonCount = enrichedProbationSearchResults.count { it.isOnProbation == false }

    return ProbationSearchResult(
      enrichedProbationSearchResults,
      inPrisonCount,
      onProbationCount,
    )
  }
}
