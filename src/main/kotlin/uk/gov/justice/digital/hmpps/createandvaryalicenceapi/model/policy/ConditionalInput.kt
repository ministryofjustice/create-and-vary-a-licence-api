package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.Case

data class ConditionalInput(
  val type: InputType,
  val label: String,
  val name: String,
  val case: Case? = null,
  val handleIndefiniteArticle: Boolean? = null,
  val includeBefore: String? = null,
  val subtext: String? = null,
) {
  fun toInput() = Input(
    type = type,
    label = label,
    name = name,
    listType = null,
    options = null,
    case = case,
    handleIndefiniteArticle = handleIndefiniteArticle,
    includeBefore = includeBefore,
    subtext = subtext,
  )
}
