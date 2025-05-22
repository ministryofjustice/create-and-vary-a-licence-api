package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import java.time.LocalDate

@Schema(description = "Request object for overriding prisoner details")
data class OverrideLicencePrisonerDetailsRequest(

  @Schema(description = "The prisoner's forename", example = "John")
  @NotEmpty
  val forename: String,

  @Schema(description = "The prisoner's middle names", example = "James Micheal")
  val middleNames: String? = null,

  @Schema(description = "The prisoner's surname", example = "Smith")
  @NotEmpty
  val surname: String,

  @Schema(description = "The prisoner's date of birth", example = "21/01/1995")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val dateOfBirth: LocalDate,

  @Schema(description = "Reason for overriding the prisoner details")
  @NotEmpty
  val reason: String,
)
