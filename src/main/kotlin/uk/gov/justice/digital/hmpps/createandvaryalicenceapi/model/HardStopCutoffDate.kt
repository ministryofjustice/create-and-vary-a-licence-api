package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "cutoff date for licence to be timed out")
data class HardStopCutoffDate(
  @Schema(description = "cutoff date for licence to be timed out", example = "05/12/2023")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val cutoffDate: LocalDate,
)
