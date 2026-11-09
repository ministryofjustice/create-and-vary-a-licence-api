package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersionPostFix
import java.time.DayOfWeek
import java.time.LocalTime

@Schema(name = "PublicHdcLicence$latestApiVersionPostFix", description = "HDC licence details, $latestApiVersion")
class PublicHdcLicence(
  licence: Licence,

  @field:Schema(description = "The curfew address for this HDC licence")
  val curfewAddress: PublicHdcCurfewAddress?,

  @field:Schema(description = "The first night curfew times for this HDC licence")
  val firstNightCurfewTimes: PublicCurfewTimes?,

  @field:Schema(description = "The weekly curfew times for this HDC licence, ordered by sequence")
  val curfewTimes: List<PublicCurfewTimes>,
) : Licence(
  id = licence.id,
  kind = licence.kind,
  licenceType = licence.licenceType,
  policyVersion = licence.policyVersion,
  version = licence.version,
  statusCode = licence.statusCode,
  prisonNumber = licence.prisonNumber,
  bookingId = licence.bookingId,
  crn = licence.crn,
  approvedByUsername = licence.approvedByUsername,
  approvedDateTime = licence.approvedDateTime,
  createdByUsername = licence.createdByUsername,
  createdDateTime = licence.createdDateTime,
  updatedByUsername = licence.updatedByUsername,
  updatedDateTime = licence.updatedDateTime,
  licenceStartDate = licence.licenceStartDate,
  isInPssPeriod = licence.isInPssPeriod,
  conditions = licence.conditions,
)

@Schema(name = "PublicHdcCurfewAddress$latestApiVersionPostFix", description = "Curfew address, $latestApiVersion")
data class PublicHdcCurfewAddress(
  @field:Schema(description = "The address's Unique Property Reference Number")
  val uprn: String?,

  @field:Schema(description = "The first line of the curfew address", example = "1 Some Street")
  val firstLine: String,

  @field:Schema(description = "The second line of the curfew address", example = "Off Some Road")
  val secondLine: String?,

  @field:Schema(description = "The town or city associated with the curfew address", example = "Some Town or City")
  val townOrCity: String,

  @field:Schema(description = "The county for the curfew address", example = "Some County")
  val county: String?,

  @field:Schema(description = "The postcode for the curfew address", example = "SO30 2UH")
  val postcode: String,
)

@Schema(name = "PublicCurfewTimes$latestApiVersionPostFix", description = "Curfew time, $latestApiVersion")
data class PublicCurfewTimes(
  @field:Schema(description = "The day on which this curfew starts", example = "MONDAY", implementation = DayOfWeek::class)
  val fromDay: DayOfWeek?,

  @field:Schema(description = "The time at which this curfew starts", example = "19:00:00")
  val fromTime: LocalTime?,

  @field:Schema(description = "The day on which this curfew ends", example = "TUESDAY", implementation = DayOfWeek::class)
  val untilDay: DayOfWeek?,

  @field:Schema(description = "The time at which this curfew ends", example = "07:00:00")
  val untilTime: LocalTime?,
)
