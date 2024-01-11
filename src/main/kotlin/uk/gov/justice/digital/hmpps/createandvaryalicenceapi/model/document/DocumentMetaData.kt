package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.document

data class DocumentMetaData(
  val licenceId: String,
  val additionalConditionId: String,
  val documentType: String,
)
