package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

@Schema(description = "Describes a vary approver case")
data class VaryApproverCase(
  @Schema(description = "Unique identifier for this licence within the service", example = "99999")
  val licenceId: Long?,

  @Schema(description = "The full name of the person on licence", example = "An offender")
  val name: String?,

  @Schema(description = "The case reference number (CRN) for the person on this licence", example = "X12444")
  val crnNumber: String,

  @Schema(description = "The licence type code", example = "AP")
  val licenceType: LicenceType?,

  @Schema(description = "The date on which the licence variation was created", example = "30/11/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val variationRequestDate: LocalDate?,

  @Schema(description = "The date on which the prisoner leaves custody", example = "30/11/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val releaseDate: LocalDate?,

  @Schema(description = "The details for the active supervising probation officer")
  val probationPractitioner: String?,
)
