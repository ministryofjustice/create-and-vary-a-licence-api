package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCaseRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadType.ComVaryStaffCaseload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadType.ComVaryTeamCaseload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TelemetryService
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
  private val licenceCaseRepository: LicenceCaseRepository,
  private val releaseDateService: ReleaseDateService,
  private val telemetryService: TelemetryService,
) {

  fun getStaffVaryCaseload(deliusStaffIdentifier: Long): List<ComCase> {
    val managedOffenders = deliusApiClient.getManagedOffenders(deliusStaffIdentifier)
    val casesToLicences = mapCaseToVaryLicence(managedOffenders)
    val cases = transformToVaryCaseload(casesToLicences)

    telemetryService.recordCaseloadLoad(ComVaryStaffCaseload, setOf(deliusStaffIdentifier.toString()), cases)
    return cases
  }

  fun getTeamVaryCaseload(probationTeamCodes: List<String>, teamSelected: List<String>): List<ComCase> {
    val teamCode = getTeamCode(probationTeamCodes, teamSelected)
    val managedOffenders = deliusApiClient.getManagedOffendersByTeam(teamCode)
    val casesToLicences = mapCaseToVaryLicence(managedOffenders)
    val cases = transformToVaryCaseload(casesToLicences)

    telemetryService.recordCaseloadLoad(ComVaryTeamCaseload, setOf(teamCode), cases)
    return cases
  }

  fun mapCaseToVaryLicence(cases: List<ManagedOffenderCrn>): Map<ManagedOffenderCrn, LicenceComCase> {
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

  private fun getComDetails(casesToLicences: Map<ManagedOffenderCrn, LicenceComCase>): Map<String?, ProbationPractitioner> {
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

  private fun findExistingActiveAndVariationLicences(crnList: List<String>): List<LicenceComCase> = if (crnList.isEmpty()) {
    emptyList()
  } else {
    licenceCaseRepository.findLicenceCasesForCom(
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

  private fun transformToVaryCaseload(casesToLicences: Map<ManagedOffenderCrn, LicenceComCase>): List<ComCase> {
    val comDetails = getComDetails(casesToLicences)

    return casesToLicences.map { (case, licence) ->
      val hardStopKind = releaseDateService.getHardStopKind(licence)

      ComCase(
        licenceId = licence.licenceId,
        licenceType = licence.typeCode,
        licenceStatus = licence.statusCode,
        crnNumber = licence.crn,
        prisonerNumber = licence.prisonNumber,
        hardStopKind = hardStopKind,
        kind = licence.kind,
        name = "${licence.forename} ${licence.surname}".trim().convertToTitleCase(),
        releaseDate = licence.licenceStartDate,
        probationPractitioner = comDetails[case.crn],
        isReviewNeeded = licence.isReviewNeeded(),
      )
    }.sortedWith(compareBy<ComCase> { it.releaseDate }.thenBy { it.name })
  }

  private fun findVaryLicenceToDisplay(licences: List<LicenceComCase>): LicenceComCase? = when {
    licences.isEmpty() -> null
    licences.size > 1 -> licences.find { licence -> licence.statusCode != ACTIVE && !licence.isReviewNeeded() }
    else -> licences.first()
  }
}
