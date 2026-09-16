package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.variations

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcCurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.Condition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.ImageUploadSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.VariedAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.VariedBespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.VariedConditions
import java.security.MessageDigest

fun compareLicenceConditions(originalLicence: Licence, variation: Licence): VariedConditions {
  val variedAdditionalLicenceConditions = compareAdditionalConditionSet(
    originalLicence.additionalLicenceConditions,
    variation.additionalLicenceConditions,
  )
  val variedBespokeConditions =
    compareBespokeConditionSet(originalLicence.bespokeConditions, variation.bespokeConditions)

  return VariedConditions(
    licenceConditionsAdded = variedAdditionalLicenceConditions.licenceConditionsAdded + variedBespokeConditions.licenceConditionsAdded,
    licenceConditionsRemoved = variedAdditionalLicenceConditions.licenceConditionsRemoved + variedBespokeConditions.licenceConditionsRemoved,
    licenceConditionsAmended = variedAdditionalLicenceConditions.licenceConditionsAmended + variedBespokeConditions.licenceConditionsAmended,
  )
}

private fun compareAdditionalConditionSet(
  originalConditionSet: List<AdditionalCondition>,
  variedConditionSet: List<AdditionalCondition>,
): VariedConditions {
  val originalConditions = groupConditions(originalConditionSet)
  val variedConditions = groupConditions(variedConditionSet)

  val sortedOriginalConditions: MutableList<ConditionAndImageUploads> =
    originalConditions.sortedBy { it.additionalCondition.code }.toMutableList()
  val sortedVariedConditions: MutableList<ConditionAndImageUploads> =
    variedConditions.sortedBy { it.additionalCondition.code }.toMutableList()

  val conditionsAdded: MutableList<Condition> = mutableListOf()
  val conditionsRemoved: MutableList<Condition> = mutableListOf()
  val conditionsAmended: MutableList<Condition> = mutableListOf()

  var originalCondition: ConditionAndImageUploads? = sortedOriginalConditions.removeFirstOrNull()
  var variedCondition: ConditionAndImageUploads? = sortedVariedConditions.removeFirstOrNull()

  while (originalCondition != null || variedCondition != null) {
    when {
      variedCondition == null || (originalCondition != null && originalCondition.additionalCondition.code!! < variedCondition.additionalCondition.code!!) -> {
        conditionsRemoved.add(
          VariedAdditionalCondition(
            category = originalCondition?.additionalCondition?.category,
            condition = originalCondition?.expandedText!!,
            uploadSummaries = originalCondition.uploadSummaries,
          ),
        )
        originalCondition = sortedOriginalConditions.removeFirstOrNull()
      }

      originalCondition == null || originalCondition.additionalCondition.code!! > variedCondition.additionalCondition.code!! -> {
        conditionsAdded.add(
          VariedAdditionalCondition(
            category = variedCondition.additionalCondition.category,
            condition = variedCondition.expandedText,
            uploadSummaries = variedCondition.uploadSummaries,
          ),
        )
        variedCondition = sortedVariedConditions.removeFirstOrNull()
      }

      else -> {
        if (originalCondition.expandedText != variedCondition.expandedText ||
          hasUpdateExclusionZones(
            originalCondition.uploadSummaries,
            variedCondition.uploadSummaries,
          )
        ) {
          conditionsAmended.add(
            VariedAdditionalCondition(
              category = variedCondition.additionalCondition.category,
              condition = variedCondition.expandedText,
              uploadSummaries = variedCondition.uploadSummaries,
            ),
          )
        }

        originalCondition = sortedOriginalConditions.removeFirstOrNull()
        variedCondition = sortedVariedConditions.removeFirstOrNull()
      }
    }
  }

  return VariedConditions(
    licenceConditionsAdded = conditionsAdded,
    licenceConditionsRemoved = conditionsRemoved,
    licenceConditionsAmended = conditionsAmended,
  )
}

private fun groupConditions(conditionSet: List<AdditionalCondition>): List<ConditionAndImageUploads> = conditionSet.groupBy { it.code }.map { (_, conditions) ->
  val conditionsText = conditions.map { it.expandedText ?: it.text }.joinToString("\n\n")
  ConditionAndImageUploads(
    additionalCondition = conditions.first(),
    expandedText = conditionsText,
    uploadSummaries = createConditionAndUploads(conditions),
  )
}

