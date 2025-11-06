package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate
import java.time.LocalDateTime

class LicenceApproverCase(
  override val licenceStartDate: LocalDate?,
  override val kind: LicenceKind,
  override val licenceId: Long,
  override val versionOfId: Long?,
  override val statusCode: LicenceStatus,
  override val prisonNumber: String?,
  override val surname: String?,
  override val forename: String?,
  override val updatedByFirstName: String?,
  override val updatedByLastName: String?,
  override val comUsername: String?,
  override val conditionalReleaseDate: LocalDate?,
  override val actualReleaseDate: LocalDate?,
  override val postRecallReleaseDate: LocalDate?,
  override val sentenceStartDate: LocalDate?,
  val approvedByName: String?,
  val approvedDate: LocalDateTime?,
  val prisonCode: String?,
  val prisonDescription: String?,
  var variationOfId: Long?,
) : LicenceCase {
  var submittedByFullName: String? = null
}
