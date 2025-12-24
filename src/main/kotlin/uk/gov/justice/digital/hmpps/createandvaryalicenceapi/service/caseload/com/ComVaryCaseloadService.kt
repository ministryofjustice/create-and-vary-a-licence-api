package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComVaryCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCaseRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadType.ComVaryStaffCaseload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadType.ComVaryTeamCaseload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TelemetryService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com.ManagedOffenderCrnTransformer.toProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_REJECTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_SUBMITTED

@Service
class ComVaryCaseloadService(
  private val deliusApiClient: DeliusApiClient,
  private val licenceCaseRepository: LicenceCaseRepository,
  private val telemetryService: TelemetryService,
) {
  companion object {
    private val COM_VARY_LICENCE_STATUSES =
      listOf(ACTIVE, VARIATION_IN_PROGRESS, VARIATION_SUBMITTED, VARIATION_APPROVED, VARIATION_REJECTED)
  }

  fun getStaffVaryCaseload(deliusStaffIdentifier: Long): List<ComVaryCase> {
    val managedOffenders = deliusApiClient.getManagedOffenders(deliusStaffIdentifier)
    val cases = mapCaseToVaryLicence(managedOffenders)

    telemetryService.recordCaseloadLoad(ComVaryStaffCaseload, setOf(deliusStaffIdentifier.toString()), cases)
    return cases
  }

  fun getTeamVaryCaseload(probationTeamCodes: List<String>, teamSelected: List<String>): List<ComVaryCase> {
    val teamCode = getTeamCode(probationTeamCodes, teamSelected)
    val managedOffenders = deliusApiClient.getManagedOffendersByTeam(teamCode)
    val cases = mapCaseToVaryLicence(managedOffenders)

    telemetryService.recordCaseloadLoad(ComVaryTeamCaseload, setOf(teamCode), cases)
    return cases
  }

  private fun getTeamCode(probationTeamCodes: List<String>, teamSelected: List<String>): String = if (teamSelected.isNotEmpty()) {
    teamSelected.first()
  } else {
    probationTeamCodes.first()
  }

  fun mapCaseToVaryLicence(cases: List<ManagedOffenderCrn>): List<ComVaryCase> {
    val licences = findExistingActiveAndVariationLicences(cases.mapNotNull { it.crn })
    return cases.mapNotNull { case ->
      val caseLicences = licences.filter { licence -> case.crn == licence.crn }
      val licence = findVaryLicenceToDisplay(caseLicences)
      when {
        licence == null -> null
        else ->
          ComVaryCase(
            licenceId = licence.licenceId,
            licenceType = licence.typeCode,
            licenceStatus = licence.statusCode,
            crnNumber = licence.crn,
            prisonerNumber = licence.prisonNumber,
            kind = licence.kind,
            name = "${licence.forename} ${licence.surname}".trim().convertToTitleCase(),
            releaseDate = licence.licenceStartDate,
            probationPractitioner = case.toProbationPractitioner() ?: ProbationPractitioner.UNALLOCATED,
            isReviewNeeded = licence.isReviewNeeded(),
          )
      }
    }.sortedWith(compareBy<ComVaryCase> { it.releaseDate }.thenBy { it.name })
  }

  private fun findExistingActiveAndVariationLicences(crnList: List<String>): List<LicenceComCase> = if (crnList.isEmpty()) {
    emptyList()
  } else {
    licenceCaseRepository.findLicenceCasesForCom(crns = crnList, statusCodes = COM_VARY_LICENCE_STATUSES)
  }

  private fun findVaryLicenceToDisplay(licences: List<LicenceComCase>): LicenceComCase? = when {
    licences.isEmpty() -> null
    licences.size > 1 -> licences.find { licence -> licence.statusCode != ACTIVE && !licence.isReviewNeeded() }
    else -> licences.first()
  }
}
