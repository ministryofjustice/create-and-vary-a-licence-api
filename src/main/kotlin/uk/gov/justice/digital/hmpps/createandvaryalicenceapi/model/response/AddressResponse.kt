package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.AddressSource

@Schema(description = "A response object for a address")
data class AddressResponse(
  @field:Schema(
    description = "The address's unique reference",
    example = "f47ac10b-58cc-4372-a567-0e02b2c3d479 or 10023122431",
    required = true,
  )
  val reference: String,

  @field:Schema(
    description = "Unique Property Reference Number, acquired from OsPlacesApi, post code and address look up",
    example = "200010019924",
    required = false,
  )
  @field:Size(min = 1, max = 12)
  val uprn: String? = null,

  @field:Schema(description = "The first line of the address", example = "12 Cardiff Road", required = true)
  val firstLine: String,

  @field:Schema(description = "The second line of the address", example = "Penarth", required = false)
  val secondLine: String? = null,

  @field:Schema(description = "The town or city of the address", example = "Cardiff", required = true)
  val townOrCity: String,

  @field:Schema(description = "The county of the address", example = "Vale of Glamorgan", required = false)
  val county: String? = null,

  @field:Schema(description = "The postcode of the address", example = "CF64 1AB", required = true)
  val postcode: String,

  @field:Schema(example = "MANUAL", description = "Source of the address")
  val source: AddressSource,

)
