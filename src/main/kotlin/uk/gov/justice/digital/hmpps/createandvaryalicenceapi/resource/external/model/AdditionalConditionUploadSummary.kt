package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.external.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Describes the files uploaded for an additional condition")
data class AdditionalConditionUploadSummary(
  @Schema(description = "The internal ID of this upload for this condition on this licence", example = "98989")
  val id: Long = -1,

  @Schema(description = "The original file name uploaded for this condition on this licence", example = "exclusion-zone.pdf")
  val filename: String? = null,

  @Schema(description = "The file type uploaded for this condition on this licence", example = "application/pdf")
  val fileType: String? = null,

  @Schema(description = "The original file size in bytes", example = "27566")
  val fileSize: Int = 0,

  @Schema(description = "The date and time this file was uploaded", example = "12/12/2021 10:35")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val uploadedTime: LocalDateTime = LocalDateTime.now(),

  @Schema(description = "The description provided in this document", example = "A description of the exclusion zone boundaries")
  val description: String? = null,

  @Schema(description = "The thumbnail for the  exclusion zone map as a base64-encoded JPEG image", example = "Base64 string")
  val thumbnailImage: String? = null,

  @Schema(description = "The id which references the original file data and full size image", example = "9999")
  val uploadDetailId: Long,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AdditionalConditionUploadSummary

    if (id != other.id) return false
    if (filename != other.filename) return false
    if (fileType != other.fileType) return false
    if (fileSize != other.fileSize) return false
    if (uploadedTime != other.uploadedTime) return false
    if (description != other.description) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()

    result = 31 * result + (fileType?.hashCode() ?: 0)
    result = 31 * result + (filename?.hashCode() ?: 0)
    result = 31 * result + fileSize
    result = 31 * result + uploadedTime.hashCode()
    result = 31 * result + (description?.hashCode() ?: 0)

    return result
  }
}
