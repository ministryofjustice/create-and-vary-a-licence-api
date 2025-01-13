package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDateTime

@Schema(description = "Describes a licence within this service")
data class Licence(
  @Schema(description = "Unique identifier for this licence within the service", example = "99999")
  val id: Long = -1,

  @Schema(description = "The licence type code", example = "AP")
  val typeCode: LicenceType = LicenceType.AP,

  @get:Schema(description = "The current status code for this licence", example = "IN_PROGRESS")
  val statusCode: LicenceStatus?,

  @get:Schema(description = "The prison identifier for the person on this licence", example = "A9999AA")
  val nomsId: String?,

  @get:Schema(description = "The prison internal booking ID for the person on this licence", example = "989898")
  val bookingId: Long?,

  @get:Schema(description = "Who the person will meet at their initial appointment", example = "Duty officer")
  val appointmentPerson: String?,

  @get:Schema(description = "The date and time of the initial appointment", example = "23/08/2022 12:12")
  val appointmentTime: LocalDateTime?,

  @get:Schema(description = "The type of appointment time of the initial appointment", example = "SPECIFIC_DATE_TIME")
  val appointmentTimeType: AppointmentTimeType?,

  @get:Schema(description = "The address of initial appointment", example = "Manchester Probation Service, Unit 4, Smith Street, Stockport, SP1 3DN")
  val appointmentAddress: String?,

  @get:Schema(description = "The UK telephone number to contact the person the offender should meet for their initial meeting", example = "0114 2557665")
  val appointmentContact: String?,

  @get:Schema(description = "The date and time that this prison approved this licence", example = "24/08/2022 11:30:33")
  val approvedDate: LocalDateTime?,

  @get:Schema(description = "The username who approved the licence on behalf of the prison governor", example = "X33221")
  val approvedByUsername: String?,

  @get:Schema(description = "The date and time that this licence was submitted for approval", example = "24/08/2022 11:30:33")
  val submittedDate: LocalDateTime?,

  @get:Schema(description = "The full name of the person who approved the licence on behalf of the prison governor", example = "John Smith")
  val approvedByName: String?,

  @get:Schema(description = "The date and time that this licence was superseded by a new variant", example = "24/08/2022 11:30:33")
  val supersededDate: LocalDateTime?,

  @get:Schema(description = "The date and time that this licence was first created", example = "24/08/2022 09:30:33")
  val dateCreated: LocalDateTime?,

  @get:Schema(description = "The username which created this licence", example = "X12333")
  val createdByUsername: String?,

  @get:Schema(description = "The date and time that this licence was last updated", example = "24/08/2022 09:30:33")
  val dateLastUpdated: LocalDateTime?,

  @get:Schema(description = "The username of the person who last updated this licence", example = "X34433")
  val updatedByUsername: String?,

  @get:Schema(description = "The list of standard licence conditions on this licence")
  val standardLicenceConditions: List<StandardCondition>?,

  @get:Schema(description = "The list of standard post sentence supervision conditions on this licence")
  val standardPssConditions: List<StandardCondition>?,

  @get:Schema(description = "The list of additional licence conditions on this licence")
  val additionalLicenceConditions: List<AdditionalCondition> = emptyList(),

  @get:Schema(description = "The list of additional post sentence supervision conditions on this licence")
  val additionalPssConditions: List<AdditionalCondition> = emptyList(),

  @get:Schema(description = "The list of bespoke conditions on this licence")
  val bespokeConditions: List<BespokeCondition> = emptyList(),

  @get:Schema(description = "The full name of the person who created licence or variation", example = "Gordon Sumner")
  val createdByFullName: String?,

  @get:Schema(description = "The version number of this licence", example = "1.3")
  val licenceVersion: String?,
)
