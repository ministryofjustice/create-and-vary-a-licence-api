package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationSearchResult

data class EnrichedProbationSearchResults(
  val results: List<ProbationSearchResult>,
  val inPrisonCount: Int,
  val onProbationCount: Int,
)
