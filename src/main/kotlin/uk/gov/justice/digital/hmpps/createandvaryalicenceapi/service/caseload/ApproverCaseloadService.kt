package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummaryApproverView
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonApproverService

@Service
class ApproverCaseloadService(
  private val prisonApproverService: PrisonApproverService,
) {

  fun getApprovalNeeded(prisons: List<String>): List<LicenceSummaryApproverView> {
    val filteredPrisons = prisons.filterNot { it == "CADM" }
    log.info(filteredPrisons.toString())
    val licences = prisonApproverService.getLicencesForApproval(filteredPrisons)
    if (licences.isEmpty()) {
      return emptyList()
    }
    return licences
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
