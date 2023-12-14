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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CaseloadResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.ProbationSearchSortByRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.SearchDirection
import java.time.LocalDateTime

@Service
class ComService(
  private val communityOffenderManagerRepository: CommunityOffenderManagerRepository,
  private val licenceRepository: LicenceRepository,
  private val communityApiClient: CommunityApiClient,
  private val probationSearchApiClient: ProbationSearchApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val prisonApiClient: PrisonApiClient,
  private val eligibilityService: EligibilityService,
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

    if (comResult.isNullOrEmpty()) {
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
    if (com.isUpdate(comDetails)) {
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

  private fun CommunityOffenderManager.isUpdate(comDetails: UpdateComRequest) =
    (comDetails.firstName != this.firstName) ||
      (comDetails.lastName != this.lastName) ||
      (comDetails.staffEmail != this.email) ||
      (!comDetails.staffUsername.equals(this.username, ignoreCase = true)) ||
      (comDetails.staffIdentifier != this.staffIdentifier)

  fun searchForOffenderOnStaffCaseload(body: ProbationUserSearchRequest): ProbationSearchResult {
    val teamCaseloadResult = probationSearchApiClient.searchLicenceCaseloadByTeam(
      body.query,
      communityApiClient.getTeamsCodesForUser(body.staffIdentifier),
      body.getSortBy(),
    )

    val resultsWithLicences: List<Pair<CaseloadResult, Licence?>> =
      teamCaseloadResult.map { it to getLicence(it) }

    val prisonerRecords = findPrisonersForRelevantRecords(resultsWithLicences)

    val searchResults = resultsWithLicences.mapNotNull { (result, licence) ->
      when (licence) {
        null -> createNotStartedRecord(result, prisonerRecords[result.identifiers.noms])
        else -> createRecord(result, licence, prisonerRecords[result.identifiers.noms])
      }
    }.filterOutHdc(prisonerRecords)

    val onProbationCount = searchResults.count { it.isOnProbation == true }
    val inPrisonCount = searchResults.count { it.isOnProbation == false }

    return ProbationSearchResult(
      searchResults,
      inPrisonCount,
      onProbationCount,
    )
  }

  fun getIneligibilityReasons(prisonNumber: String): List<String> {
    val prisoners = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(prisonNumber))
    if (prisoners.size != 1) {
      error("Found ${prisoners.size} prisoners for: $prisonNumber")
    }
    val reasons = eligibilityService.getIneligibilityReasons(prisoners.first())
    val hdcReasonIfPresent = if (prisoners.findBookingsWithHdc().isEmpty()) emptyList() else listOf("Approved for HDC")
    return reasons + hdcReasonIfPresent
  }

  private fun PrisonerSearchPrisoner.getLicenceType() = when {
    this.licenceExpiryDate == null -> LicenceType.PSS
    this.topUpSupervisionExpiryDate == null || topUpSupervisionExpiryDate <= licenceExpiryDate -> LicenceType.AP
    else -> LicenceType.AP_PSS
  }

  private fun PrisonerSearchPrisoner.getReleaseDate() = confirmedReleaseDate ?: releaseDate

  private fun getLicence(result: CaseloadResult): Licence? {
    val licences =
      licenceRepository.findAllByCrnAndStatusCodeIn(result.identifiers.crn, LicenceStatus.IN_FLIGHT_LICENCES)
    return if (licences.isEmpty()) {
      null
    } else {
      val nonActiveLicenceStatuses = LicenceStatus.IN_FLIGHT_LICENCES - LicenceStatus.ACTIVE
      if (licences.size > 1) licences.find { licence -> licence.statusCode in nonActiveLicenceStatuses } else licences.first()
    }
  }

  private fun findPrisonersForRelevantRecords(record: List<Pair<CaseloadResult, Licence?>>): Map<String, PrisonerSearchPrisoner> {
    val prisonNumbers = record
      .filter { (_, licence) -> licence == null || !licence.statusCode.isOnProbation() }
      .mapNotNull { (result, _) -> result.identifiers.noms }

    if (prisonNumbers.isEmpty()) {
      return emptyMap()
    }

    // we gather further data from prisoner search if there is no licence or a create licence
    val prisoners = this.prisonerSearchApiClient.searchPrisonersByNomisIds(prisonNumbers)

    return prisoners.associateBy { it.prisonerNumber }
  }

  private fun createNotStartedRecord(deliusOffender: CaseloadResult, prisonOffender: PrisonerSearchPrisoner?) = when {
    // no match for prisoner in Delius
    prisonOffender == null -> null

    !eligibilityService.isEligibleForCvl(prisonOffender) -> null

    else -> {
      deliusOffender.transformToUnstartedRecord(
        releaseDate = prisonOffender.getReleaseDate(),
        licenceType = prisonOffender.getLicenceType(),
        LicenceStatus.NOT_STARTED,
      )
    }
  }

  private fun createRecord(deliusOffender: CaseloadResult, licence: Licence, prisonOffender: PrisonerSearchPrisoner?): FoundProbationRecord? =
    when {
      prisonOffender == null -> null

      licence.statusCode.isOnProbation() -> deliusOffender.transformToModelFoundProbationRecord(licence)

      eligibilityService.isExistingLicenceEligible(prisonOffender) -> deliusOffender.transformToModelFoundProbationRecord(licence)

      else -> null
    }

  private fun ProbationUserSearchRequest.getSortBy() =
    this.sortBy.map {
      ProbationSearchSortByRequest(
        it.field.probationSearchApiSortType,
        if (it.direction == SearchDirection.ASC) "asc" else "desc",
      )
    }

  private fun List<PrisonerSearchPrisoner>.findBookingsWithHdc(): List<Long> {
    val bookingsWithHdc = this
      .filter { it.homeDetentionCurfewEligibilityDate != null }
      .map { it.bookingId.toLong() }
    val hdcStatuses = prisonApiClient.getHdcStatuses(bookingsWithHdc)
    return hdcStatuses.filter { it.approvalStatus == "APPROVED" }.mapNotNull { it.bookingId }
  }

  private fun List<FoundProbationRecord>.filterOutHdc(prisonerRecords: Map<String, PrisonerSearchPrisoner>): List<FoundProbationRecord> {
    val prisonersForHdcCheck = this.filter { it.licenceStatus == LicenceStatus.NOT_STARTED }.mapNotNull { prisonerRecords[it.nomisId] }
    val bookingIdsWithHdc = prisonersForHdcCheck.findBookingsWithHdc()

    val prisonersWithoutHdc = prisonerRecords.values.filterNot { bookingIdsWithHdc.contains(it.bookingId.toLong()) }
    return this.filter { prisonersWithoutHdc.any { prisoner -> prisoner.prisonerNumber == it.nomisId } }
  }
}
