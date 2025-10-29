package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util.LicenceLike
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

interface LicenceCase :
  SentenceDateHolder,
  LicenceLike {
  override val kind: LicenceKind
  val licenceId: Long
  val versionOfId: Long?
  override val statusCode: LicenceStatus
  val prisonNumber: String?
  val surname: String?
  val forename: String?
  val updatedByFirstName: String?
  val updatedByLastName: String?
  val comUsername: String?

  val updatedByFullName: String?
    get() = listOfNotNull(updatedByFirstName, updatedByLastName)
      .joinToString(" ")
      .ifBlank { null }

  val fullName: String
    get() = listOfNotNull(forename, surname)
      .joinToString(" ")
      .convertToTitleCase()
      .trim()
}
