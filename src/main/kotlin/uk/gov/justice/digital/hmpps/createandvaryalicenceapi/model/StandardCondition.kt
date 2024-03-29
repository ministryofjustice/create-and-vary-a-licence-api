package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonView
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes a standard condition on this licence")
data class StandardCondition(

  @Schema(description = "The internal ID for this standard condition on this licence", example = "98987")
  val id: Long? = null,

  @Schema(description = "The unique code for this standard condition", example = "9ce9d594-e346-4785-9642-c87e764bee37")
  @get:JsonView(Views.SubjectAccessRequest::class)
  val code: String? = null,

  @Schema(description = "The sequence of this standard condition", example = "1")
  val sequence: Int? = null,

  @Schema(description = "The text of this standard condition", example = "Be of generally good behaviour")
  @get:JsonView(Views.SubjectAccessRequest::class)
  val text: String? = null,
)
