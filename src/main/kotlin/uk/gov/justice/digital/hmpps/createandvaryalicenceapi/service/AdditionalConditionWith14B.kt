package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateTimeFormatter = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy")!!
private const val CONDITION_CODE_FOR_14B = "524f2fd6-ad53-47dd-8edc-2161d3dd2ed4"
private const val LED_CHANGE_MSG = "licence end date"
private const val CRD_OR_ARD_CHANGE_MSG = "release date"
private const val END_DATE = "endDate"

fun updateAdditionalConditionWithAdditionConditionDataForElectronicMonitoring(
  sentenceChanges: SentenceChanges,
  updatedLicenceEntity: Licence,
  licenceEntity: Licence
): AdditionalCondition? {
  val electronicMonitoringCondition = getElectronicMonitoringCondition(licenceEntity)
  electronicMonitoringCondition?.let {
    if (isLicenseOfValidStatusCodeForUpdate(licenceEntity)) {
      calculateEndDateForLedArdCrdChanges(sentenceChanges, updatedLicenceEntity, licenceEntity)?.let { it ->
        val electronicMonitoringConditionData = electronicMonitoringCondition.additionalConditionData
          .find { additionalConditionData -> additionalConditionData.dataField == END_DATE }?.copy(
            dataValue = it
          )
        return electronicMonitoringCondition.copy(
          conditionVersion = updatedLicenceEntity.version!!,
          additionalConditionData = electronicMonitoringCondition.additionalConditionData.removeExistingEndDateAndAddNew(
            electronicMonitoringConditionData!!
          ),
          expandedConditionText = electronicMonitoringCondition.conditionText?.replace("[INSERT END DATE]", it!!)
        )
      }
    }
  }
  return null
}
fun getAdditionalConditionDataForElectronicMonitoring(
  conditionCode: String,
  licenceEntity: Licence,
  newAdditionalCondition: AdditionalCondition
): List<AdditionalConditionData>? {
  return when {
    requiresInitialElectronicMonitoringData(conditionCode, licenceEntity) -> createAdditionalConditionData(licenceEntity, newAdditionalCondition)
    else -> null
  }
}
fun getReasonForDateChange(
  hasLedChange: Boolean,
  updatedLicenceEntity: Licence,
  licenceEntity: Licence
): String? {
  val hasArdOrCrdChanged = hasArdOrCrdChanged(updatedLicenceEntity, licenceEntity)
  return when {
    hasLedChange && hasArdOrCrdChanged -> "$CRD_OR_ARD_CHANGE_MSG and $LED_CHANGE_MSG"
    hasLedChange -> LED_CHANGE_MSG
    hasArdOrCrdChanged -> CRD_OR_ARD_CHANGE_MSG
    else -> null
  }
}
private fun isElectronicMonitoringConditionPresent(
  conditionCode: String
) = conditionCode == CONDITION_CODE_FOR_14B

fun calculateEndDate(
  licenceEntity: Licence
): String {
  return when {
    isLicenseExpiryDateOnOrAfterTwelveMonths(licenceEntity.licenceExpiryDate) -> calculateEndDateWithArdOrCrd(licenceEntity)
    else -> licenceEntity.licenceExpiryDate?.format(dateTimeFormatter).toString()
  }
}
private fun isLicenseOfValidStatusCode(
  licenceEntity: Licence
) = setOf(
  LicenceStatus.IN_PROGRESS,
  LicenceStatus.SUBMITTED,
  LicenceStatus.APPROVED,
  LicenceStatus.VARIATION_IN_PROGRESS,
  LicenceStatus.VARIATION_SUBMITTED,
  LicenceStatus.VARIATION_APPROVED
)
  .contains(licenceEntity.statusCode)

private fun isLicenseOfValidStatusCodeForUpdate(
  licenceEntity: Licence
) = setOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED, LicenceStatus.APPROVED)
  .contains(licenceEntity.statusCode)

// USE ARD if exits else CRD
private fun calculateEndDateWithArdOrCrd(
  licenceEntity: Licence
): String {
  val additionalConditionEndDate = licenceEntity.actualReleaseDate
    ?: licenceEntity.conditionalReleaseDate
  return additionalConditionEndDate?.plusYears(1)?.format(dateTimeFormatter).toString()
}

