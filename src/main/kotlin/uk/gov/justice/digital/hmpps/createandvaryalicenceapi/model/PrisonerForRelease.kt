package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Schema(description = "Describes a prisoner due for release")
data class PrisonerForRelease(
  @Schema(description = "The full name of the prisoner", example = "John Smith")
  @field:NotBlank
  val name: String,

  @Schema(description = "The date on which the prisoner leaves custody", example = "30/11/2022")
  @JsonFormat(pattern = "yyyy-MM-dd")
  @field:NotNull
  val releaseDate: LocalDate,
)
