package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

enum class LicenceDeactivationReason(val message: String) {
  RECALLED("Licence inactivated due to being recalled"),
  FIXED_TERM("Licence inactivated due to the offender returning to custody on a fixed term recall"),
  STANDARD_RECALL("Licence inactivated due to the offender returning to custody on a standard recall"),
  RESENTENCED("Licence inactivated due to being resentenced"),
}
