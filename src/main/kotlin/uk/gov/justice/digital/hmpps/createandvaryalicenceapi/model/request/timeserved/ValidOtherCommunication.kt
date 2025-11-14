package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.timeserved

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.timeserved.CommunicationMethod
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [OtherCommunicationDetailValidator::class])
annotation class ValidOtherCommunication(
  val message: String = "Enter a form of communication",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)

class OtherCommunicationDetailValidator : ConstraintValidator<ValidOtherCommunication, TimeServedProbationConfirmContactRequest> {

  override fun isValid(
    value: TimeServedProbationConfirmContactRequest,
    context: ConstraintValidatorContext,
  ): Boolean = if (CommunicationMethod.OTHER in value.communicationMethods) {
    !value.otherCommunicationDetail.isNullOrBlank()
  } else {
    true
  }
}
