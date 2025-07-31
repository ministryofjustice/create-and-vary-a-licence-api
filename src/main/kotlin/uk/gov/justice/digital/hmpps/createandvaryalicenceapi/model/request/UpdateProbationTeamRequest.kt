package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Request object for updating an offender's probation team")
data class UpdateProbationTeamRequest(
  @field:Schema(description = "The probation area code supervising this licence", example = "N01")
  @param:NotNull
  val probationAreaCode: String? = null,

  @field:Schema(description = "The probation area description", example = "Wales")
  val probationAreaDescription: String? = null,

  @field:Schema(description = "The probation delivery unit (PDU or borough) code", example = "NA01A12")
  val probationPduCode: String? = null,

  @field:Schema(description = "The PDU description", example = "Cardiff")
  val probationPduDescription: String? = null,

  @field:Schema(description = "The local administrative unit (LAU or district) code", example = "NA01A12")
  val probationLauCode: String? = null,

  @field:Schema(description = "The LAU description", example = "Cardiff North")
  val probationLauDescription: String? = null,

  @field:Schema(description = "The probation team code supervising this licence", example = "NA01A12-A")
  val probationTeamCode: String? = null,

  @field:Schema(description = "The team description", example = "Cardiff North A")
  val probationTeamDescription: String? = null,
)
