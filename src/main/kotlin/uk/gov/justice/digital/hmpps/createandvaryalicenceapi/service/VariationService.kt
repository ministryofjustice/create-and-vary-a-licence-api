package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceKinds
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.VariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.Condition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.VariedAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.VariedBespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.VariedConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.InvalidStateException

data class ImageUploadSummary(
  val text: String?,
  val description: String?,
  val thumbnailImage: String?,
)

data class ConditionAndImageUploads(
  val additionalCondition: AdditionalCondition,
  val expandedText: String,
  val uploadSummaries: List<ImageUploadSummary> = emptyList(),
)

@Service
class VariationService(
  private val licenceService: LicenceService,
) {
  @Transactional
  fun variationDiffFromParent(variationId: Long): VariedConditions {
    val variationLicence = licenceService.getLicenceById(variationId)
    val kind = variationLicence.kind
    if (kind != LicenceKinds.VARIATION && kind != LicenceKinds.HDC_VARIATION) {
      throw InvalidStateException("Licence with id $variationId is not a variation")
    }

    val originalLicence = licenceService.getLicenceById((variationLicence as VariationLicence).variationOf!!)
    return compareLicenceConditions(originalLicence, variationLicence)
  }

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
          if (originalCondition.expandedText != variedCondition.expandedText) {
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

  fun createConditionAndUploads(additionalConditions: List<AdditionalCondition>): List<ImageUploadSummary> = additionalConditions.map { condition ->
    val uploadSummary = condition.uploadSummary
    if (uploadSummary.isNotEmpty()) {
      ImageUploadSummary(
        text = condition.text,
        description = uploadSummary[0].description,
        thumbnailImage = uploadSummary[0].thumbnailImage,
      )
    }
    return emptyList()
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
}
