package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import com.fasterxml.jackson.annotation.JsonValue

enum class SarLicenceType(@JsonValue val description: String) {
  AP("All purpose"),
  AP_PSS("All purpose & post sentence supervision"),
  PSS("Post sentence supervision"),
}
