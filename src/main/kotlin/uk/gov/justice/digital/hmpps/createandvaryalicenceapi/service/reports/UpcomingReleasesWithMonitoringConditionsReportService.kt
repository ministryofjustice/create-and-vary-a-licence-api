package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.reports

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.UpcomingReleasesWithMonitoringConditionsResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.ReportRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.ElectronicMonitoringData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ElectronicMonitoringType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val BATCH_SIZE = 500
private val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH)

@Service
class UpcomingReleasesWithMonitoringConditionsReportService(
  private val reportRepository: ReportRepository,
) {

  fun getUpcomingReleasesWithMonitoringConditions(): List<UpcomingReleasesWithMonitoringConditionsResponse> {
    val releases = reportRepository.getUpcomingReleasesWithMonitoringConditions()

    val monitoringDataByLicence = releases
      .map { it.licenceId }
      .chunked(BATCH_SIZE)
      .flatMap(reportRepository::findElectronicMonitoringData)
      .groupBy { it.licenceId }

    return releases.map {
      val monitoringData = monitoringDataByLicence[it.licenceId].orEmpty()

      UpcomingReleasesWithMonitoringConditionsResponse(
        crn = it.crn,
        prisonNumber = it.prisonNumber,
        status = LicenceStatus.valueOf(it.status),
        licenceStartDate = it.licenceStartDate,
        submittedDate = it.submittedDate,
        emConditionCodes = it.emConditionCodes,
        electronicMonitoringTypes = getElectronicMonitoringTypes(monitoringData),
        emEndDate = getEmEndDate(monitoringData),
        fullName = it.fullName,
      )
    }
  }

  private fun getEmEndDate(monitoringData: List<ElectronicMonitoringData>): LocalDate? = monitoringData
    .find { it.type == "endDate" }
    ?.value
    ?.let { LocalDate.parse(it, formatter) }

  private fun getElectronicMonitoringTypes(monitoringData: List<ElectronicMonitoringData>): String = monitoringData
    .filter { it.type == "electronicMonitoringTypes" }
    .joinToString(",") {
      ElectronicMonitoringType.find(it.value).toString()
    }
}
