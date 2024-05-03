package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import java.time.LocalDate

@Schema(description = "Request object for overriding licence dates")
data class OverrideLicenceDatesRequest(

  @Schema(description = "The conditional release date", example = "18/06/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val conditionalReleaseDate: LocalDate? = null,

  @Schema(description = "The actual release date", example = "18/07/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val actualReleaseDate: LocalDate? = null,

  @Schema(description = "The sentence start date", example = "06/05/2019")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val sentenceStartDate: LocalDate? = null,

  @Schema(description = "The sentence end date", example = "06/05/2023")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val sentenceEndDate: LocalDate? = null,

  @Schema(description = "The licence start date", example = "06/05/2021")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val licenceStartDate: LocalDate? = null,

  @Schema(description = "The licence expiry date", example = "06/05/2023")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val licenceExpiryDate: LocalDate? = null,

  @Schema(
    description = "The date when the post sentence supervision period starts",
    example = "06/05/2023",
  )
  @JsonFormat(pattern = "dd/MM/yyyy")
  val topupSupervisionStartDate: LocalDate? = null,

  @Schema(
    description = "The date when the post sentence supervision period ends",
    example = "06/06/2023",
  )
  @JsonFormat(pattern = "dd/MM/yyyy")
  val topupSupervisionExpiryDate: LocalDate? = null,

  @Schema(
    description = "The release date after being recalled",
    example = "06/06/2023",
  )
  @JsonFormat(pattern = "dd/MM/yyyy")
  val postRecallReleaseDate: LocalDate? = null,

  @Schema(description = "Reason for overriding the licence dates")
  @NotEmpty
  val reason: String,
)
