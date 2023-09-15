package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.FoundProbationRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ProbationUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.CommunityOffenderManagerRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchResponseResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.ProbationSearchSortByRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.SearchDirection
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ComService(
  private val communityOffenderManagerRepository: CommunityOffenderManagerRepository,
  private val licenceRepository: LicenceRepository,
  private val communityApiClient: CommunityApiClient,
  private val probationSearchApiClient: ProbationSearchApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
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

    val entityProbationSearchResult = probationSearchApiClient.searchLicenceCaseloadByTeam(
      body.query,
      communityApiClient.getTeamsCodesForUser(body.staffIdentifier),
      probationSearchApiSortBy,
    )

    val resultsWithLicences: List<Pair<ProbationSearchResponseResult, Licence?>> =
      entityProbationSearchResult.map { it to getLicence(it) }

    val resultsWithUnstartedLicences: List<PrisonerSearchPrisoner> =
      findPrisoners(resultsWithLicences.filter { (_, licence) -> licence == null })

    val enrichedProbationSearchResults = resultsWithLicences.mapNotNull { (result, licence) ->
      if (licence == null) {
        createUnstartedRecord(
          result,
          resultsWithUnstartedLicences.find { it.prisonerNumber == result.identifiers.noms },
        )
      } else {
        createRecord(result, licence)
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

  private fun PrisonerSearchPrisoner.getLicenceType(): LicenceType {
    return if (this.licenceExpiryDate.isNullOrBlank()) {
      LicenceType.PSS
    } else if (this.topUpSupervisionExpiryDate.isNullOrBlank() || LocalDate.parse(
        this.topUpSupervisionExpiryDate,
      ) <= LocalDate.parse(this.licenceExpiryDate)
    ) {
      LicenceType.AP
    } else {
      LicenceType.AP_PSS
    }
  }

  private fun PrisonerSearchPrisoner.getReleaseDate(): LocalDate? {
    return if (this.confirmedReleaseDate.isNullOrBlank()) {
      LocalDate.parse(this.releaseDate)
    } else {
      LocalDate.parse(this.confirmedReleaseDate)
    }
  }

  private fun getLicence(result: ProbationSearchResponseResult): Licence? {
    val licences =
      licenceRepository.findAllByCrnAndStatusCodeIn(result.identifiers.crn, LicenceStatus.IN_FLIGHT_LICENCES)
    return if (licences.isEmpty()) {
      null
    } else {
      val nonActiveLicenceStatuses = LicenceStatus.IN_FLIGHT_LICENCES - LicenceStatus.ACTIVE
      if (licences.size > 1) licences.find { licence -> licence.statusCode in nonActiveLicenceStatuses } else licences.first()
    }
  }

  private fun findPrisoners(record: List<Pair<ProbationSearchResponseResult, Licence?>>): List<PrisonerSearchPrisoner> {
    return record.mapNotNull { (result, _) ->
      if (result.identifiers.noms != null) {
        val prisonerSearchResult =
          this.prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(result.identifiers.noms))
        if (prisonerSearchResult.isEmpty()) {
          null
        } else {
          prisonerSearchResult.first()
        }
      } else {
        null
      }
    }
  }

  private fun createUnstartedRecord(
    result: ProbationSearchResponseResult,
    prisoner: PrisonerSearchPrisoner?,
  ): FoundProbationRecord? {
    if (prisoner != null) {
      // if both dates are null from the prisoner, we do not want to show the result as a licence cannot be created without them
      return if (prisoner.confirmedReleaseDate.isNullOrBlank() && prisoner.releaseDate.isNullOrBlank()) {
        null
      } else {
        val licenceType = prisoner.getLicenceType()
        val releaseDate = prisoner.getReleaseDate()
        result.transformToUnstartedRecord(
          releaseDate,
          licenceType,
          LicenceStatus.NOT_STARTED,
        )
      }
    }
    return null
  }

  private fun createRecord(result: ProbationSearchResponseResult, licence: Licence): FoundProbationRecord {
    return result.transformToModelFoundProbationRecord(licence)
  }
}
