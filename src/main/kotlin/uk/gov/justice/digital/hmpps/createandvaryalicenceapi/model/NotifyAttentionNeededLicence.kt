package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Describes a licence that needs attention for support")
data class NotifyAttentionNeededLicence(
  @Schema(description = "The prison nomis identifier for this offender", example = "A1234AA")
  val nomsId: String?,

  @Schema(description = "Prison Name", example = "HMP Leeds")
  var prisonName: String? = null,

  @Schema(
    description = "Legal Status",
    example = "SENTENCED",
    allowableValues = ["RECALL", "DEAD", "INDETERMINATE_SENTENCE", "SENTENCED", "CONVICTED_UNSENTENCED", "CIVIL_PRISONER", "IMMIGRATION_DETAINEE", "REMAND", "UNKNOWN", "OTHER"],
  )
  var legalStatus: String? = null,

  @Schema(description = "The conditional release date on the licence", example = "12/12/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val conditionalReleaseDate: LocalDate?,

  @Schema(description = "The actual release date on the licence", example = "12/12/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val actualReleaseDate: LocalDate?,

  @Schema(description = "The date that the licence will start", example = "13/09/2022")
  val licenceStartDate: LocalDate?,
)
