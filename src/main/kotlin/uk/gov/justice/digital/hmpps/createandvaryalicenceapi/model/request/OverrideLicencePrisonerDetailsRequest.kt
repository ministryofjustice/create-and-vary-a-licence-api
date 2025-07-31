package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import java.time.LocalDate

@Schema(description = "Request object for overriding prisoner details")
data class OverrideLicencePrisonerDetailsRequest(

  @field:Schema(description = "The prisoner's forename", example = "John")
  @field:NotEmpty
  val forename: String,

  @field:Schema(description = "The prisoner's middle names", example = "James Micheal")
  val middleNames: String? = null,

  @field:Schema(description = "The prisoner's surname", example = "Smith")
  @field:NotEmpty
  val surname: String,

  @field:Schema(description = "The prisoner's date of birth", example = "21/01/1995")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val dateOfBirth: LocalDate,

  @field:Schema(description = "Reason for overriding the prisoner details")
  @field:NotEmpty
  val reason: String,
)
