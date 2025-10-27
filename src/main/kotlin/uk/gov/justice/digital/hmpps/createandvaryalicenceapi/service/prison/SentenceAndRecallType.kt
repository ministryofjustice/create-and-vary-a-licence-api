package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

data class BookingSentenceAndRecallTypes(
  val bookingId: Long,
  val sentenceTypeRecallTypes: List<SentenceAndRecallType>,
)

data class SentenceAndRecallType(
  val sentenceType: String,
  val recallType: RecallType,
)

data class RecallType(
  val recallName: String,
  val isStandardRecall: Boolean,
  val isFixedTermRecall: Boolean,
)
