package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceStatistics
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.Prison
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonRegisterApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class LicenceStatisticsService(
  private val prisonRegisterApiClient: PrisonRegisterApiClient,
  private val licenceService: LicenceService
) {

  fun getStatistics(startDate: String, endDate: String): List<LicenceStatistics> {
    val prisons = prisonRegisterApiClient.getPrisonIds().sortedBy { it.prisonId }
    val prisonLicences = getLicencesForPrison(prisons)
    val crdScopedLicences = filterLicencesWithCrdInScope(prisonLicences, startDate, endDate)
    val crdScopedPrisons = filterUniquePrisonsOfCrdScopedLicences(crdScopedLicences)
    return licenceStatsForEachPrison(crdScopedPrisons, crdScopedLicences)
  }

  private fun getLicencesForPrison(prisons: List<Prison>): List<LicenceSummary> {
    return licenceService.findLicencesMatchingCriteria(LicenceQueryObject(prisonCodes = prisons.map { it.prisonId }))
  }

  private fun filterLicencesWithCrdInScope(
    prisonLicences: List<LicenceSummary>,
    startDate: String,
    endDate: String
  ): List<LicenceSummary> {
    val dateFormatter = DateTimeFormatter.ofPattern("d/M/yyyy")
    val sDate = LocalDate.parse(startDate, dateFormatter)
    val eDate = LocalDate.parse(endDate, dateFormatter)
    return prisonLicences.filter { (sDate <= it.conditionalReleaseDate) && (it.conditionalReleaseDate!! <= eDate) }
  }

  private fun filterUniquePrisonsOfCrdScopedLicences(crdScopedLicences: List<LicenceSummary>): List<Prison> {
    return crdScopedLicences.map { it.prisonCode }.toSet().map { Prison(prisonId = it!!) }
  }

  private fun licenceStatsForEachPrison(
    prisons: List<Prison>,
    prisonLicences: List<LicenceSummary>
  ): List<LicenceStatistics> {
    val prisonGroups =
      prisons.map { it.prisonId to prisonLicences.filter { licence -> licence.prisonCode == it.prisonId } } // array of Pairs comprising prisonId with licences having a matching prisonCode
    return prisonGroups.flatMap { extractStatsForPrison(it.first, it.second) }
  }

  private fun extractStatsForPrison(prisonId: String, licenceSummaries: List<LicenceSummary>): List<LicenceStatistics> {
    return listOf(
      extractStatsForType(prisonId, LicenceType.AP, licenceSummaries),
      extractStatsForType(prisonId, LicenceType.PSS, licenceSummaries),
      extractStatsForType(prisonId, LicenceType.AP_PSS, licenceSummaries)
    )
  }

  private fun extractStatsForType(
    prisonId: String,
    licenceType: LicenceType,
    licenceSummaries: List<LicenceSummary>
  ): LicenceStatistics {
    val filteredLicenceSummaries = licenceSummaries.filter { it.licenceType == licenceType }
    return LicenceStatistics(
      prison = prisonId,
      licenceType = licenceType.toString(),
      inProgress = filteredLicenceSummaries.count { it.licenceStatus == LicenceStatus.IN_PROGRESS },
      submitted = filteredLicenceSummaries.count { it.licenceStatus == LicenceStatus.SUBMITTED },
      approved = filteredLicenceSummaries.count { it.licenceStatus == LicenceStatus.APPROVED },
      active = filteredLicenceSummaries.count { it.licenceStatus == LicenceStatus.ACTIVE }
    )
  }
}
