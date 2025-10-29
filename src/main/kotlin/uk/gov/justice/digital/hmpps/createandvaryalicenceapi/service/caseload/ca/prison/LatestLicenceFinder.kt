package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceCaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT

object LatestLicenceFinder {
  fun findLatestLicenceCases(licenceCaCases: List<LicenceCaCase>) = when {
    licenceCaCases.size == 1 -> licenceCaCases[0]
    licenceCaCases.any { it.statusCode == TIMED_OUT } -> licenceCaCases.find { it.statusCode != TIMED_OUT }
    else -> licenceCaCases.find { (it.statusCode == SUBMITTED) || (it.statusCode == IN_PROGRESS) }
  }
}
