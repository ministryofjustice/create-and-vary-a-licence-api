package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcCurfewTimes
import kotlin.reflect.KClass

@ValidProgrammeName
@Schema(description = "Request for adding a electronic monitoring programme request")
data class ElectronicMonitoringProgrammeRequest(
  @Schema(description = "Is the licence to be tagged for electronic monitoring programme")
  val isToBeTaggedForProgramme: Boolean? = null,

  @Schema(description = "Programme Name of the licence", example = "Off Some Road")
  val programmeName: String? = null,
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ProgrammeNameValidator::class])
annotation class ValidProgrammeName(
  val message: String = "If 'isToBeTaggedForProgramme' is true, 'programmeName' must be provided",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = []
)

class ProgrammeNameValidator : ConstraintValidator<ValidProgrammeName, ElectronicMonitoringProgrammeRequest> {
  override fun isValid(
    value: ElectronicMonitoringProgrammeRequest?,
    context: ConstraintValidatorContext
  ): Boolean {
    if (value == null) return true
    return !(value.isToBeTaggedForProgramme == true && value.programmeName.isNullOrBlank())
  }
}
