package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.AddressSource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.hdc.AccommodationType

@Schema(description = "Describes a curfew address on a HDC licence")
data class HdcCurfewAddress(

  @field:Schema(description = "The internal ID for this curfew address on this HDC licence", example = "98987")
  val id: Long? = null,

  @field:Schema(description = "The address's Unique Property Reference Number")
  val uprn: String? = null,

  @field:Schema(description = "The first line of the curfew address", example = "1 Some Street")
  val firstLine: String,

  @field:Schema(description = "The second line of the curfew address", example = "Off Some Road")
  val secondLine: String? = null,

  @field:Schema(description = "The town or city associated with the curfew address", example = "Some Town or City")
  val townOrCity: String,

  @field:Schema(description = "The county for the curfew address", example = "SomeCounty")
  val county: String? = null,

  @field:Schema(description = "The postcode for the curfew address", example = "SO30 2UH")
  val postcode: String,

  @field:Schema(description = "Source of the address", example = "MANUAL")
  val source: AddressSource,

  @field:Schema(description = "Accommodation types of the curfew address", example = "RESIDENTIAL")
  val accommodationType: AccommodationType? = null,

  @field:Schema(description = "Post release residential checks completed for this curfew address", example = "true")
  val postReleaseResidentialChecksCompleted: Boolean? = null,

  @field:Schema(description = "Reason for not completing post release residential checks", example = "some reason")
  val postReleaseResidentialChecksNotCompletedReason: String? = null,
)
