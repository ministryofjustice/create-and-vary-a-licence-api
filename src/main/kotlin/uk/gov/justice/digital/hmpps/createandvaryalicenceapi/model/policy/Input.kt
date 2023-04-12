package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

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

data class AddAnother(
  val label: String,
)
