package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.Fields

@Schema(description = "Describes the data entered for an additional condition")
data class AdditionalConditionData(
  @Schema(description = "The internal ID of this data item, for this condition on this licence", example = "98989")
  val id: Long = -1,

  @Schema(description = "The field name of this data item for this condition on this licence", example = "location")
  val field: String? = null,

  @Schema(description = "The value of this data item", example = "Norfolk")
  val value: String? = null,

  @Schema(description = "The sequence of this data item, for this condition on this licence", example = "1")
  val sequence: Int = -1,

) {
  @get:Schema(description = "Whether this data item contributes to the licence or whether it is just used for rendering purposes")
  val contributesToLicence: Boolean
    get() = !Fields.NON_CONTRIBUTING_FIELDS.contains(this.field)
}
