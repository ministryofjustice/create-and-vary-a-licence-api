package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType

enum class SarAppointmentTimeType(@JsonValue val description: String) {
  IMMEDIATE_UPON_RELEASE("Immediate upon release"),
  NEXT_WORKING_DAY_2PM("Next working day before 2pm"),
  SPECIFIC_DATE_TIME("Specific date time"),
  ;

  companion object {
    fun from(type: AppointmentTimeType?) = when (type) {
      null -> null
      AppointmentTimeType.IMMEDIATE_UPON_RELEASE -> IMMEDIATE_UPON_RELEASE
      AppointmentTimeType.NEXT_WORKING_DAY_2PM -> NEXT_WORKING_DAY_2PM
      AppointmentTimeType.SPECIFIC_DATE_TIME -> SPECIFIC_DATE_TIME
    }
  }
}
