package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model


data class UnapprovedLicence(
  val crn: String? = null,
  val forename: String? = null,
  val surname: String? = null,
  val comFirstName: String? = null,
  val comLastName: String? = null,
  val comEmail: String? = null,
)
