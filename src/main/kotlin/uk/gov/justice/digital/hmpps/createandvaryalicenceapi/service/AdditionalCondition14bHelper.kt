package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateTimeFormatter = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy")!!
private const val conditionCode14B = "524f2fd6-ad53-47dd-8edc-2161d3dd2ed4"
private const val ledDateChangeMsg = "licence end date"
private const val crdOrArdChangeMsg = "release date"
private const val ledAndCrdOrArdChangeMsg = "release date and licence end date"
private const val END_Date = "endDate"

fun updateAdditionalConditionWithAdditionConditionData(
  sentenceChanges: SentenceChanges,
  updatedLicenceEntity: Licence,
  licenceEntity: Licence
): AdditionalCondition? {
  var endDate: String? = null
  val additionalCondition14b = hasExistingLicenseContains14B(licenceEntity)
  additionalCondition14b?.let {
    if (hasCorrectStatusCodeForExisting14B(licenceEntity)) {
      endDate = calculateEndDateForLedArdCrdChanges(sentenceChanges, updatedLicenceEntity, licenceEntity)
    }
    if (!endDate.isNullOrBlank()) {
      val additionalConditionDataWith14bEndDate = additionalCondition14b.additionalConditionData
        .find { it.dataField == END_Date }?.copy(
          dataValue = endDate
        )
      return additionalCondition14b.copy(
        conditionVersion = updatedLicenceEntity.version!!,
        additionalConditionData = additionalCondition14b.additionalConditionData.removeExistingEndDateAndAddNew(
          additionalConditionDataWith14bEndDate!!
        ),
        expandedConditionText = additionalCondition14b.conditionText?.replace("[INSERT END DATE]", endDate!!)
      )
    }
  }
  return null
}
fun addAdditionalConditionDatasToAdditionalCondition(
  conditionCode: String,
  licenceEntity: Licence,
  newAdditionalCondition: AdditionalCondition
): List<AdditionalConditionData>? {
  return when {
    isAdditionalConditionOf14BWithCorrectStatusCode(conditionCode, licenceEntity) -> createAdditionalConditionDataFor14b(licenceEntity, newAdditionalCondition)
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
    hasLedChange && hasArdOrCrdChanged -> ledAndCrdOrArdChangeMsg
    hasLedChange -> ledDateChangeMsg
    hasArdOrCrdChanged -> crdOrArdChangeMsg
    else -> null
  }
}
private fun isAdditionalConditionOf14B(
  conditionCode: String
) = conditionCode == conditionCode14B

fun calculateEndDate(
  licenceEntity: Licence
): String {
  if (isLicenseExpiryDateOnOrAfterTwelveMonths(licenceEntity.licenceExpiryDate)) {
    return calculateEndDateWithArdOrCrd(licenceEntity)
  } else (
    return licenceEntity.licenceExpiryDate?.format(dateTimeFormatter).toString()
    )
}
private fun hasCorrectStatusCodeForNew14B(
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

private fun hasCorrectStatusCodeForExisting14B(
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

private fun hasExistingLicenseContains14B(
  licenceEntity: Licence
) = licenceEntity.additionalConditions.find { additionalCondition ->
  isAdditionalConditionOf14B(additionalCondition.conditionCode!!)
}

private fun hasDateChanged(
  oldDate: LocalDate?,
  newDate: LocalDate?
) = nullableDatesDiffer(oldDate, newDate)

private fun calculateEndDateForLEDChange(
  updatedLicenceEntity: Licence,
  licenceEntity: Licence
): String? {
  var endDate: String? = null
  if (isLicenseExpiryDateOnOrAfterTwelveMonths(updatedLicenceEntity.licenceExpiryDate)) {
    // existing license LED before 12 months
    if (!isLicenseExpiryDateOnOrAfterTwelveMonths(licenceEntity.licenceExpiryDate)) {
      endDate = calculateEndDateWithArdOrCrd(updatedLicenceEntity)
    }
  } else {
    endDate = updatedLicenceEntity.licenceExpiryDate?.format(dateTimeFormatter)
  }
  return endDate
}

private fun calculateEndDate(
  licenceEntity: Licence,
  updatedLicenceEntity: Licence
): String? = if (isLicenseExpiryDateOnOrAfterTwelveMonths(licenceEntity.licenceExpiryDate)) {
  calculateEndDateWithArdOrCrd(updatedLicenceEntity)
} else {
  null
}
private fun createAdditionalConditionDataFor14b(
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
  val restAdditionalConditionDate = filter { it.dataField != END_Date }.toMutableList()
  restAdditionalConditionDate.add(newAdditionalCondition)
  return restAdditionalConditionDate
}
private fun isAdditionalConditionOf14BWithCorrectStatusCode(
  conditionCode: String,
  licenceEntity: Licence
) = isAdditionalConditionOf14B(conditionCode) && hasCorrectStatusCodeForNew14B(licenceEntity)

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
