package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.AddressSource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.hdc.AccommodationType
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(description = "Information about HDC")
data class SarHdcInfo(
  @field:Schema(description = "Information about the HDC curfew address")
  val curfewAddress: SarHdcCurfewAddress?,
  @field:Schema(description = "Information about curfew hours for the first night")
  val firstNight: SarFirstNight?,
  @field:Schema(description = "Information about curfew hours across the week")
  val curfewTimes: List<SarCurfewTimes>?,
)

@Schema(description = "Information about the HDC curfew address")
data class SarHdcCurfewAddress(
  @field:Schema(description = "the type of HDC curfew address", example = "Residential accommodation")
  val accommodationType: SarAccommodationType?,
  @field:Schema(
    description = "whether post release checks have been completed, only asked if address changes as part of a variations",
    example = "true",
  )
  val postReleaseResidentialChecksCompleted: Boolean?,
  @field:Schema(
    description = "the reason that post release checks have not been completed",
    example = "Checks are in progress",
  )
  val postReleaseResidentialChecksNotCompletedReason: String?,
  @field:Schema(description = "the address UPRN if provided by OS Places")
  val uprn: String?,
  @field:Schema(description = "the first line of the HDC curfew address")
  val firstLine: String,
  @field:Schema(description = "the second line of the HDC curfew address")
  val secondLine: String?,
  @field:Schema(description = "the town or city of the HDC curfew address")
  val townOrCity: String,
  @field:Schema(description = "the county of the HDC curfew address")
  val county: String?,
  @field:Schema(description = "the postcode of the HDC curfew address")
  val postcode: String?,
  @field:Schema(
    description = "the source of the HDC curfew address, either from OS Places or manually entered",
    example = "Manual",
  )
  val source: SarAddressSource?,
  @field:JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  @field:Schema(description = "When the curfew address was created")
  val createdTimestamp: LocalDateTime?,
  @field:JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  @field:Schema(description = "When the curfew address was last updated")
  val lastUpdatedTimestamp: LocalDateTime?,
)

@Schema(description = "Information about a specific days curfew time for a HDC licence")
data class SarCurfewTimes(
  @field:Schema(description = "the sequence of the curfew times (representing order in the week)")
  val curfewTimesSequence: Int?,
  @field:Schema(description = "the day the curfew starts")
  val fromDay: DayOfWeek?,
  @field:Schema(description = "the time the curfew starts")
  val fromTime: LocalTime?,
  @field:Schema(description = "the day the curfew ends")
  val untilDay: DayOfWeek?,
  @field:Schema(description = "the time the curfew ends")
  val untilTime: LocalTime?,
  @field:Schema(description = "the date and time the curfew was created")
  @field:JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val createdTimestamp: LocalDateTime?,
)

@Schema(description = "Information about the first night curfew times for a HDC licence")
data class SarFirstNight(
  @field:Schema(description = "the time the curfew starts")
  val firstNightFrom: LocalTime?,
  @field:Schema(description = "the time the curfew end")
  val firstNightUntil: LocalTime?,
  @field:Schema(description = "the date and time the curfew time was created")
  @field:JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val createdTimestamp: LocalDateTime?,
)

enum class SarAddressSource(@JsonValue val description: String) {
  MANUAL("Manual"),
  OS_PLACES("From OS Places"),
  MANUAL_MIGRATED("Manually migrated"),
  ;

  companion object {
    fun from(type: AddressSource) = when (type) {
      AddressSource.MANUAL -> MANUAL
      AddressSource.MANUAL_MIGRATED -> MANUAL_MIGRATED
      AddressSource.OS_PLACES -> OS_PLACES
    }
  }
}

enum class SarAccommodationType(@JsonValue val description: String) {
  CAS("Community accommodation services provided accommodation"),
  RESIDENTIAL("Residential accommodation"),
  ;

  companion object {
    fun from(type: AccommodationType) = when (type) {
      AccommodationType.CAS -> CAS
      AccommodationType.RESIDENTIAL -> RESIDENTIAL
    }
  }
}
