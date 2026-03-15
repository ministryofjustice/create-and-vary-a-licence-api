package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentPersonType

enum class SarAppointmentPersonType(@JsonValue val description: String) {
  DUTY_OFFICER("Duty officer"),
  RESPONSIBLE_COM("Responsible COM"),
  SPECIFIC_PERSON("Specific person"),
  ;

  companion object {
    fun from(type: AppointmentPersonType?) = when (type) {
      null -> null
      AppointmentPersonType.DUTY_OFFICER -> DUTY_OFFICER
      AppointmentPersonType.RESPONSIBLE_COM -> RESPONSIBLE_COM
      AppointmentPersonType.SPECIFIC_PERSON -> SPECIFIC_PERSON
    }
  }
}
