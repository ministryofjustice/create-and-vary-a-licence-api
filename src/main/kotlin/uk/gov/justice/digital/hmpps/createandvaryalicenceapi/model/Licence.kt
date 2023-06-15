package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Describes a licence within this service")
data class Licence(

  @Schema(description = "Unique identifier for this licence within the service", example = "99999")
  val id: Long = -1,

  @Schema(description = "The licence type code", example = "AP")
  val typeCode: LicenceType,

  @Schema(description = "The version number used for standard and additional conditions", example = "1.4")
  val version: String? = null,

  @Schema(
    description = "The current status code for this licence",
    example = "IN_PROGRESS",
  )
  val statusCode: LicenceStatus?,

  @Schema(description = "The prison identifier for the person on this licence", example = "A9999AA")
  val nomsId: String? = null,

  @Schema(description = "The prison booking number for the person on this licence", example = "F12333")
  val bookingNo: String? = null,

  @Schema(description = "The prison internal booking ID for the person on this licence", example = "989898")
  val bookingId: Long? = null,

  @Schema(description = "The case reference number (CRN) for the person on this licence", example = "X12444")
  val crn: String? = null,

  @Schema(
    description = "The police national computer number (PNC) for the person on this licence",
    example = "2015/12444",
  )
  val pnc: String? = null,

  @Schema(
    description = "The criminal records office number (CRO) for the person on this licence",
    example = "A/12444",
  )
  val cro: String? = null,

  @Schema(description = "The agency code of the detaining prison", example = "LEI")
  val prisonCode: String? = null,

  @Schema(description = "The agency description of the detaining prison", example = "Leeds (HMP)")
  val prisonDescription: String? = null,

  @Schema(description = "The telephone number to contact the prison", example = "0161 234 4747")
  val prisonTelephone: String? = null,

  @Schema(description = "The first name of the person on licence", example = "Michael")
  val forename: String? = null,

  @Schema(description = "The middle names of the person on licence", example = "John Peter")
  val middleNames: String? = null,

  @Schema(description = "The family name of the person on licence", example = "Smith")
  val surname: String? = null,

  @Schema(description = "The date of birth of the person on licence", example = "12/05/1987")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val dateOfBirth: LocalDate? = null,

  @Schema(description = "The earliest conditional release date of the person on licence", example = "13/08/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val conditionalReleaseDate: LocalDate? = null,

  @Schema(description = "The actual release date (if set)", example = "13/09/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val actualReleaseDate: LocalDate? = null,

  @Schema(description = "The sentence start date", example = "13/09/2019")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val sentenceStartDate: LocalDate? = null,

  @Schema(description = "The sentence end date", example = "13/09/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val sentenceEndDate: LocalDate? = null,

  @Schema(description = "The date that the licence will start", example = "13/09/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val licenceStartDate: LocalDate? = null,

  @Schema(description = "The date that the licence will expire", example = "13/09/2024")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val licenceExpiryDate: LocalDate? = null,

  @Schema(description = "The date when the post sentence supervision period starts, from prison services", example = "06/05/2023")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val topupSupervisionStartDate: LocalDate? = null,

  @Schema(description = "The date when the post sentence supervision period ends, from prison services", example = "06/06/2023")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val topupSupervisionExpiryDate: LocalDate? = null,

  @Schema(description = "The nDELIUS user name for the supervising probation officer", example = "X32122")
  val comUsername: String? = null,

  @Schema(description = "The nDELIUS staff identifier for the supervising probation officer", example = "12345")
  val comStaffId: Long? = null,

  @Schema(description = "The email address for the supervising probation officer", example = "jane.jones@nps.gov.uk")
  val comEmail: String? = null,

  @Schema(description = "The probation area code where this licence is supervised from", example = "N01")
  val probationAreaCode: String? = null,

  @Schema(description = "The probation area description", example = "Wales")
  val probationAreaDescription: String? = null,

  @Schema(description = "The Probation Delivery Unit (PDU or borough) supervising this licence", example = "PDU01")
  val probationPduCode: String? = null,

  @Schema(description = "The description for the PDU", example = "North Wales")
  val probationPduDescription: String? = null,

  @Schema(description = "The Local Administrative Unit (LAU or district) supervising this licence", example = "LAU01")
  val probationLauCode: String? = null,

  @Schema(description = "The LAU description", example = "North Wales")
  val probationLauDescription: String? = null,

  @Schema(description = "The team code that is supervising this licence", example = "Cardiff-A")
  val probationTeamCode: String? = null,

  @Schema(description = "The team description", example = "Cardiff South")
  val probationTeamDescription: String? = null,

  @Schema(description = "Who the person will meet at their initial appointment", example = "Duty officer")
  val appointmentPerson: String? = null,

  @Schema(description = "The date and time of the initial appointment", example = "23/08/2022 12:12")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val appointmentTime: LocalDateTime? = null,

  @Schema(
    description = "The address of initial appointment",
    example = "Manchester Probation Service, Unit 4, Smith Street, Stockport, SP1 3DN",
  )
  val appointmentAddress: String? = null,

  @Schema(description = "The UK telephone number to contact the person the offender should meet for their initial meeting", example = "0114 2557665")
  val appointmentContact: String? = null,

  @Schema(description = "Have you have discussed this variation request with your SPO?", example = "Yes")
  val spoDiscussion: String? = null,

  @Schema(description = "Have you consulted with the victim liaison officer (VLO) for this case?", example = "Yes")
  val vloDiscussion: String? = null,

  @Schema(description = "The date and time that this prison approved this licence", example = "24/08/2022 11:30:33")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val approvedDate: LocalDateTime? = null,

  @Schema(description = "The username who approved the licence on behalf of the prison governor", example = "X33221")
  val approvedByUsername: String? = null,

  @Schema(description = "The full name of the person who approved the licence on behalf of the prison governor", example = "John Smith")
  val approvedByName: String? = null,

  @Schema(
    description = "The date and time that this licence was superseded by a new variant",
    example = "24/08/2022 11:30:33",
  )
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val supersededDate: LocalDateTime? = null,

  @Schema(description = "The date and time that this licence was first created", example = "24/08/2022 09:30:33")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val dateCreated: LocalDateTime? = null,

  @Schema(description = "The username which created this licence", example = "X12333")
  val createdByUsername: String? = null,

  @Schema(description = "The date and time that this licence was last updated", example = "24/08/2022 09:30:33")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val dateLastUpdated: LocalDateTime? = null,

  @Schema(description = "The username of the person who last updated this licence", example = "X34433")
  val updatedByUsername: String? = null,

  @Schema(description = "The list of standard licence conditions on this licence")
  val standardLicenceConditions: List<StandardCondition>? = emptyList(),

  @Schema(description = "The list of standard post sentence supervision conditions on this licence")
  val standardPssConditions: List<StandardCondition>? = emptyList(),

  @Schema(description = "The list of additional licence conditions on this licence")
  val additionalLicenceConditions: List<AdditionalCondition> = emptyList(),

  @Schema(description = "The list of additional post sentence supervision conditions on this licence")
  val additionalPssConditions: List<AdditionalCondition> = emptyList(),

  @Schema(description = "The list of bespoke conditions on this licence")
  val bespokeConditions: List<BespokeCondition> = emptyList(),

  @Schema(description = "Is this licence a variation of another licence?")
  val isVariation: Boolean,

  @Schema(description = "The licence Id which this licence is a variation of")
  val variationOf: Long? = null,

  @Schema(description = "The full name of the person who created licence or variation", example = "Gordon Sumner")
  val createdByFullName: String? = null,

  @Schema(description = "Is this licence in PSS period?", example = "(LED < TODAY <= TUSED)")
  val isInPssPeriod: Boolean? = false,
)
