package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDateTime

@Schema(description = "Describes a licence within this service")
data class Licence(

  @Schema(description = "Unique identifier for this licence within the service", example = "99999")
  val id: Long = -1,

  @Schema(description = "The licence type", example = "STD | HDC")
  val type: LicenceType,

  @Schema(description = "The licence sub type", example = "AP | PSS | AP_PSS")
  val subType: LicenceSubType,

  @Schema(description = "The policy version number the licence is currently on", example = "2.1")
  val policyVersion: String,

  @Schema(description = "The version number used for standard and additional conditions", example = "1.4")
  val version: String,

  @Schema(description = "The current status code for the licence", example = "IN_PROGRESS")
  val statusCode: LicenceStatus,

  @Schema(description = "The prison identifier for the person on the licence", example = "A9999AA")
  val nomsId: String,

  @Schema(description = "The prison internal booking ID for the person on the licence", example = "989898")
  val bookingId: Long,

  @Schema(description = "The case reference number (CRN) for the person on the licence", example = "X12444")
  val crn: String,

  @Schema(
    description = "The username of the person who approved the licence on behalf of the prison governor",
    example = "X33221",
  )
  val approvedByUsername: String,

  @Schema(description = "The date and time that the prison approved the licence", example = "24/08/2022 11:30:33")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val approvedDate: LocalDateTime,

  @Schema(description = "The username of the person who created the licence", example = "X12333")
  val createdByUsername: String,

  @Schema(description = "The date and time that the licence was first created", example = "24/08/2022 09:30:33")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val createdDate: LocalDateTime,

  @Schema(description = "The username of the person who last updated the licence", example = "X34433")
  val updatedByUsername: String,

  @Schema(description = "The date and time that the licence was last updated", example = "24/08/2022 09:30:33")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val updatedDate: LocalDateTime,

  @Schema(description = "Whether the licence in PSS period?(LED < TODAY <= TUSED)")
  val isInPssPeriod: Boolean,

  @Schema(description = "The initial appointment details for the licence")
  val appointment: Appointment,

  @Schema(description = "The AP and PSS conditions that form the licence")
  val conditions: ConditionType,
)
