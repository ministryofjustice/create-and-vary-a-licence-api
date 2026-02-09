package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Describes a licence within this service")
data class SarLicence(

  @field:Schema(description = "Kind of licence", example = "CRD")
  val kind: String,

  @field:Schema(description = "The licence type code", example = "All purpose")
  val typeCode: SarLicenceType = SarLicenceType.AP,

  @field:Schema(description = "The current status code for this licence", example = "In progress")
  val statusCode: SarLicenceStatus?,

  @field:Schema(description = "The prison identifier for the person on this licence", example = "A9999AA")
  val prisonNumber: String?,

  @field:Schema(description = "Who the person will meet at their initial appointment", example = "Duty officer")
  val appointmentPerson: String?,

  @field:Schema(description = "The date and time of the initial appointment", example = "23/08/2022 12:12")
  @field:JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val appointmentTime: LocalDateTime?,

  @field:Schema(description = "The type of appointment time of the initial appointment", example = "Specific date time")
  val appointmentTimeType: SarAppointmentTimeType?,

  @field:Schema(
    description = "The address of initial appointment",
    example = "Manchester Probation Service, Unit 4, Smith Street, Stockport, SP1 3DN",
  )
  val appointmentAddress: String?,

  @Deprecated("Use appointmentTelephoneNumber instead")
  @get:Schema(
    description = "The UK telephone number to contact the person the offender should meet for their initial meeting",
    example = "0114 2557665",
  )
  val appointmentContact: String?,

  @get:Schema(
    description = "The UK telephone number to contact the person the offender should meet for their initial meeting",
    example = "0114 2557665",
  )
  val appointmentTelephoneNumber: String?,

  @get:Schema(
    description = "An alternative UK telephone number to contact the person the offender should meet for their initial meeting",
    example = "07700 900000",
  )
  val appointmentAlternativeTelephoneNumber: String?,

  @field:Schema(
    description = "The date and time that this prison approved this licence",
    example = "24/08/2022 11:30:33",
  )
  @field:JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val approvedDate: LocalDateTime?,

  @field:Schema(
    description = "The date and time that this licence was submitted for approval",
    example = "24/08/2022 11:30:33",
  )
  @field:JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val submittedDate: LocalDateTime?,

  @field:Schema(
    description = "The full name of the person who approved the licence on behalf of the prison governor",
    example = "John Smith",
  )
  val approvedByName: String?,

  @field:Schema(
    description = "The date and time that this licence was superseded by a new variant",
    example = "24/08/2022 11:30:33",
  )
  @field:JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val supersededDate: LocalDateTime?,

  @field:Schema(description = "The date and time that this licence was first created", example = "24/08/2022 09:30:33")
  @field:JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val dateCreated: LocalDateTime?,

  @field:Schema(description = "The date and time that this licence was last updated", example = "24/08/2022 09:30:33")
  @field:JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val dateLastUpdated: LocalDateTime?,

  @field:Schema(description = "The list of standard licence conditions on this licence")
  val standardLicenceConditions: List<SarStandardCondition>?,

  @field:Schema(description = "The list of standard post sentence supervision conditions on this licence")
  val standardPssConditions: List<SarStandardCondition>?,

  @field:Schema(description = "The list of additional licence conditions on this licence")
  val additionalLicenceConditions: List<SarAdditionalCondition> = emptyList(),

  @field:Schema(description = "The list of additional post sentence supervision conditions on this licence")
  val additionalPssConditions: List<SarAdditionalCondition> = emptyList(),

  @field:Schema(description = "The list of bespoke conditions on this licence")
  val bespokeConditions: List<String> = emptyList(),

  @field:Schema(description = "The full name of the person who created licence or variation", example = "Test Person")
  val createdByFullName: String?,

  @field:Schema(description = "The version number of this licence", example = "1.3")
  val licenceVersion: String?,

  @field:Schema(description = "Is the licence to be tagged for electronic monitoring programme")
  val isToBeTaggedForProgramme: Boolean? = null,

  @field:Schema(description = "Programme Name of the licence", example = "Off Some Road")
  val programmeName: String? = null,
)
