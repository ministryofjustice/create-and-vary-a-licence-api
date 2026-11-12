package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersionPostFix
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.PolicyVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(
  name = "Licence$latestApiVersionPostFix",
  description = "Describes a licence within this service, $latestApiVersion",
)
open class Licence(
  @field:Schema(description = "Unique identifier for this licence within the service", example = "99999")
  val id: Long = -1,

  @field:Schema(description = "Kind of licence", example = "CRD")
  val kind: String,

  @field:Schema(description = "The licence type", example = "AP")
  val licenceType: LicenceType,

  @field:Schema(description = "The policy version number the licence is currently on", example = "V4_0")
  val policyVersion: PolicyVersion,

  @field:Schema(description = "The version of this licence within the context of a booking", example = "1.4")
  val version: String,

  @field:Schema(description = "The current status code, which is non exhaustive and subject to change", example = "IN_PROGRESS")
  val statusCode: LicenceStatus,

  @field:Schema(description = "The prison identifier for the person, also known as the NOMIS ID", example = "A1234AA")
  val prisonNumber: String,

  @field:Schema(description = "The prison internal booking ID for the person on the licence", example = "989898")
  val bookingId: Long,

  @field:Schema(description = "The Delius case reference number for the person on the licence", example = "X12444")
  val crn: String,

  @field:Schema(description = "The username of the person who approved the licence")
  val approvedByUsername: String?,

  @field:Schema(description = "The approval date and time; licences approved before 01/04/2023 may not have a time")
  @field:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val approvedDateTime: LocalDateTime?,

  @field:Schema(description = "The username of the person who created the licence")
  val createdByUsername: String,

  @field:Schema(description = "The date and time when the version or variation was created")
  @field:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val createdDateTime: LocalDateTime?,

  @field:Schema(description = "The username of the person who last updated the licence")
  val updatedByUsername: String?,

  @field:Schema(description = "The date and time that the licence was last updated")
  @field:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val updatedDateTime: LocalDateTime?,

  @field:Schema(
    description = "The date the licence is to become active. This may be incorrect for IS-91 cases and people on remand. Get in touch for details",
    example = "2022-08-24",
  )
  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val licenceStartDate: LocalDate?,

  @field:Schema(description = "Whether Licence End Date < TODAY <= TUSED (Top Up Supervision End Date)")
  val isInPssPeriod: Boolean,

  @field:Schema(description = "The AP and PSS conditions that form the licence")
  val conditions: Conditions,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as Licence
    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()
}
