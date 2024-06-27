package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Describes a probation practitioner on an approval case")
data class ProbationPractitioner(

  @Schema(description = "The unique staff code for the probation practitioner", example = "SH00001")
  val staffCode: String? = null,

  @Schema(description = "The full name of the probation practitioner", example = "Joe Bloggs")
  @field:NotBlank
  val name: String,

  @Schema(description = "Probation staffIdentifier in nDelius", example = "120003434")
  val staffIdentifier: Long? = null,

  @Schema(description = "The NOMIS username of the case administrator", example = "jbloggs")
  val staffUsername: String? = null,
)
