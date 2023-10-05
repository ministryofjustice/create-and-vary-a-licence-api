package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes each instance of the same additional condition on the licence")
data class AdditionalConditionInstance(

  @Schema(description = "The ID of the instance", example = "123456")
  val id: Long,

  @Schema(description = "The inputted text for the instance", example = "You must not enter the location X")
  val text: String,

  @Schema(description = "Whether any data was uploaded when the instance was created", example = "true")
  val hasUpload: Boolean,

  @Schema(description = "The particular key words associated with this instance", example = "['ALCOHOL']")
  val variants: List<String>,
)
