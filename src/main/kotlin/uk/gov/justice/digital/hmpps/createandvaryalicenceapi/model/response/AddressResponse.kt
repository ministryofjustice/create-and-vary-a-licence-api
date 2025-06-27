package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.AddressSource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.Country

@Schema(description = "A response object for a address")
data class AddressResponse(
  @Schema(description = "The address's unique reference", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479 or 10023122431", required = true)
  val reference: String,

  @Schema(description = "The first line of the address", example = "12 Cardiff Road", required = true)
  val firstLine: String,

  @Schema(description = "The second line of the address", example = "Penarth", required = false)
  val secondLine: String? = null,

  @Schema(description = "The town or city of the address", example = "Cardiff", required = true)
  val townOrCity: String,

  @Schema(description = "The county of the address", example = "Vale of Glamorgan", required = false)
  val county: String? = null,

  @Schema(description = "The postcode of the address", example = "CF64 1AB", required = true)
  val postcode: String,

  @Schema(example = "WALES", description = "Country (e.g. ENGLAND, SCOTLAND, WALES, NORTHERN_IRELAND)", required = false)
  val country: Country? = null,

  @Schema(example = "MANUAL", description = "Source of the address")
  val source: AddressSource,

)
