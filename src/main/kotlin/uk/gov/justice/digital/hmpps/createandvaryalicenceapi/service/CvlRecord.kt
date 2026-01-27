package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

data class CvlRecord(
  val nomisId: String,
  val licenceStartDate: LocalDate? = null,
  val isEligible: Boolean = false,
  val eligibleKind: LicenceKind? = null,
  val ineligibilityReasons: List<String> = emptyList(),
  val isDueToBeReleasedInTheNextTwoWorkingDays: Boolean,
  val isEligibleForEarlyRelease: Boolean,

  val hardStopWarningDate: LocalDate? = null,
  val hardStopDate: LocalDate? = null,
  val isInHardStopPeriod: Boolean,
  val hardStopKind: LicenceKind? = null,
  val licenceType: LicenceType,
)
