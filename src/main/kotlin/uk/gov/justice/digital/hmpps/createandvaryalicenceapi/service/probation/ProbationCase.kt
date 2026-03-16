package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.InvalidStateException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.corePersonRecord.PrisonCanonicalRecord

data class ProbationCase(
  val crn: String,
  val nomisId: String? = null,
  val croNumber: String? = null,
  val pncNumber: String? = null,
) {
  companion object {
    fun from(prisonCanonicalRecord: PrisonCanonicalRecord): ProbationCase {
      val prisonNumber = prisonCanonicalRecord.identifiers.prisonNumbers.firstOrNull()
      val crn = prisonCanonicalRecord.identifiers.crns.firstOrNull()
        ?: throw InvalidStateException("CRN not found for prison record in core person record: $prisonNumber")
      return ProbationCase(
        crn = crn,
        nomisId = prisonNumber,
        croNumber = prisonCanonicalRecord.identifiers.cros.firstOrNull(),
        pncNumber = prisonCanonicalRecord.identifiers.pncs.firstOrNull(),
      )
    }
  }
}
