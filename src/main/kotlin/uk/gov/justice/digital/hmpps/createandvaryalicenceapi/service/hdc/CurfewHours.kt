package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc

data class CurfewHours(
  val mondayFrom: String,
  val mondayUntil: String,
  val tuesdayFrom: String,
  val tuesdayUntil: String,
  val wednesdayFrom: String,
  val wednesdayUntil: String,
  val thursdayFrom: String,
  val thursdayUntil: String,
  val fridayFrom: String,
  val fridayUntil: String,
  val saturdayFrom: String,
  val saturdayUntil: String,
  val sundayFrom: String,
  val sundayUntil: String,
)
