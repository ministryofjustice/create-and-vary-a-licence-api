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

@Service
class LicenceStatisticsService(
  private val prisonRegisterApiClient: PrisonRegisterApiClient,
  private val licenceService: LicenceService,
) {

  fun getStatistics(startDate: LocalDate, endDate: LocalDate): List<LicenceStatistics> {
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
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<LicenceSummary> {
    return prisonLicences.filter { (startDate <= it.conditionalReleaseDate) && (it.conditionalReleaseDate!! <= endDate) }
  }

  private fun filterUniquePrisonsOfCrdScopedLicences(crdScopedLicences: List<LicenceSummary>): List<String> {
    return crdScopedLicences.map { it.prisonCode!! }.distinctBy { it }
  }

  private fun licenceStatsForEachPrison(
    prisons: List<String>,
    prisonLicences: List<LicenceSummary>,
  ): List<LicenceStatistics> {
    return prisons.map { it to prisonLicences.getForPrison(it) }.flatMap { extractStatsForPrison(it.first, it.second) }
  }

  fun List<LicenceSummary>.getForPrison(prisonId: String): List<LicenceSummary> {
    return this.filter { it.prisonCode == prisonId }
  }

  private fun extractStatsForPrison(prisonId: String, licenceSummaries: List<LicenceSummary>): List<LicenceStatistics> {
    return listOf(
      extractStatsForType(prisonId, LicenceType.AP, licenceSummaries),
      extractStatsForType(prisonId, LicenceType.PSS, licenceSummaries),
      extractStatsForType(prisonId, LicenceType.AP_PSS, licenceSummaries),
    )
  }

  private fun extractStatsForType(
    prisonId: String,
    licenceType: LicenceType,
    licenceSummaries: List<LicenceSummary>,
  ): LicenceStatistics {
    val filteredLicenceSummaries = licenceSummaries.filter { it.licenceType == licenceType }
    return LicenceStatistics(
      prison = prisonId,
      licenceType = licenceType.toString(),
      inProgress = filteredLicenceSummaries.count { it.licenceStatus == LicenceStatus.IN_PROGRESS },
      submitted = filteredLicenceSummaries.count { it.licenceStatus == LicenceStatus.SUBMITTED },
      approved = filteredLicenceSummaries.count { it.licenceStatus == LicenceStatus.APPROVED },
      active = filteredLicenceSummaries.count { it.licenceStatus == LicenceStatus.ACTIVE },
    )
  }
}
