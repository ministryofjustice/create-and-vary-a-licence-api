package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.InputType.ADDRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.InputType.TEXT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.Case
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.adjustCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.formatAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.formatUsing
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.startsWithVowel

object Fields {
  /**
   * Some fields are only used for display purposes, e.g: expanding conditional inputs.
   * These fields do not contribute to the licence and should not be played back to the user.
   */
  val NON_CONTRIBUTING_FIELDS = setOf("numberOfCurfews", "nameTypeAndOrAddress")
}

enum class InputType(@JsonValue val description: String) {
  RADIO("radio"),
  ADDRESS("address"),
  TIME_PICKER("timePicker"),
  DATE_PICKER("datePicker"),
  FILE_UPLOAD("fileUpload"),
  TEXT("text"),
  CHECK("check"),
}

data class Input(
  override val type: InputType,
  val label: String,
  val name: String,
  override val listType: String? = null,
  val options: List<Option>? = null,
  override val case: Case? = null,
  override val handleIndefiniteArticle: Boolean? = null,
  val addAnother: AddAnother? = null,
  override val includeBefore: String? = null,
  val subtext: String? = null,
) : FormattingRule

interface FormattingRule {
  val type: InputType
  val case: Case?
  val listType: String?
  val includeBefore: String?
  val handleIndefiniteArticle: Boolean?

  companion object {
    val DEFAULT = object : FormattingRule {
      override val type = TEXT
      override val case = null
      override val listType = "AND"
      override val includeBefore = null
      override val handleIndefiniteArticle = null
    }
  }
}

fun FormattingRule.format(data: List<AdditionalConditionData>) = when (data.size) {
  0 -> ""
  1 -> formatSingleValue(data.first().dataValue!!)
  else -> formatMultipleValues(data)
}

private fun FormattingRule.formatSingleValue(item: String) = item
  .adjustCase(this.case)
  .let { this.applyFormattingRules(it) }

private fun FormattingRule.formatMultipleValues(data: List<AdditionalConditionData>) = data
  .mapNotNull { it.dataValue }
  .map { it.adjustCase(this.case) }
  .formatUsing(this.listType ?: "AND")
  .let { this.applyFormattingRules(it) }

fun FormattingRule.applyFormattingRules(value: String) = value
  .let { if (this.includeBefore != null) "${this.includeBefore}$it" else it }
  .let { if (this.type == ADDRESS) formatAddress(it) else it }
  .let {
    if (this.handleIndefiniteArticle != null) {
      if (it.startsWithVowel()) "an $it" else "a $it"
    } else {
      it
    }
  }

data class AddAnother(
  val label: String,
)