private fun getElectronicMonitoringCondition(
  licenceEntity: Licence
) = licenceEntity.additionalConditions.find { additionalCondition ->
  isElectronicMonitoringConditionPresent(additionalCondition.conditionCode!!)
}

private fun hasDateChanged(
  oldDate: LocalDate?,
  newDate: LocalDate?
) = nullableDatesDiffer(oldDate, newDate)

private fun calculateEndDateForLEDChange(
  updatedLicenceEntity: Licence,
  licenceEntity: Licence
): String? {
  if (isLicenseExpiryDateOnOrAfterTwelveMonths(updatedLicenceEntity.licenceExpiryDate)) {
    // existing license LED before 12 months
    if (!isLicenseExpiryDateOnOrAfterTwelveMonths(licenceEntity.licenceExpiryDate)) {
      return calculateEndDateWithArdOrCrd(updatedLicenceEntity)
    }
  } else {
    return updatedLicenceEntity.licenceExpiryDate?.format(dateTimeFormatter)
  }
  return null
}

private fun calculateEndDate(
  licenceEntity: Licence,
  updatedLicenceEntity: Licence
): String? = if (isLicenseExpiryDateOnOrAfterTwelveMonths(licenceEntity.licenceExpiryDate)) {
  calculateEndDateWithArdOrCrd(updatedLicenceEntity)
} else {
  null
}
private fun createAdditionalConditionData(
  licenceEntity: Licence,
  newAdditionalCondition: AdditionalCondition
) = listOf(
  AdditionalConditionData(
    dataSequence = 0,
    dataField = "endDate",
    dataValue = calculateEndDate(licenceEntity),
    additionalCondition = newAdditionalCondition
  ),
  AdditionalConditionData(
    dataSequence = 1,
    dataField = "infoInputReviewed",
    dataValue = "false",
    additionalCondition = newAdditionalCondition
  )
)
private fun calculateEndDateForLedArdCrdChanges(
  sentenceChanges: SentenceChanges,
  updatedLicenceEntity: Licence,
  licenceEntity: Licence
): String? {
  return when {
    sentenceChanges.ledChanged -> calculateEndDateForLEDChange(updatedLicenceEntity, licenceEntity)
    hasDateChanged(updatedLicenceEntity.actualReleaseDate, licenceEntity.actualReleaseDate) -> calculateEndDate(licenceEntity, updatedLicenceEntity)
    hasDateChanged(updatedLicenceEntity.conditionalReleaseDate, licenceEntity.conditionalReleaseDate) && licenceEntity.actualReleaseDate == null -> calculateEndDate(licenceEntity, updatedLicenceEntity)
    else -> null
  }
}
private fun List<AdditionalConditionData>.removeExistingEndDateAndAddNew(
  newAdditionalCondition: AdditionalConditionData
): List<AdditionalConditionData> {
  val restAdditionalConditionDate = filter { it.dataField != END_DATE }.toMutableList()
  restAdditionalConditionDate.add(newAdditionalCondition)
  return restAdditionalConditionDate
}
private fun requiresInitialElectronicMonitoringData(
  conditionCode: String,
  licenceEntity: Licence
) = isElectronicMonitoringConditionPresent(conditionCode) && isLicenseOfValidStatusCode(licenceEntity)

private fun isLicenseExpiryDateOnOrAfterTwelveMonths(
  licenceExpiryDate: LocalDate?
) = licenceExpiryDate?.isEqual(LocalDate.now().plusMonths(12)) == true ||
  licenceExpiryDate?.isAfter(LocalDate.now().plusMonths(12)) == true
private fun hasArdOrCrdChanged(
  updatedLicenceEntity: Licence,
  licenceEntity: Licence
) = hasDateChanged(updatedLicenceEntity.actualReleaseDate, licenceEntity.actualReleaseDate) || hasDateChanged(
  updatedLicenceEntity.conditionalReleaseDate,
  licenceEntity.conditionalReleaseDate
)
