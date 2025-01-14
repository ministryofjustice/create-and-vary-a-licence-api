package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Describes the files uploaded for an additional condition")
data class SarAdditionalConditionUploadSummary(

  @Schema(
    description = "The original file name uploaded for this condition on this licence",
    example = "exclusion-zone.pdf",
  )
  val filename: String? = null,

  @Schema(description = "The file type uploaded for this condition on this licence", example = "application/pdf")
  val fileType: String? = null,

  @Schema(description = "The original file size in bytes", example = "27566")
  val fileSize: Int = 0,

  @Schema(description = "The date and time this file was uploaded", example = "12/12/2021 10:35")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val uploadedTime: LocalDateTime = LocalDateTime.now(),

  @Schema(
    description = "The description provided in this document",
    example = "A description of the exclusion zone boundaries",
  )
  val description: String? = null,

  @Schema(description = "The id which references the original file data and full size image", example = "9999")
  val uploadDetailId: Long,
)
