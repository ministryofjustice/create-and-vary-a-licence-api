package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import com.fasterxml.jackson.annotation.JsonValue

enum class SarAuditEventType(@JsonValue val description: String) {
  USER_EVENT("User event"),
  SYSTEM_EVENT("System event"),
}
