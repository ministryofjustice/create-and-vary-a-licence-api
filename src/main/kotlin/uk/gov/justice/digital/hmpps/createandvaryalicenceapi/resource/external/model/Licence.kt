package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.external.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
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

  @Schema(description = "The prison identifier for the person on this licence", example = "A1234AA")
  val prisonNumber: String? = null,

  @Schema(description = "The prison internal booking ID for the person on this licence", example = "989898")
  val bookingId: Long? = null,

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
)
