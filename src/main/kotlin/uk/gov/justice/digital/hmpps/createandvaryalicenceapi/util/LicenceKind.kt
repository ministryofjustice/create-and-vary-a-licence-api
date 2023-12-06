package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_IN_PROGRESS

enum class LicenceKind(val initialStatus: LicenceStatus) {
  CRD(IN_PROGRESS),
  VARIATION(VARIATION_IN_PROGRESS),
}
