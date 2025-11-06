package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

// internal representation of a licence summary for COM create caseload
data class ComCreateCaseloadLicenceDto(
  val licenceId: Long? = null,
  val licenceStatus: LicenceStatus,
  val kind: LicenceKind,
  val licenceType: LicenceType,
  val crn: String?,
  val nomisId: String,
  val name: String,
  val versionOf: Long? = null,
  val licenceStartDate: LocalDate? = null,
  val releaseDate: LocalDate? = null,
  val isReviewNeeded: Boolean = false,
  val licenceCreationType: LicenceCreationType? = null,
)
