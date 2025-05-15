package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates

enum class LicenceDateType(
  val description: String,
  val hdcOnly: Boolean = false,
  val notifyOnChange: Boolean = true,
  val denotesMaterialChange: Boolean = notifyOnChange,
) {
  LSD("Release date"),
  CRD("Conditional release data", notifyOnChange = false),
  ARD("Confirmed release data", notifyOnChange = false),
  LED("Licence end date"),
  SSD("Sentence start date", notifyOnChange = false),
  TUSSD("Top up supervision start date"),
  TUSED("Top up supervision end date"),
  PRRD("Post recall release date"),
  HDCAD("HDC actual date", hdcOnly = true),
  HDCENDDATE("HDC end date", hdcOnly = true),

  /**
   * SED has special rules that mean we include any changes in notifications,
   * but it won't trigger a notification on its own unless the licence is approved
   */
  SED("Sentence end date", notifyOnChange = true, denotesMaterialChange = false),
}
