package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

data class LicenceCaCase(
  override val kind: LicenceKind,
  override val licenceId: Long,
  override val versionOfId: Long?,
  override val licenceStatus: LicenceStatus,
  override val prisonNumber: String,
  override val surname: String?,
  override val forename: String?,
  override val conditionalReleaseDate: LocalDate?,
  override val actualReleaseDate: LocalDate?,
  override val licenceStartDate: LocalDate?,
  override val postRecallReleaseDate: LocalDate?,
  override val homeDetentionCurfewActualDate: LocalDate?,
  override val updatedByFirstName: String?,
  override val updatedByLastName: String?,
  override val comUsername: String?,
  val prisonCode: String?,
  val prisonDescription: String?,
) : LicenceCase
