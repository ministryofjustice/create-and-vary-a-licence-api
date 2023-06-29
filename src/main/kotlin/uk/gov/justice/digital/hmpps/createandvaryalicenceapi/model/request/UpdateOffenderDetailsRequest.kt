package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

@Schema(description = "Request object for updating an offender's personal details")
data class UpdateOffenderDetailsRequest(
  @Schema(description = "The offender forename", example = "Steven")
  @NotNull
  val forename: String? = null,

  @Schema(description = "The offender middle names", example = "Jason Kyle")
  val middleNames: String? = null,

  @Schema(description = "The offender surname", example = "Smith")
  @NotNull
  val surname: String? = null,

  @Schema(description = "The offender's date of birth, from either prison or probation services", example = "12/12/2001")
  @NotNull
  @JsonFormat(pattern = "dd/MM/yyyy")
  val dateOfBirth: LocalDate? = null
)