private fun createConditionAndUploads(additionalConditions: List<AdditionalCondition>): List<ImageUploadSummary> = additionalConditions.mapNotNull { condition ->
  val uploadSummary = condition.uploadSummary
  if (uploadSummary.isNotEmpty()) {
    ImageUploadSummary(
      text = condition.text,
      description = uploadSummary[0].description,
      thumbnailImage = uploadSummary[0].thumbnailImage,
    )
  } else {
    null
  }
}

fun compareBespokeConditionSet(
  originalConditionSet: List<BespokeCondition>,
  variedConditionSet: List<BespokeCondition>,
): VariedConditions {
  val sortedOriginalConditionSet = originalConditionSet.sortedBy { it.text }.toMutableList()
  val sortedVariedConditionSet = variedConditionSet.sortedBy { it.text }.toMutableList()

  val conditionsAdded: MutableList<Condition> = mutableListOf()
  val conditionsRemoved: MutableList<Condition> = mutableListOf()
  val conditionsAmended: MutableList<Condition> = mutableListOf()

  var originalCondition: BespokeCondition? = sortedOriginalConditionSet.removeFirstOrNull()
  var variedCondition: BespokeCondition? = sortedVariedConditionSet.removeFirstOrNull()

  while (originalCondition != null || variedCondition != null) {
    when {
      variedCondition == null || (originalCondition != null && originalCondition.text!! < variedCondition.text!!) -> {
        conditionsRemoved.add(
          VariedBespokeCondition(
            category = "Bespoke condition",
            condition = originalCondition!!.text!!,
          ),
        )
        originalCondition = sortedOriginalConditionSet.removeFirstOrNull()
      }

      originalCondition == null || originalCondition.text!! > variedCondition.text!! -> {
        conditionsAdded.add(
          VariedBespokeCondition(
            category = "Bespoke condition",
            condition = variedCondition.text!!,
          ),
        )
        variedCondition = sortedVariedConditionSet.removeFirstOrNull()
      }

      else -> {
        originalCondition = sortedOriginalConditionSet.removeFirstOrNull()
        variedCondition = sortedVariedConditionSet.removeFirstOrNull()
      }
    }
  }

  return VariedConditions(
    licenceConditionsAdded = conditionsAdded,
    licenceConditionsRemoved = conditionsRemoved,
    licenceConditionsAmended = conditionsAmended,
  )
}

fun hasUpdatedCurfewAddress(originalAddress: HdcCurfewAddress?, variedAddress: HdcCurfewAddress?): Boolean = (
  originalAddress?.firstLine != variedAddress?.firstLine ||
    originalAddress?.secondLine != variedAddress?.secondLine ||
    originalAddress?.county != variedAddress?.county ||
    originalAddress?.postcode != variedAddress?.postcode ||
    originalAddress?.townOrCity != variedAddress?.townOrCity
  )

fun hasUpdatedCurfewHours(
  originalCurfewHours: List<CurfewTimes>,
  variedCurfewHours: List<CurfewTimes>,
): Boolean = originalCurfewHours.any { curfew ->
  val variedCurfew = variedCurfewHours.find { v -> v.curfewTimesSequence == curfew.curfewTimesSequence }
  curfew.fromTime != variedCurfew?.fromTime ||
    curfew.untilTime != variedCurfew?.untilTime ||
    curfew.fromDay != variedCurfew?.fromDay ||
    curfew.untilDay != variedCurfew?.untilDay
}

private fun hasUpdateExclusionZones(
  originalUploadSummaries: List<ImageUploadSummary>,
  variedUploadSummaries: List<ImageUploadSummary>,
): Boolean {
  val originalImages =
    originalUploadSummaries.map { upload -> upload.thumbnailImage?.md5() }.sortedBy { it }.filterNotNull()
  val variedImages =
    variedUploadSummaries.map { upload -> upload.thumbnailImage?.md5() }.sortedBy { it }.filterNotNull()
  return originalImages != variedImages
}

fun String.md5(): String {
  val md = MessageDigest.getInstance("MD5")
  val digest = md.digest(this.toByteArray())
  return digest.toHexString()
}

data class ConditionAndImageUploads(
  val additionalCondition: AdditionalCondition,
  val expandedText: String,
  val uploadSummaries: List<ImageUploadSummary> = emptyList(),
)
