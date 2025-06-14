package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Describes a licence within this service1")
data class SarLicence(

  @Schema(description = "Unique identifier for this licence within the service", example = "99999")
  val id: Long = -1,

  @Schema(description = "Kind of licence", example = "CRD")
  val kind: String,

  @Schema(description = "The licence type code", example = "All purpose")
  val typeCode: SarLicenceType = SarLicenceType.AP,

  @Schema(description = "The current status code for this licence", example = "In progress")
  val statusCode: SarLicenceStatus?,

  @Schema(description = "The prison identifier for the person on this licence", example = "A9999AA")
  val nomsId: String?,

  @Schema(description = "The prison internal booking ID for the person on this licence", example = "989898")
  val bookingId: Long?,

  @Schema(description = "Who the person will meet at their initial appointment", example = "Duty officer")
  val appointmentPerson: String?,

  @Schema(description = "The date and time of the initial appointment", example = "23/08/2022 12:12")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val appointmentTime: LocalDateTime?,

  @Schema(description = "The type of appointment time of the initial appointment", example = "Specific date time")
  val appointmentTimeType: SarAppointmentTimeType?,

  @Schema(description = "The address of initial appointment", example = "Manchester Probation Service, Unit 4, Smith Street, Stockport, SP1 3DN")
  val appointmentAddress: String?,

  @Schema(description = "The UK telephone number to contact the person the offender should meet for their initial meeting", example = "0114 2557665")
  val appointmentContact: String?,

  @Schema(description = "The date and time that this prison approved this licence", example = "24/08/2022 11:30:33")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val approvedDate: LocalDateTime?,

  @Schema(description = "The username who approved the licence on behalf of the prison governor", example = "X33221")
  val approvedByUsername: String?,

  @Schema(description = "The date and time that this licence was submitted for approval", example = "24/08/2022 11:30:33")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val submittedDate: LocalDateTime?,

  @Schema(description = "The full name of the person who approved the licence on behalf of the prison governor", example = "John Smith")
  val approvedByName: String?,

  @Schema(description = "The date and time that this licence was superseded by a new variant", example = "24/08/2022 11:30:33")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val supersededDate: LocalDateTime?,

  @Schema(description = "The date and time that this licence was first created", example = "24/08/2022 09:30:33")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val dateCreated: LocalDateTime?,

  @Schema(description = "The username which created this licence", example = "X12333")
  val createdByUsername: String?,

  @Schema(description = "The date and time that this licence was last updated", example = "24/08/2022 09:30:33")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val dateLastUpdated: LocalDateTime?,

  @Schema(description = "The username of the person who last updated this licence", example = "X34433")
  val updatedByUsername: String?,

  @Schema(description = "The list of standard licence conditions on this licence")
  val standardLicenceConditions: List<SarStandardCondition>?,

  @Schema(description = "The list of standard post sentence supervision conditions on this licence")
  val standardPssConditions: List<SarStandardCondition>?,

  @Schema(description = "The list of additional licence conditions on this licence")
  val additionalLicenceConditions: List<SarAdditionalCondition> = emptyList(),

  @Schema(description = "The list of additional post sentence supervision conditions on this licence")
  val additionalPssConditions: List<SarAdditionalCondition> = emptyList(),

  @Schema(description = "The list of bespoke conditions on this licence")
  val bespokeConditions: List<String> = emptyList(),

  @Schema(description = "The full name of the person who created licence or variation", example = "Test Person")
  val createdByFullName: String?,

  @Schema(description = "The version number of this licence", example = "1.3")
  val licenceVersion: String?,
)
