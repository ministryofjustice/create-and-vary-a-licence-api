package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.adjustCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.formatAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.formatUsing
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.startsWithVowel

private const val ADDRESS = "address"

data class Input(
  val type: String,
  val label: String,
  val name: String,
  val listType: String? = null,
  val options: List<Option>? = null,
  val case: String? = null,
  val handleIndefiniteArticle: Boolean? = null,
  val addAnother: AddAnother? = null,
  val includeBefore: String? = null,
  val subtext: String? = null,
)

fun Input?.format(data: List<AdditionalConditionData>) = when (data.size) {
  0 -> ""
  1 -> this.format(data.first().dataValue!!)
  else -> data.mapNotNull { it.dataValue }.formatUsing(this?.listType ?: "AND").let { this.format(it) }
}

fun Input?.format(
  value: String
): String {
  if (this == null) return value
  return value
    .let { value.adjustCase(this.case) }
    .let { if (this.includeBefore != null) "${this.includeBefore}$it" else it }
    .let { if (this.type == ADDRESS) formatAddress(it) else it }
    .let {
      if (this.handleIndefiniteArticle != null)
        if (it.startsWithVowel()) "an $it" else "a $it"
      else it
    }
}

data class AddAnother(
  val label: String,
)
