package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response object which describes an result from a address search")
data class AddressSearchResponse(
  @field:Schema(
    description = "The address's Unique Property Reference Number",
    example = "200010019924",
    required = true,
  )
  val uprn: String,

  @field:Schema(description = "The address's first line", example = "34 Maryport Street", required = true)
  val firstLine: String,

  @field:Schema(description = "The address's second line", example = "Urchfont")
  val secondLine: String? = null,

  @field:Schema(description = "The address's Town or City", example = "Chippenham", required = true)
  val townOrCity: String,

  @field:Schema(description = "The address's county", example = "Shropshire", required = true)
  val county: String,

  @field:Schema(description = "The address's postcode", example = "RG13HS", required = true)
  val postcode: String,

  @field:Schema(description = "The address's country", example = "Wales", required = true)
  val country: String,
)
