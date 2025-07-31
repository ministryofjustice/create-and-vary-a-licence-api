package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes a curfew address on a HDC licence")
data class HdcCurfewAddress(

  @field:Schema(description = "The internal ID for this curfew address on this HDC licence", example = "98987")
  val id: Long? = null,

  @field:Schema(description = "The first line of the curfew address", example = "1 Some Street")
  val addressLine1: String? = null,

  @field:Schema(description = "The second line of the curfew address", example = "Off Some Road")
  val addressLine2: String? = null,

  @field:Schema(description = "The town or city associated with the curfew address", example = "Some Town or City")
  val townOrCity: String? = null,

  @field:Schema(description = "The county for the curfew address", example = "SomeCounty")
  val county: String? = null,

  @field:Schema(description = "The postcode for the curfew address", example = "SO30 2UH")
  val postcode: String? = null,
)
