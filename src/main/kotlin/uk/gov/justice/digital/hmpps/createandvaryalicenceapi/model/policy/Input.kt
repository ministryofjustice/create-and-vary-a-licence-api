package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

data class Input(
  val type: String,
  val label: String,
  val name: String,
  val listType: String?,
  val options: List<Option>?,
  val case: String?,
  val handleIndefiniteArticle: Boolean?,
  val addAnother: AddAnother?,
  val includeBefore: String?,
  val subtext: String?,
  val helpLink: HelpLink?
)

data class AddAnother(
  val label: String,
)

data class HelpLink(
  val summary: String,
  val text: String
)
