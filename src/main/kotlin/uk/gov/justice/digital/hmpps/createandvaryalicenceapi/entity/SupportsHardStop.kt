package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

interface SupportsHardStop {
  var versionOfId: Long?
  fun timeOut()
}
