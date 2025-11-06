package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model

data class LicenceSubmitName(
  val licenceId: Long,
  private val firstName: String?,
  private val lastName: String?,
) {
  val fullName: String
    get() = firstName?.let { "$it $lastName" } ?: ""
}
