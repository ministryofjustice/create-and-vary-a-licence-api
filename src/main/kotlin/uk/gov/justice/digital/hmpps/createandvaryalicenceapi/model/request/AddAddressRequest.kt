package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.AddressSource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.Country
import kotlin.reflect.KClass

@ReferenceRequiredForOsPlaces
@Schema(description = "A request object to add an address")
data class AddAddressRequest(

  @Schema(
    description = "The unique reference, e.g. OsPlacesApi uprn, or existing manual reference UUID , " +
      "if already exists the associated address will re-used, otherwise a new address entity will be created, if source is" +
      "OS_PLACES then a uprn reference must be given!",
    example = "200010019924",
    required = true,
  )
  @field:Size(min = 1, max = 36)
  val reference: String? = null,

  @field:NotBlank(message = "First line of the address must not be blank")
  @Schema(description = "The first line of the address", example = "12 Cardiff Road", required = true)
  val firstLine: String,

  @Schema(description = "The second line of the address", example = "Penarth", required = false)
  val secondLine: String? = null,

  @field:NotBlank(message = "Town or city must not be blank")
  @Schema(description = "The town or city of the address", example = "Cardiff", required = true)
  val townOrCity: String,

  @Schema(description = "The county of the address", example = "Vale of Glamorgan", required = false)
  val county: String? = null,

  @field:NotBlank(message = "Postcode must not be blank")
  @field:Pattern(
    regexp = "^[A-Z]{1,2}[0-9][0-9A-Z]? [0-9][A-Z]{2}\$",
    message = "Postcode must be in a valid UK format",
  )
  @Schema(description = "The postcode of the address", example = "CF64 1AB", required = true)
  val postcode: String,

  @field:NotNull(message = "Country must not be null")
  @Schema(example = "WALES", description = "Country (e.g. ENGLAND, SCOTLAND, WALES, NORTHERN_IRELAND)", required = true)
  val country: Country,

  @field:NotNull(message = "Source must not be null")
  @Schema(example = "MANUAL", description = "Source of the address", required = true)
  val source: AddressSource,
) {

  override fun toString(): String = listOf(
    reference,
    firstLine,
    secondLine.orEmpty(),
    townOrCity,
    county.orEmpty(),
    postcode,
    country.name,
    source.name,
  ).joinToString(",")
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ReferenceRequiredForOsPlacesValidator::class])
@MustBeDocumented
annotation class ReferenceRequiredForOsPlaces(
  val message: String = "Reference must be provided when source is OS_PLACES",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)

class ReferenceRequiredForOsPlacesValidator : ConstraintValidator<ReferenceRequiredForOsPlaces, AddAddressRequest> {
  override fun isValid(value: AddAddressRequest?, context: ConstraintValidatorContext): Boolean {
    if (value == null) return true

    if (value.source == AddressSource.OS_PLACES) {
      return !value.reference.isNullOrBlank()
    }
    return true
  }
}
