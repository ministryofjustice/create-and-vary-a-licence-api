package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.HasInputs
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.IAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.Input
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.format

// check no additional condition data items have null values in DB

class ConditionFormatter {

  fun format(condition: IAdditionalCondition, data: List<AdditionalConditionData>): String {
    if (!condition.requiresInput) {
      return condition.text
    }
    val placeholders = condition.tpl.getPlaceholderNames()
    val rules = condition.getFormattingRules()

    return placeholders.fold(condition.tpl ?: "") { conditionText, placeholder ->
      val fieldName = data.findFieldNameToUse(placeholder)
      val matchingItems = data.getItemsNamed(fieldName)
      val value = rules.find { it.name == fieldName }.format(matchingItems)
      conditionText.replacePlaceholder(placeholder, value)
    }
  }

  // Recursively gather all formatting rules, top level and then recursively to get any conditional rules
  private fun HasInputs.getFormattingRules(): List<Input> {
    val inputs = this.getConditionInputs() ?: emptyList()
    val options = inputs.flatMap { it.options ?: emptyList() }
    val conditionalInputs =
      options.filter { it.conditional != null }.mapNotNull { it.conditional }.flatMap { it.getFormattingRules() }

    return inputs + conditionalInputs
  }
}

fun List<AdditionalConditionData>.getItemsNamed(name: String) = this.filter { it.dataField == name }

/**
 * Double pipe notation is used to indicate that either value can be used in this placeholder.
 * The field names are in priority order (i.e. the second field name will be used only if the first field has no data)
 */
private fun List<AdditionalConditionData>.findFieldNameToUse(placeholder: String) =
  placeholder.splitToSequence("||").map { it.trim() }.find { this.getItemsNamed(it).isNotEmpty() } ?: placeholder

fun String?.getPlaceholderNames(): List<String> =
  this?.let { "\\{(.*?)}".toRegex().findAll(this).map { it.groupValues[1].trim() }.toList() } ?: emptyList()

fun String.replacePlaceholder(placeholder: String, replacement: String) =
  this.replaceFirst("{$placeholder}", replacement)
