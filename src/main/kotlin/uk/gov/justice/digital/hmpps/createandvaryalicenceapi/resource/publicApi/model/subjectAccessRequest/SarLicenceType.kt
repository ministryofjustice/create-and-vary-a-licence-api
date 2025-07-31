package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType

enum class SarLicenceType(@JsonValue val description: String) {
  AP("All purpose"),
  AP_PSS("All purpose & post sentence supervision"),
  PSS("Post sentence supervision"),
  ;

  companion object {
    fun from(type: LicenceType) = when (type) {
      LicenceType.AP -> AP
      LicenceType.AP_PSS -> AP_PSS
      LicenceType.PSS -> PSS
    }
  }
}
