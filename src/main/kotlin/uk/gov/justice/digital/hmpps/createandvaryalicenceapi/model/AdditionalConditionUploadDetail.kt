package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes the file details uploaded for an additional condition")
data class AdditionalConditionUploadDetail(
  @Schema(description = "The internal ID of this upload detail for this condition on this licence", example = "98989")
  val id: Long = -1,

  @Schema(description = "The licence id to which this uploaded file belongs", example = "1234")
  val licenceId: Long,

  @Schema(description = "The id of the additional condition that this upload detail relates to", example = "1234")
  val additionalConditionId: Long,

  @Schema(description = "The raw data of the PDF file originally uploaded", example = "ByteArray")
  val originalData: ByteArray? = null,

  @Schema(description = "The raw data for the full sized image extracted from the PDF upload", example = "ByteArray")
  val fullSizeImage: ByteArray? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AdditionalConditionUploadDetail

    if (id != other.id) return false
    if (licenceId != other.licenceId) return false
    if (additionalConditionId != other.additionalConditionId) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + licenceId.hashCode()
    result = 31 * result + additionalConditionId.hashCode()
    return result
  }
}
