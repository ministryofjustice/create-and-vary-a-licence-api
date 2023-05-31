package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.LicenceCaseloadSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.ProbationSearchSortByRequest

@Component
class ProbationSearchApiClient(@Qualifier("oauthProbationSearchApiClient") val probationSearchApiClient: WebClient) {

  fun searchLicenceCaseloadByTeam(query: String, teamCodes: List<String>): List<ProbationSearchResult> {

    val licenceCaseLoadRequestBody = LicenceCaseloadSearchRequest(
      teamCodes,
      query,
      ProbationSearchSortByRequest()
    )

    val probationOffenderSearchResponse = probationSearchApiClient
      .post()
      .uri("/licence-caseload/by-team")
      .bodyValue(licenceCaseLoadRequestBody)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(ProbationSearchResponse::class.java)
      .block()

    return probationOffenderSearchResponse?.content ?: throw IllegalStateException("Unexpected null response from API")
  }
}
