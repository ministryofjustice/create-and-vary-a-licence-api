package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.PolicyVersion
import java.time.LocalDateTime

@Schema(description = "Describes a licence summary within this service")
data class LicenceSummary(

  @Schema(description = "Unique identifier for this licence within the service", example = "99999")
  val id: Long = -1,

  @Schema(
    description = "The type of conditions on a licence policy which can be AP (All Purpose) and/or PSS " +
      "(Post Sentence Supervision)",
    example = "AP",
  )
  val licenceType: LicenceType,

  @Schema(
    description = "The policy version number the licence is currently on",
    example = "V2_1",
  )
  val policyVersion: PolicyVersion,

  @Schema(
    description = "The version of this specific licence, this is unique within the context of a booking",
    example = "1.4",
  )
  val version: String,

  @Schema(
    description = "The current status code for the licence. This is non exhaustive and subject to change",
    example = "IN_PROGRESS",
  )
  val statusCode: LicenceStatus,

  @Schema(description = "The prison identifier for the person on the licence. Also known as the NOMIS ID", example = "A1234AA")
  val prisonNumber: String,

  @Schema(description = "The prison internal booking ID for the person on the licence", example = "989898")
  val bookingId: Long,

  @Schema(description = "The Delius case reference number (CRN) for the person on the licence", example = "X12444")
  val crn: String,

  @Schema(
    description = "The username of the person who approved the licence (PDU head or governor)",
    example = "X33221",
  )
  val approvedByUsername: String?,

  @Schema(
    description = "The date and time that the prison approved the licence, where licences approved before 01/04/2023 " +
      "will not have an approved time",
    example = "2023-11-20T00:00:00Z",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val approvedDateTime: LocalDateTime?,

  @Schema(description = "The username of the person who created the licence", example = "X12333")
  val createdByUsername: String,

  @Schema(description = "The date and time when the version/variation of the licence was created at", example = "2023-11-20T00:00:00Z")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val createdDateTime: LocalDateTime,

  @Schema(description = "The username of the person who last updated the licence", example = "X34433")
  val updatedByUsername: String?,

  @Schema(description = "The date and time that the licence was last updated", example = "2023-11-20T00:00:00Z")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val updatedDateTime: LocalDateTime?,

  @Schema(description = "Whether the licence in PSS period? This is when Licence End Date < TODAY <= TUSED (Top Up Supervision End Date)")
  val isInPssPeriod: Boolean,

)
