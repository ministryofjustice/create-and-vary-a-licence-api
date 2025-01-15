package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import com.fasterxml.jackson.annotation.JsonValue

enum class SarAppointmentTimeType(@JsonValue val description: String) {
  IMMEDIATE_UPON_RELEASE("Immediate upon release"),
  NEXT_WORKING_DAY_2PM("Next working day before 2pm"),
  SPECIFIC_DATE_TIME("Specific date time"),
}
