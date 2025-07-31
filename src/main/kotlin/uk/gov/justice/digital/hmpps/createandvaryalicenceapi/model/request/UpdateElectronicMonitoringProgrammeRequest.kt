package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@ValidProgrammeName
@Schema(description = "Request for providing details about any electronic monitoring programme")
data class UpdateElectronicMonitoringProgrammeRequest(
  @field:Schema(description = "Is the licence to be tagged for electronic monitoring programme")
  val isToBeTaggedForProgramme: Boolean? = null,

  @field:Schema(description = "Programme Name of the licence", example = "Off Some Road")
  val programmeName: String? = null,
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ProgrammeNameValidator::class])
annotation class ValidProgrammeName(
  val message: String = "If 'isToBeTaggedForProgramme' is true, 'programmeName' must be provided",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)

class ProgrammeNameValidator : ConstraintValidator<ValidProgrammeName, UpdateElectronicMonitoringProgrammeRequest> {
  override fun isValid(
    value: UpdateElectronicMonitoringProgrammeRequest?,
    context: ConstraintValidatorContext,
  ): Boolean {
    if (value == null) return true
    return !(value.isToBeTaggedForProgramme == true && value.programmeName.isNullOrBlank())
  }
}
