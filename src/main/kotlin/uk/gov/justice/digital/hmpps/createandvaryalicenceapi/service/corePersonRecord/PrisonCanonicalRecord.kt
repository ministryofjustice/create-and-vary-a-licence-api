package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.corePersonRecord

data class PrisonCanonicalRecord(
  val cprUUID: String? = null,
  val identifiers: CanonicalIdentifiers,
)

data class CanonicalIdentifiers(
  val crns: List<String> = emptyList(),
  val prisonNumbers: List<String> = emptyList(),
  val pncs: List<String> = emptyList(),
  val cros: List<String> = emptyList(),
)
