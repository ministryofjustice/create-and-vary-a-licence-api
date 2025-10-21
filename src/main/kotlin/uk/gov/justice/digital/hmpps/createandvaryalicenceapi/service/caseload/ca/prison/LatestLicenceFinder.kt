package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT

object LatestLicenceFinder {
  fun findLatestLicenceCases(licenceCases: List<LicenceCase>) = when {
    licenceCases.size == 1 -> licenceCases[0]
    licenceCases.any { it.licenceStatus == TIMED_OUT } -> licenceCases.find { it.licenceStatus != TIMED_OUT }
    else -> licenceCases.find { (it.licenceStatus == SUBMITTED) || (it.licenceStatus == IN_PROGRESS) }
  }
}
