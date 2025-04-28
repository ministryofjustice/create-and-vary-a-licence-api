package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

data class Name(
  val forename: String?,
  val middleName: String? = null,
  val surname: String?,
)

fun Name.fullName() = listOfNotNull(this.forename, this.middleName, this.surname).joinToString(" ")
