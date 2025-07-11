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
import kotlin.reflect.KClass

@AddressWithUprnMustBeFromOsPlaces
@Schema(description = "A request object to add an address")
data class AddAddressRequest(

  @Schema(
    description = "Unique Property Reference Number, acquired from OsPlacesApi, post code and address look up",
    example = "200010019924",
    required = false,
  )
  @field:Size(min = 1, max = 12)
  val uprn: String? = null,

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

  @field:NotNull(message = "Source must not be null")
  @Schema(example = "MANUAL", description = "Source of the address", required = true)
  val source: AddressSource,
) {

  override fun toString(): String = listOf(
    uprn.orEmpty(),
    firstLine,
    secondLine.orEmpty(),
    townOrCity,
    county.orEmpty(),
    postcode,
    source.name,
  ).joinToString(",")
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [AddressWithUprnMustBeFromOsPlacesValidator::class])
@MustBeDocumented
annotation class AddressWithUprnMustBeFromOsPlaces(
  val message: String = "Unique Property Reference Number must be provided only with source OS_PLACES",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)

class AddressWithUprnMustBeFromOsPlacesValidator : ConstraintValidator<AddressWithUprnMustBeFromOsPlaces, AddAddressRequest> {
  override fun isValid(value: AddAddressRequest?, context: ConstraintValidatorContext): Boolean = when (value?.source) {
    null -> true // Or false, depending on what you want
    AddressSource.OS_PLACES -> !value.uprn.isNullOrBlank()
    AddressSource.MANUAL -> value.uprn.isNullOrBlank()
  }
}
