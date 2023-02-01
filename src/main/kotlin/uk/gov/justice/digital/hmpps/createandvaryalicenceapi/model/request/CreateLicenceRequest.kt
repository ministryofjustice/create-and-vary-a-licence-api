package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

@Schema(description = "Request object for creating a new licence")
data class CreateLicenceRequest(
  @Schema(description = "Type of licence requested - one of AP, PSS or AP_PSS", example = "AP")
  @NotNull
  val typeCode: LicenceType,

  @Schema(description = "The version of licence conditions currently active as a string value", example = "1.0")
  @NotNull
  val version: String,

  @Schema(description = "The prison nomis identifier for this offender", example = "A1234AA")
  @NotNull
  val nomsId: String? = null,

  @Schema(description = "The prison booking number for the current sentence for this offender", example = "12334")
  @NotNull
  val bookingNo: String? = null,

  @Schema(description = "The prison booking id for the current sentence for this offender", example = "87666")
  @NotNull
  val bookingId: Long? = null,

  @Schema(description = "The case reference number (CRN) of this person, from either prison or probation service", example = "X12344")
  @NotNull
  val crn: String? = null,

  @Schema(description = "The police national computer number (PNC) of this person, from either prison or probation service", example = "2014/12344A")
  val pnc: String? = null,

  @Schema(description = "The criminal records office (CRO) identifier police of this person, from either prison or probation service", example = "2014/12344A")
  val cro: String? = null,

  @Schema(description = "The prison location code where this person is currently resident - leave null if not in prison", example = "MDI")
  @NotNull
  val prisonCode: String? = null,

  @Schema(description = "The prison description - leave null if not in prison", example = "Leeds (HMP)")
  @NotNull
  val prisonDescription: String? = null,

  @Schema(description = "The prison telephone number - leave null if not in prison", example = "+44 276 54545")
  val prisonTelephone: String? = null,

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
  val dateOfBirth: LocalDate? = null,

  @Schema(description = "The conditional release date, from prison services", example = "18/06/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val conditionalReleaseDate: LocalDate? = null,

  @Schema(description = "The actual release date, from prison services", example = "18/07/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val actualReleaseDate: LocalDate? = null,

  @Schema(description = "The sentence start date, from prison services", example = "06/05/2019")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val sentenceStartDate: LocalDate? = null,

  @Schema(description = "The sentence end date, from prison services", example = "06/05/2023")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val sentenceEndDate: LocalDate? = null,

  @Schema(description = "The licence start date, from prison services", example = "06/05/2021")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val licenceStartDate: LocalDate? = null,

  @Schema(description = "The licence end date, from prison services", example = "06/05/2023")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val licenceExpiryDate: LocalDate? = null,

  @Schema(description = "The date when the post sentence supervision period starts, from prison services", example = "06/05/2023")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val topupSupervisionStartDate: LocalDate? = null,

  @Schema(description = "The date when the post sentence supervision period ends, from prison services", example = "06/06/2023")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val topupSupervisionExpiryDate: LocalDate? = null,

  @Schema(description = "The probation area code supervising this licence", example = "N01")
  @NotNull
  val probationAreaCode: String? = null,

  @Schema(description = "The probation area description", example = "Wales")
  val probationAreaDescription: String? = null,

  @Schema(description = "The probation delivery unit (PDU or borough) code", example = "NA01A12")
  val probationPduCode: String? = null,

  @Schema(description = "The PDU description", example = "Cardiff")
  val probationPduDescription: String? = null,

  @Schema(description = "The local administrative unit (LAU or district) code", example = "NA01A12")
  val probationLauCode: String? = null,

  @Schema(description = "The LAU description", example = "Cardiff North")
  val probationLauDescription: String? = null,

  @Schema(description = "The probation team code supervising this licence", example = "NA01A12-A")
  val probationTeamCode: String? = null,

  @Schema(description = "The team description", example = "Cardiff North A")
  val probationTeamDescription: String? = null,

  @Schema(description = "The list of standard licence conditions from service configuration")
  val standardLicenceConditions: List<StandardCondition> = emptyList(),

  @Schema(description = "The list of standard post sentence supervision conditions from service configuration")
  val standardPssConditions: List<StandardCondition> = emptyList(),

  @Schema(description = "The community offender manager who is responsible for this case", example = "1231332")
  @field:NotNull
  val responsibleComStaffId: Long,
)
