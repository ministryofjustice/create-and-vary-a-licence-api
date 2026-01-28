package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.reports

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.UpcomingReleasesWithMonitoringConditionsResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.ReportRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Service
class UpcomingReleasesWithMonitoringConditionsReportService(
  private val reportRepository: ReportRepository,
) {
  fun getUpcomingReleasesWithMonitoringConditions(): List<UpcomingReleasesWithMonitoringConditionsResponse> = reportRepository.getUpcomingReleasesWithMonitoringConditions().map {
    UpcomingReleasesWithMonitoringConditionsResponse(
      crn = it.crn,
      prisonNumber = it.prisonNumber,
      status = LicenceStatus.valueOf(it.status),
      licenceStartDate = it.licenceStartDate?.toLocalDate(),
      emConditionCodes = it.emConditionCodes,
      fullName = it.fullName,
    )
  }
}
