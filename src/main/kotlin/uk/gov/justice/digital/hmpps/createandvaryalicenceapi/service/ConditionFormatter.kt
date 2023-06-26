package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.FormattingRule
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.IAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.format

@Service
class ConditionFormatter {

  fun format(condition: IAdditionalCondition, data: List<AdditionalConditionData>): String {
    if (!condition.requiresInput) {
      return condition.text
    }
    val placeholders = condition.tpl.getPlaceholderNames()
    val rules = condition.getFormattingRules()

    return placeholders.fold(condition.tpl ?: "") { conditionText, placeholder ->
      val fieldName = data.findFieldNameToUse(placeholder)
      val conditionData = data.getProvidedDataFor(fieldName)
      val value = (rules.find { it.name == fieldName } ?: FormattingRule.DEFAULT).format(conditionData)
      conditionText.replacePlaceholder(placeholder, value)
    }
  }
}

/**
 * Double pipe notation in a placeholder is used to indicate that either value can be used.
 * The field names are in priority order (i.e. the second field name will be used only if the first field has no data)
 */
private fun List<AdditionalConditionData>.findFieldNameToUse(placeholder: String) =
  placeholder
    .splitToSequence("||")
    .map { it.trim() }
    .find { this.getProvidedDataFor(it).isNotEmpty() }
    ?: placeholder

fun List<AdditionalConditionData>.getProvidedDataFor(name: String) = this.filter { it.dataField == name }
