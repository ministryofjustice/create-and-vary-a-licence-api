package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComCreateCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCaseRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadType.ComCreateStaffCaseload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadType.ComCreateTeamCaseload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecordService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TelemetryService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com.ManagedOffenderCrnTransformer.toProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com.RelevantLicenceFinder.findRelevantLicencePerCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.CaseAccessResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.CaseAccessResponse.Companion.unrestricted
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.NOT_STARTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.isTodayOrInTheFuture

@Service
class ComCreateCaseloadService(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val deliusApiClient: DeliusApiClient,
  private val licenceCaseRepository: LicenceCaseRepository,
  private val cvlRecordService: CvlRecordService,
  private val telemetryService: TelemetryService,
  @param:Value("\${feature.toggle.lao.enabled}") private val laoEnabled: Boolean = false,

) {
  companion object {
    private val COM_CREATE_LICENCE_STATUSES = listOf(ACTIVE, IN_PROGRESS, SUBMITTED, APPROVED, TIMED_OUT)
  }

  fun getStaffCreateCaseload(deliusStaffIdentifier: Long): List<ComCreateCase> {
    val managedOffenders = deliusApiClient.getManagedOffenders(deliusStaffIdentifier)
    val cases = buildCreateCaseload(managedOffenders)

    telemetryService.recordCaseloadLoad(ComCreateStaffCaseload, setOf(deliusStaffIdentifier.toString()), cases)
    return cases
  }

  fun getTeamCreateCaseload(probationTeamCodes: List<String>, teamSelected: List<String>): List<ComCreateCase> {
    val teamCode = getTeamCode(probationTeamCodes, teamSelected)
    val managedOffenders = deliusApiClient.getManagedOffendersByTeam(teamCode)
    val cases = buildCreateCaseload(managedOffenders)

    telemetryService.recordCaseloadLoad(ComCreateTeamCaseload, setOf(teamCode), cases)
    return cases
  }

  private fun getTeamCode(probationTeamCodes: List<String>, teamSelected: List<String>): String = if (teamSelected.isNotEmpty()) {
    teamSelected.first()
  } else {
    probationTeamCodes.first()
  }

  private fun buildCreateCaseload(managedOffenders: List<ManagedOffenderCrn>): List<ComCreateCase> {
    val deliusAndNomisRecords = pairDeliusRecordsWithNomis(managedOffenders)
    val cvlRecords =
      cvlRecordService.getCvlRecords(deliusAndNomisRecords.map { (_, nomisRecord) -> nomisRecord })
    val eligibleCases = filterCasesEligibleForCvl(deliusAndNomisRecords, cvlRecords)
    val cases = createComCases(eligibleCases, cvlRecords)
    val filteredCases = filterFutureOrTimeservedReleases(cases)

    return transformToCreateCaseload(filteredCases)
  }

  private fun pairDeliusRecordsWithNomis(managedOffenders: List<ManagedOffenderCrn>): Map<ManagedOffenderCrn, PrisonerSearchPrisoner> {
    val caseloadNomisIds = managedOffenders.mapNotNull { offender -> offender.nomisId }

    val nomisRecords =
      prisonerSearchApiClient.searchPrisonersByNomisIds(caseloadNomisIds).associateBy { it.prisonerNumber }

    return managedOffenders.mapNotNull { deliusRecord -> nomisRecords[deliusRecord.nomisId]?.let { deliusRecord to it } }
      .toMap()
  }

  private fun filterCasesEligibleForCvl(
    cases: Map<ManagedOffenderCrn, PrisonerSearchPrisoner>,
    cvlRecords: List<CvlRecord>,
  ): Map<ManagedOffenderCrn, PrisonerSearchPrisoner> = cases.filter { (_, nomisRecord) ->
    cvlRecords.first { it.nomisId == nomisRecord.prisonerNumber }.isEligible
  }

  private fun createComCases(
    cases: Map<ManagedOffenderCrn, PrisonerSearchPrisoner>,
    cvlRecords: List<CvlRecord>,
  ): List<Case> {
    val crns = cases.map { (deliusRecord, _) -> deliusRecord.crn!! }
    val licencesByCrn = getExistingActiveAndPreReleaseLicences(crns).groupBy { it.crn }
    val cvlRecordsByNomisId = cvlRecords.associateBy { it.nomisId }
    val caseAccessRecords = if (laoEnabled) {
      getCaseAccessRecords(crns)
    } else {
      emptyMap()
    }

    return cases.mapNotNull { (deliusRecord, nomisRecord) ->
      val licences = licencesByCrn[deliusRecord.crn!!] ?: emptyList()
      val cvlRecord = cvlRecordsByNomisId[nomisRecord.prisonerNumber]!!
      val caseAccessRecord = caseAccessRecords[deliusRecord.crn] ?: unrestricted
      val isLao = caseAccessRecord.userExcluded || caseAccessRecord.userRestricted
      val probationPractitioner = if (isLao) {
        ProbationPractitioner.laoProbationPractitioner()
      } else {
        deliusRecord.toProbationPractitioner()
      }

      when {
        // No licences found for this offender so treat as a not started case
        licences.isEmpty() -> Case(
          probationPractitioner,
          nomisRecord,
          cvlRecord,
          createNotStartedLicenceDto(deliusRecord, nomisRecord, cvlRecord, isLao),
        )

        // Has an active licence so shouldn't appear in create caseload
        licences.any { it.licenceStatus == ACTIVE } ->
          null

        // Should appear in create caseload with relevant licence
        else ->
          createCaseForRelevantLicence(probationPractitioner, nomisRecord, cvlRecord, findRelevantLicencePerCase(licences), isLao)
      }
    }
  }

  private fun getExistingActiveAndPreReleaseLicences(crnList: List<String>): List<ComCreateCaseloadLicenceDto> = if (crnList.isEmpty()) {
    emptyList()
  } else {
    licenceCaseRepository.findLicenceCasesForCom(crns = crnList, statusCodes = COM_CREATE_LICENCE_STATUSES)
      .map { mapToLicenceDto(it) }
  }

  private fun mapToLicenceDto(case: LicenceComCase) = ComCreateCaseloadLicenceDto(
    licenceId = case.licenceId,
    versionOf = case.versionOfId,
    licenceStatus = case.statusCode,
    kind = case.kind,
    crn = case.crn,
    nomisId = case.prisonNumber!!,
    name = case.fullName,
    licenceType = case.typeCode,
    releaseDate = case.licenceStartDate,
    isReviewNeeded = case.isReviewNeeded(),
    // populated by findRelevantLicencePerCase
    licenceCreationType = null,
    isLao = false,
  )

  private fun createNotStartedLicenceDto(
    deliusRecord: ManagedOffenderCrn,
    nomisRecord: PrisonerSearchPrisoner,
    cvlRecord: CvlRecord,
    isLao: Boolean,
  ): ComCreateCaseloadLicenceDto {
    val kind = cvlRecord.hardStopKind ?: cvlRecord.eligibleKind!!
    val name = "${nomisRecord.firstName} ${nomisRecord.lastName}".trim().convertToTitleCase()
    val licenceStatus = if (cvlRecord.isTimedOut) {
      TIMED_OUT
    } else {
      NOT_STARTED
    }

    if (isLao) {
      return ComCreateCaseloadLicenceDto.restrictedCase(
        licenceStatus,
        kind,
        cvlRecord.licenceType,
        deliusRecord.crn,
        nomisRecord.prisonerNumber,
        cvlRecord.licenceStartDate,
      )
    }

    val caseLoadSummary = ComCreateCaseloadLicenceDto(
      licenceId = null,
      versionOf = null,
      licenceStatus = licenceStatus,
      licenceType = cvlRecord.licenceType,
      crn = deliusRecord.crn,
      nomisId = nomisRecord.prisonerNumber,
      name = name,
      releaseDate = cvlRecord.licenceStartDate,
      kind = kind,
      isReviewNeeded = false,
      licenceCreationType = null,
      isLao = false,
    )
    return findRelevantLicencePerCase(listOf(caseLoadSummary))
  }

  private fun filterFutureOrTimeservedReleases(
    cases: List<Case>,
  ): List<Case> = cases.filter {
    it.comLicenceCaseDto.releaseDate.isTodayOrInTheFuture() || it.cvlRecord.hardStopKind == LicenceKind.TIME_SERVED
  }

  private fun transformToCreateCaseload(cases: List<Case>): List<ComCreateCase> = cases.map {
    with(it.comLicenceCaseDto) {
      ComCreateCase(
        licenceId = licenceId,
        licenceStatus = licenceStatus,
        licenceType = licenceType,
        name = name,
        crnNumber = crn,
        prisonerNumber = nomisId,
        releaseDate = releaseDate,
        probationPractitioner = it.probationPractitioner,
        hardStopDate = it.cvlRecord.hardStopDate,
        hardStopWarningDate = it.cvlRecord.hardStopWarningDate,
        kind = kind,
        licenceCreationType = licenceCreationType,
        isReviewNeeded = isReviewNeeded,
        isLao = isLao,
      )
    }
  }.sortedWith(compareBy<ComCreateCase> { it.releaseDate }.thenBy { it.name })

  private fun getCaseAccessRecords(crns: List<String>): Map<String, CaseAccessResponse> {
    val username = SecurityContextHolder.getContext().authentication.name
    return deliusApiClient.getCheckUserAccess(username, crns).associateBy { it.crn }
  }

  private fun createCaseForRelevantLicence(probationPractitioner: ProbationPractitioner, nomisRecord: PrisonerSearchPrisoner, cvlRecord: CvlRecord, createCaseloadLicence: ComCreateCaseloadLicenceDto, isLao: Boolean): Case = if (isLao) {
    Case(
      probationPractitioner,
      nomisRecord,
      cvlRecord,
      ComCreateCaseloadLicenceDto.restrictedCase(
        createCaseloadLicence.licenceStatus,
        createCaseloadLicence.kind,
        cvlRecord.licenceType,
        createCaseloadLicence.crn,
        nomisRecord.prisonerNumber,
        cvlRecord.licenceStartDate,
      ),
    )
  } else {
    Case(probationPractitioner, nomisRecord, cvlRecord, createCaseloadLicence)
  }

  private data class Case(
    val probationPractitioner: ProbationPractitioner,
    val nomisRecord: PrisonerSearchPrisoner,
    val cvlRecord: CvlRecord,
    val comLicenceCaseDto: ComCreateCaseloadLicenceDto,
  )
}
