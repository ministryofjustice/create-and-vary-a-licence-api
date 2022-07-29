package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

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

  @Schema(description = "The offender forename", example = "Brian")
  val forename: String?,

  @Schema(description = "The prison code where this offender resides or was released from", example = "MDI")
  val prisonCode: String?,

  @Schema(description = "The prison where this offender resides or was released from", example = "Moorland (HMP)")
  val prisonDescription: String?,

  @Schema(description = "The probation area code where the licence is supervised", example = "N01")
  val probationAreaCode: String?,

  @Schema(description = "The probation area description", example = "Wales")
  val probationAreaDescription: String? = null,

  @Schema(description = "The probation delivery unit (PDU or borough) where the licence is supervised", example = "N01CA")
  val probationPduCode: String?,

  @Schema(description = "The description for the PDU", example = "North Wales")
  val probationPduDescription: String? = null,

  @Schema(description = "The local administrative unit (LAU or district) where the licence is supervised", example = "NA01CA-02")
  val probationLauCode: String?,

  @Schema(description = "The LAU description", example = "North Wales")
  val probationLauDescription: String? = null,

  @Schema(description = "The probation team code which supervises the licence", example = "NA01CA-02-A")
  val probationTeamCode: String?,

  @Schema(description = "The team description", example = "Cardiff South")
  val probationTeamDescription: String? = null,

  @Schema(description = "The conditional release date on the licence", example = "12/12/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val conditionalReleaseDate: LocalDate?,

  @Schema(description = "The actual release date on the licence", example = "12/12/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val actualReleaseDate: LocalDate?,

  @Schema(description = "The case reference number (CRN) of this person, from either prison or probation service", example = "X12344")
  val crn: String?,

  @Schema(description = "The offender's date of birth, from either prison or probation services", example = "12/12/2001")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val dateOfBirth: LocalDate?,

  @Schema(description = "The username of the responsible probation officer", example = "jsmith")
  val comUsername: String?,

  @Schema(description = "The bookingId associated with the licence", example = "773722")
  val bookingId: Long?,

  @Schema(description = "The date the licence was created", example = "02/12/2001 10:15")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val dateCreated: LocalDateTime?,
)
