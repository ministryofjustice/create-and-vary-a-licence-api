package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

class OffenderProfile(
  val disabilities: List<Disability>?,
  val ethnicity: String?,
  val immigrationStatus: String?,
  val nationality: String?,
  val notes: String?,
  val offenderDetails: String?,
  val offenderLanguages: OffenderLanguages?,
  val previousConviction: PreviousConviction?,
  val religion: String?,
  val remandStatus: String?,
  val riskColour: String?,
  val secondaryNationality: String?,
  val sexualOrientation: String?,
)
