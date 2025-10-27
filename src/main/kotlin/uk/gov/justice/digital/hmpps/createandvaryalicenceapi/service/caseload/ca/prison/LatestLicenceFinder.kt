package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT

object LatestLicenceFinder {
  fun findLatestLicenceSummary(licences: List<LicenceSummary>) = when {
    licences.size == 1 -> licences[0]
    licences.any { it.licenceStatus == TIMED_OUT } -> licences.find { it.licenceStatus != TIMED_OUT }
    else -> licences.find { (it.licenceStatus == SUBMITTED) || (it.licenceStatus == IN_PROGRESS) }
  }
}
