package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Request object for updating sentence dates")
data class UpdateSentenceDatesRequest(
  @Schema(description = "The conditional release date, from prison services", example = "18/06/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val conditionalReleaseDate: LocalDate? = null,

  @Schema(description = "The actual release date, from prison services", example = "18/07/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val actualReleaseDate: LocalDate? = null,

  @Schema(description = "The sentence start date, from prison services", example = "06/05/2019")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val sentenceStartDate: LocalDate? = null,

  @Schema(description = "The sentence end date, from prison services", example = "06/05/2023")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val sentenceEndDate: LocalDate? = null,

  @Schema(description = "The licence start date, from prison services", example = "06/05/2021")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val licenceStartDate: LocalDate? = null,

  @Schema(description = "The licence end date, from prison services", example = "06/05/2023")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val licenceExpiryDate: LocalDate? = null,

  @Schema(
    description = "The date when the post sentence supervision period starts, from prison services",
    example = "06/05/2023",
  )
  @JsonFormat(pattern = "dd/MM/yyyy")
  val topupSupervisionStartDate: LocalDate? = null,

  @Schema(
    description = "The date when the post sentence supervision period ends, from prison services",
    example = "06/06/2023",
  )
  @JsonFormat(pattern = "dd/MM/yyyy")
  val topupSupervisionExpiryDate: LocalDate? = null,

  @Schema(
    description = "The date when a person is recalled to prison, after being released on a license",
    example = "06/06/2023",
  )
  @JsonFormat(pattern = "dd/MM/yyyy")
  val postRecallReleaseDate: LocalDate? = null,

  @Schema(description = "The person's actual home detention curfew date", example = "06/06/2023")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val homeDetentionCurfewActualDate: LocalDate? = null,

  @Schema(description = "The person's home detention curfew end date", example = "06/06/2023")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val homeDetentionCurfewEndDate: LocalDate? = null,
)
