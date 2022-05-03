package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotNull

@Schema(description = "Request object for updating an offender's probation team")
data class UpdateProbationTeamRequest(
  @Schema(description = "The probation area code supervising this licence", example = "N01")
  @NotNull
  val probationAreaCode: String? = null,

  @Schema(description = "The probation area description", example = "Wales")
  val probationAreaDescription: String? = null,

  @Schema(description = "The probation delivery unit (PDU or borough) code", example = "NA01A12")
  val probationPduCode: String? = null,

  @Schema(description = "The PDU description", example = "Cardiff")
  val probationPduDescription: String? = null,

  @Schema(description = "The local administrative unit (LAU or district) code", example = "NA01A12")
  val probationLauCode: String? = null,

  @Schema(description = "The LAU description", example = "Cardiff North")
  val probationLauDescription: String? = null,

  @Schema(description = "The probation team code supervising this licence", example = "NA01A12-A")
  val probationTeamCode: String? = null,

  @Schema(description = "The team description", example = "Cardiff North A")
  val probationTeamDescription: String? = null,
)
