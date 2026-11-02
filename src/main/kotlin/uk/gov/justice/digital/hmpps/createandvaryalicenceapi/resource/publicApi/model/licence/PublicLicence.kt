package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalTime

@Schema(description = "HDC licence details")
class PublicHdcLicence(
  licence: Licence,
  val curfewAddress: PublicHdcCurfewAddress?,
  val firstNightCurfewTimes: PublicCurfewTimes?,
  val curfewTimes: List<PublicCurfewTimes>,
) : Licence(
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

@Schema(description = "Curfew address")
data class PublicHdcCurfewAddress(
  val uprn: String?,
  val firstLine: String,
  val secondLine: String?,
  val townOrCity: String,
  val county: String?,
  val postcode: String,
  val accommodationType: String?,
  val postReleaseResidentialChecksCompleted: Boolean?,
  val postReleaseResidentialChecksNotCompletedReason: String?,
)

@Schema(description = "Curfew time")
data class PublicCurfewTimes(
  val fromDay: String?,
  val fromTime: LocalTime?,
  val untilDay: String?,
  val untilTime: LocalTime?,
)
