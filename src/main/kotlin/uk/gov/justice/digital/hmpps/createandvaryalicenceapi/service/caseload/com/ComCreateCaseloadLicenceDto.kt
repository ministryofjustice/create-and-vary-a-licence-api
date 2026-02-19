package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

// internal representation of a licence summary for COM create caseload
data class ComCreateCaseloadLicenceDto(
  val licenceId: Long?,
  val licenceStatus: LicenceStatus,
  val kind: LicenceKind,
  val licenceType: LicenceType,
  val crn: String?,
  val nomisId: String,
  val name: String,
  val versionOf: Long?,
  val releaseDate: LocalDate?,
  val isReviewNeeded: Boolean,
  val licenceCreationType: LicenceCreationType?,
  val isLao: Boolean,
) {

  companion object {
    fun restrictedCase(licenceStatus: LicenceStatus, kind: LicenceKind, licenceType: LicenceType, crn: String?, nomisId: String, releaseDate: LocalDate?) = ComCreateCaseloadLicenceDto(
      licenceId = null,
      licenceStatus = licenceStatus,
      kind = kind,
      licenceType = licenceType,
      crn = crn,
      nomisId = nomisId,
      name = "Access restricted on NDelius",
      versionOf = null,
      releaseDate = releaseDate,
      isReviewNeeded = false,
      licenceCreationType = null,
      isLao = true,
    )
  }
}
