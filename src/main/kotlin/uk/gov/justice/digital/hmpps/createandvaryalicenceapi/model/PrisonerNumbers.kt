package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class PrisonerNumbers(
  @Schema(description = "List of prisoner numbers to search by", example = "[\"A1234AA\"]")
  @NotEmpty
  @Size(min = 1, max = 1000)
  val prisonerNumbers: List<String>,
)
