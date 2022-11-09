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

fun isAdditionalConditionOf14B(
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
fun hasCorrectStatusCodeForNew14B(
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

fun hasCorrectStatusCodeForExisting14B(
  licenceEntity: Licence
) = setOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED, LicenceStatus.APPROVED)
  .contains(licenceEntity.statusCode)

fun isLicenseExpiryDateOnOrAfterTwelveMonths(
  licenceExpiryDate: LocalDate?
) = licenceExpiryDate?.isEqual(LocalDate.now().plusMonths(12)) == true ||
  licenceExpiryDate?.isAfter(LocalDate.now().plusMonths(12)) == true

// USE ARD if exits else CRD
fun calculateEndDateWithArdOrCrd(
  licenceEntity: Licence
): String {
  val additionalConditionEndDate = licenceEntity.actualReleaseDate
    ?: licenceEntity.conditionalReleaseDate
  return additionalConditionEndDate?.plusYears(1)?.format(dateTimeFormatter).toString()
}

fun hasExistingLicenseContains14B(
  licenceEntity: Licence
) = licenceEntity.additionalConditions.filter { additionalCondition ->
  isAdditionalConditionOf14B(additionalCondition.conditionCode!!)
}.toMutableList()

fun hasDateChanged(
  oldDate: LocalDate?,
  newDate: LocalDate?
) = nullableDatesDiffer(oldDate, newDate)

fun calculateEndDateForLEDChange(
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

fun calculateEndDate(
  licenceEntity: Licence,
  updatedLicenceEntity: Licence
): String? = if (isLicenseExpiryDateOnOrAfterTwelveMonths(licenceEntity.licenceExpiryDate)) {
  calculateEndDateWithArdOrCrd(updatedLicenceEntity)
} else {
  null
}

fun isARDNull(
  licenceEntity: Licence
) = (licenceEntity.actualReleaseDate == null)

fun getReasonFor14BDateChange(
  hasLedChange: Boolean,
  updatedLicenceEntity: Licence,
  licenceEntity: Licence
): String? {
  var dateChanges: String? = null
  if (hasLedChange) {
    dateChanges = ledDateChangeMsg
  } else if (hasArdOrCrdChanged(updatedLicenceEntity, licenceEntity)) {
    dateChanges = crdOrArdChangeMsg
  }
  if (hasLedChange && hasArdOrCrdChanged(updatedLicenceEntity, licenceEntity)) {
    dateChanges = ledAndCrdOrArdChangeMsg
  }

  return dateChanges
}

fun hasArdOrCrdChanged(
  updatedLicenceEntity: Licence,
  licenceEntity: Licence
) = hasDateChanged(updatedLicenceEntity.actualReleaseDate, licenceEntity.actualReleaseDate) || hasDateChanged(
  updatedLicenceEntity.conditionalReleaseDate,
  licenceEntity.conditionalReleaseDate
)

fun createAdditionalConditionDataFor14b(
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

fun calculateEndDateForLedArdCrdChanges(
  sentenceChanges: SentenceChanges,
  updatedLicenceEntity: Licence,
  licenceEntity: Licence
): String? {
  var endDate: String? = null
  if (sentenceChanges.ledChanged) {
    endDate = calculateEndDateForLEDChange(updatedLicenceEntity, licenceEntity)
  } else if (hasDateChanged(updatedLicenceEntity.actualReleaseDate, licenceEntity.actualReleaseDate)) {
    endDate = calculateEndDate(licenceEntity, updatedLicenceEntity)
  } else if (hasDateChanged(updatedLicenceEntity.conditionalReleaseDate, licenceEntity.conditionalReleaseDate) && isARDNull(licenceEntity)
  ) {
    endDate = calculateEndDate(licenceEntity, updatedLicenceEntity)
  }
  return endDate
}
fun List<AdditionalConditionData>.removeExistingEndDateAndAddNew(
  newAdditionalCondition: AdditionalConditionData
): List<AdditionalConditionData> {
  val restAdditionalConditionDate = filter { it.dataField != "endDate" }.toMutableList()
  restAdditionalConditionDate.add(newAdditionalCondition)
  return restAdditionalConditionDate
}
