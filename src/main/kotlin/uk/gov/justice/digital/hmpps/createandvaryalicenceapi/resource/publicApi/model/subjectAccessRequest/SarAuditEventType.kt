package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType

enum class SarAuditEventType(@JsonValue val description: String) {
  USER_EVENT("User event"),
  SYSTEM_EVENT("System event"),
  ;

  companion object {
    fun from(type: AuditEventType) = when (type) {
      AuditEventType.USER_EVENT -> USER_EVENT
      AuditEventType.SYSTEM_EVENT -> SYSTEM_EVENT
    }
  }
}
