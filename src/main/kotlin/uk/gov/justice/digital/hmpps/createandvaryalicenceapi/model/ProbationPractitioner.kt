package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Describes a probation practitioner on an approval case")
data class ProbationPractitioner(
  @field:Schema(description = "The unique staff code for the probation practitioner", example = "SH00001")
  val staffCode: String? = null,

  @field:Schema(description = "The full name of the probation practitioner", example = "Joe Bloggs")
  @field:NotBlank
  val name: String? = null,

  @field:Schema(description = "Is a probation practitioner allocated?")
  val allocated: Boolean,
) {
  companion object {
    const val NOT_ALLOCATED = "Not allocated"
    val UNALLOCATED = ProbationPractitioner(
      null,
      NOT_ALLOCATED,
      false,
    )
    fun unallocated(staffCode: String?): ProbationPractitioner = ProbationPractitioner(staffCode, NOT_ALLOCATED, false)
    fun restrictedView(): ProbationPractitioner = ProbationPractitioner("Restricted", "Restricted", true)
  }
}
