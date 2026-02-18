package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Details about case access restrictions for a probation staff member")
data class CaseAccessDetails(
  @field:Schema(description = "The type of access restriction", example = "RESTRICTED", required = true)
  val type: CaseAccessRestrictionType,

  @field:Schema(description = "Additional details that have been entered in NDelius", example = "User is restricted from viewing this case", required = false)
  val message: String? = null,
)

@Schema(description = "Type of case access restriction")
enum class CaseAccessRestrictionType {
  @Schema(description = "The user is not on the allowlist for this case")
  RESTRICTED,

  @Schema(description = "The user has been excluded from accessing this case")
  EXCLUDED,

  @Schema(description = "No access restrictions apply")
  NONE,
}
