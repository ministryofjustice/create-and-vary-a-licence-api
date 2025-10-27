package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_REJECTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_SUBMITTED

@Service
class ComVaryCaseloadService(
  private val deliusApiClient: DeliusApiClient,
  private val licenceService: LicenceService,
  private val releaseDateService: ReleaseDateService,
) {

  fun getStaffVaryCaseload(deliusStaffIdentifier: Long): List<ComCase> {
    val managedOffenders = deliusApiClient.getManagedOffenders(deliusStaffIdentifier)
    val casesToLicences = mapCaseToVaryLicence(managedOffenders)
    return transformToVaryCaseload(casesToLicences)
  }

  fun getTeamVaryCaseload(probationTeamCodes: List<String>, teamSelected: List<String>): List<ComCase> {
    val teamCode = getTeamCode(probationTeamCodes, teamSelected)
    val managedOffenders = deliusApiClient.getManagedOffendersByTeam(teamCode)
    val casesToLicences = mapCaseToVaryLicence(managedOffenders)
    return transformToVaryCaseload(casesToLicences)
  }

  fun mapCaseToVaryLicence(cases: List<ManagedOffenderCrn>): Map<ManagedOffenderCrn, LicenceSummary> {
    val licences = findExistingActiveAndVariationLicences(cases.mapNotNull { it.crn })
    return cases.mapNotNull { case ->
      val caseLicences = licences.filter { licence -> case.crn == licence.crn }
      val varyLicence = findVaryLicenceToDisplay(caseLicences)
      when {
        varyLicence == null -> null
        else -> case to varyLicence
      }
    }.toMap()
  }

  private fun getComDetails(casesToLicences: Map<ManagedOffenderCrn, LicenceSummary>): Map<String?, ProbationPractitioner> {
    val comUsernames = casesToLicences.mapNotNull { (_, licence) -> licence.comUsername }.distinct()
    val coms = deliusApiClient.getStaffDetailsByUsername(comUsernames)
    return casesToLicences.map { (case, licence) ->
      val com = coms.find { c -> licence.comUsername.equals(c.username, ignoreCase = true) }
      when {
        com != null -> case.crn to ProbationPractitioner(
          com.code,
          name = com.name.fullName(),
          staffUsername = com.username,
        )

        case.staff == null || case.staff.unallocated == true -> case.crn to ProbationPractitioner()
        else -> case.crn to ProbationPractitioner(
          staffCode = case.staff.code,
          name = case.staff.name?.fullName(),
        )
      }
    }.toMap()
  }

  private fun findExistingActiveAndVariationLicences(crnList: List<String>): List<LicenceSummary> = if (crnList.isEmpty()) {
    emptyList()
  } else {
    licenceService.findLicencesForCrnsAndStatuses(
      crns = crnList,
      statusCodes = listOf(
        ACTIVE,
        VARIATION_IN_PROGRESS,
        VARIATION_SUBMITTED,
        VARIATION_APPROVED,
        VARIATION_REJECTED,
      ),
    )
  }

  private fun getTeamCode(probationTeamCodes: List<String>, teamSelected: List<String>): String = if (teamSelected.isNotEmpty()) {
    teamSelected.first()
  } else {
    probationTeamCodes.first()
  }

  private fun transformToVaryCaseload(casesToLicences: Map<ManagedOffenderCrn, LicenceSummary>): List<ComCase> {
    val comDetails = getComDetails(casesToLicences)

    return casesToLicences.map { (case, licence) ->
      val hardStopKind = releaseDateService.getHardStopKind(licence.toHardStopData())

      ComCase(
        licenceId = licence.licenceId,
        licenceType = licence.licenceType,
        licenceStatus = licence.licenceStatus,
        crnNumber = licence.crn,
        prisonerNumber = licence.nomisId,
        hardStopKind = hardStopKind,
        kind = licence.kind,
        name = "${licence.forename} ${licence.surname}".trim().convertToTitleCase(),
        releaseDate = licence.licenceStartDate,
        probationPractitioner = comDetails[case.crn],
        isReviewNeeded = licence.isReviewNeeded,
      )
    }.sortedWith(compareBy<ComCase> { it.releaseDate }.thenBy { it.name })
  }

  private fun findVaryLicenceToDisplay(licences: List<LicenceSummary>): LicenceSummary? = when {
    licences.isEmpty() -> null
    licences.size > 1 -> licences.find { licence -> licence.licenceStatus != ACTIVE && !licence.isReviewNeeded }
    else -> licences.first()
  }
}
