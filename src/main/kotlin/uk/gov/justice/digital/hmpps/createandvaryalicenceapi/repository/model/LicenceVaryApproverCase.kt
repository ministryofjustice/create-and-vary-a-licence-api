package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

class LicenceVaryApproverCase(
  val crn: String,
  var typeCode: LicenceType?,
  var dateCreated: LocalDateTime?,
  override val kind: LicenceKind,
  override val licenceId: Long,
  override val versionOfId: Long?,
  override val statusCode: LicenceStatus,
  override val surname: String?,
  override val forename: String?,
  override val updatedByFirstName: String?,
  override val updatedByLastName: String?,
  override val sentenceStartDate: LocalDate?,
  override val conditionalReleaseDate: LocalDate?,
  override val actualReleaseDate: LocalDate?,
  override val postRecallReleaseDate: LocalDate?,
  override val licenceStartDate: LocalDate?,
  override val prisonNumber: String?,
  override val comUsername: String?,
  override val homeDetentionCurfewActualDate: LocalDate?,
  override val homeDetentionCurfewEligibilityDate: LocalDate?,
): LicenceCase
