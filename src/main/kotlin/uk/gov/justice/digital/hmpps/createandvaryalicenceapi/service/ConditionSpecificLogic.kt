package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val LONG_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy")
const val CONDITION_CODE_FOR_14B = "524f2fd6-ad53-47dd-8edc-2161d3dd2ed4"
const val CONDITION_14B_END_DATE = "endDate"

fun AdditionalCondition.getInitialData(): List<AdditionalConditionData> = when (conditionCode) {
  CONDITION_CODE_FOR_14B -> createAdditionalConditionDataFor14b()
  else -> emptyList()
}

private fun AdditionalCondition.createAdditionalConditionDataFor14b() = listOf(
  AdditionalConditionData(
    dataSequence = 0,
    dataField = CONDITION_14B_END_DATE,
    dataValue = licence.getElectronicMonitoringEndDate(),
    additionalCondition = this
  ),
  AdditionalConditionData(
    dataSequence = 1,
    dataField = "infoInputReviewed",
    dataValue = "false",
    additionalCondition = this
  )
)

fun Licence.getElectronicMonitoringEndDate(): String {
  val date = when {
    isLicenceExpiryLessThan12Months() -> licenceExpiryDate!!
    actualReleaseDate != null -> actualReleaseDate!!.plusYears(1)
    conditionalReleaseDate != null -> conditionalReleaseDate!!.plusYears(1)
    else -> throw IllegalStateException("licence: $id is missing both actualReleaseDate and conditionalReleaseDate")
  }
  return date.format(LONG_DATE_FORMATTER)
}

private fun Licence.isLicenceExpiryLessThan12Months(): Boolean {
  val licenceExpiryDate = licenceExpiryDate
    ?: throw IllegalStateException("No licence expiry date for licence: $id")
  return licenceExpiryDate.isBefore(LocalDate.now().plusMonths(12))
}
