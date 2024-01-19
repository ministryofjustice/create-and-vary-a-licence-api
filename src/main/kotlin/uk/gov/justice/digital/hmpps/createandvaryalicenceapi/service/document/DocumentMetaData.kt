package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.document

data class DocumentMetaData(
  val licenceId: String,
  val additionalConditionId: String,
  val documentType: String,
)
