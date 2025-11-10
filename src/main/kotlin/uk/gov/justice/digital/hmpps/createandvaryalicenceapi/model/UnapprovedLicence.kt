package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes a prisoner's first and last name, their CRN if present and a COM's contact details for use in an email to COM")
data class UnapprovedLicence(
  @field:Schema(description = "The Crime Reference Number", example = "Z882661")
  val crn: String? = null,

  @field:Schema(description = "The prisoner's first name", example = "Jim")
  val forename: String? = null,

  @field:Schema(description = "The prisoner's last name", example = "Smith")
  val surname: String? = null,

  @field:Schema(description = "The COM's first name", example = "Joseph")
  val comFirstName: String? = null,

  @field:Schema(description = "The COM's last name", example = "Bloggs")
  val comLastName: String? = null,

  @field:Schema(description = "The COM's email address", example = "jbloggs@probation.gov.uk")
  val comEmail: String? = null,
)
