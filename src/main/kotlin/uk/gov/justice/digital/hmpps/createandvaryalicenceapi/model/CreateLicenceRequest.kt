package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

import javax.validation.constraints.NotNull

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

  @Schema(description = "The forename of the offender manager, from probation services", example = "Paula")
  val comFirstName: String? = null,

  @Schema(description = "The surname of the offender manager, from probation services", example = "Wells")
  val comLastName: String? = null,

  @Schema(description = "The username used in login for the person creating this licence", example = "X1233")
  @NotNull
  val comUsername: String? = null,

  @Schema(description = "The staff identifier of the offender manager, from probation services", example = "44553343")
  @NotNull
  val comStaffId: Long? = null,

  @Schema(description = "The email address of the offender manager, from probation services", example = "paula.wells@northeast.probation.gov.uk")
  val comEmail: String? = null,

  @Schema(description = "The telephone contact number for the offender manager, from probation services", example = "07876 443554")
  val comTelephone: String? = null,

  @Schema(description = "The probation area code where the offender manager is based, from probation services", example = "N01")
  @NotNull
  val probationAreaCode: String? = null,

  @Schema(description = "The local delivery unit code where the offender manager works, from probation services", example = "LDU1332")
  @NotNull
  val probationLduCode: String? = null,

  @Schema(description = "The list of standard conditions which apply to all licences, from service configuration", example = "List of conditions")
  @NotNull
  val standardConditions: List<String>
)
