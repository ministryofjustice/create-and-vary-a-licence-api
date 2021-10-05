package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

@Schema(description = "Response object which summarises a licence")
data class LicenceSummary(
  @Schema(description = "Internal identifier for this licence generated within this service", example = "123344")
  val licenceId: Long,

  @Schema(description = "Licence type code", example = "AP")
  val licenceType: LicenceType,

  @Schema(description = "The status of this licence", example = "IN_PROGRESS")
  val licenceStatus: LicenceStatus,

  @Schema(description = "The prison nomis identifier for this offender", example = "A1234AA")
  val nomisId: String?,

  @Schema(description = "The offender surname", example = "Smith")
  val surname: String?,

  @Schema(description = "The case reference number (CRN) of this person, from either prison or probation service", example = "X12344")
  val crn: String?,

  @Schema(description = "The offender's date of birth, from either prison or probation services", example = "12/12/2001")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val dateOfBirth: LocalDate?
)
