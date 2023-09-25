package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.LicenceCaseloadSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.ProbationSearchSortByRequest

@Component
class ProbationSearchApiClient(@Qualifier("oauthProbationSearchApiClient") val probationSearchApiClient: WebClient) {

  fun searchLicenceCaseloadByTeam(
    query: String,
    teamCodes: List<String>,
    sortBy: List<ProbationSearchSortByRequest> = emptyList(),
  ): List<ProbationSearchResponseResult> {
    val sortOptions = sortBy.ifEmpty { listOf(ProbationSearchSortByRequest()) }

    val licenceCaseLoadRequestBody = LicenceCaseloadSearchRequest(
      teamCodes,
      query,
      sortOptions,
    )

    val probationOffenderSearchResponse = probationSearchApiClient
      .post()
      .uri("/licence-caseload/by-team")
      .bodyValue(licenceCaseLoadRequestBody)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(ProbationSearchResponse::class.java)
      .block()

    return probationOffenderSearchResponse?.content ?: error("Unexpected null response from API")
  }
}
