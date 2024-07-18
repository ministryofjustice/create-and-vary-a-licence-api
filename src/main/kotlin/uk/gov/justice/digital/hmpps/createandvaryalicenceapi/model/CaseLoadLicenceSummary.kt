package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

// TODO : add schema stuff
// TODO : can we use licency summary?
data class CaseLoadLicenceSummary(
  val licenceId: Long? = null,
  val licenceStatus: LicenceStatus,
  val kind: LicenceKind? = null,
  val licenceType: LicenceType,
  val comUsername: String? = null,
  val dateCreated: LocalDateTime? = null,
  val approvedBy: String? = null,
  val approvedDate: LocalDateTime? = null,
  val versionOf: Long? = null,
  val updatedByFullName: String? = null,
  val hardStopWarningDate: LocalDate? = null,
  val hardStopDate: LocalDate? = null,
  val licenceStartDate: LocalDate? = null,
  val releaseDate: LocalDate? = null,
  val isDueToBeReleasedInTheNextTwoWorkingDays: Boolean,
  val isReviewNeeded: Boolean? = null,
)
