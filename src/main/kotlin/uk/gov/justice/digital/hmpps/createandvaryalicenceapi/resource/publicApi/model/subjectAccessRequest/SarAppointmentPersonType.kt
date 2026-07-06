package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentType

enum class SarAppointmentPersonType(@JsonValue val description: String) {
  DUTY_OFFICER("Duty officer"),
  RESPONSIBLE_COM("Responsible COM"),
  SPECIFIC_PERSON("Specific person"),
  NO_APPOINTMENT_NEEDED("No appointment needed"),
  ;

  companion object {
    fun from(type: AppointmentType?) = when (type) {
      null -> null
      AppointmentType.DUTY_OFFICER -> DUTY_OFFICER
      AppointmentType.RESPONSIBLE_COM -> RESPONSIBLE_COM
      AppointmentType.SPECIFIC_PERSON -> SPECIFIC_PERSON
      AppointmentType.NO_APPOINTMENT_NEEDED -> NO_APPOINTMENT_NEEDED
    }
  }
}
