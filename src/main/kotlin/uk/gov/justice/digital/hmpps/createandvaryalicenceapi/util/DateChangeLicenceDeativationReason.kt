package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

enum class DateChangeLicenceDeativationReason(val message: String) {
  RECALLED("Licence inactivated due to being recalled"),
  RESENTENCED("Licence inactivated due to being resentenced"),
}
