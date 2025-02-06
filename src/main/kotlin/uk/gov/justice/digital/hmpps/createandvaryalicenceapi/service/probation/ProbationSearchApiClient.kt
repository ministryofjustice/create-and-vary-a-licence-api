package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.LicenceCaseloadSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.OffenderSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.ProbationSearchSortByRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.Batching.batchRequests

@Component
class ProbationSearchApiClient(@Qualifier("oauthProbationSearchApiClient") val probationSearchApiClient: WebClient) {
  companion object {
    private const val BY_NOMS_ID_BATCH_SIZE = 500
    private const val BY_CRN_BATCH_SIZE = 500
  }

  fun searchLicenceCaseloadByTeam(
    query: String,
    teamCodes: List<String>,
    sortBy: List<ProbationSearchSortByRequest> = emptyList(),
  ): List<CaseloadResult> {
    if (teamCodes.isEmpty()) return emptyList()
    val sortOptions = sortBy.ifEmpty { listOf(ProbationSearchSortByRequest()) }

    val request = LicenceCaseloadSearchRequest(
      teamCodes,
      query,
      sortOptions,
      2000,
    )

    val probationOffenderSearchResponse = probationSearchApiClient
      .post()
      .uri("/licence-caseload/by-team")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(CaseloadResponse::class.java)
      .block()

    return probationOffenderSearchResponse?.content ?: error("Unexpected null response from API")
  }

  fun searchForPersonOnProbation(
    nomisId: String,
  ): OffenderDetail {
    val probationOffenderSearchResponse = probationSearchApiClient
      .post()
      .uri("/search")
      .bodyValue(OffenderSearchRequest(nomsNumber = nomisId))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(typeReference<List<OffenderDetail>>())
      .block()
    return probationOffenderSearchResponse?.get(0) ?: error("Unexpected null response from API")
  }

  fun searchForPeopleByNomsNumber(
    nomsIds: List<String>,
    batchSize: Int = BY_NOMS_ID_BATCH_SIZE,
  ) = batchRequests(batchSize, nomsIds) { batch ->
    probationSearchApiClient
      .post()
      .uri("/nomsNumbers")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(batch)
      .retrieve()
      .bodyToMono(typeReference<List<OffenderDetail>>())
      .block()
  }

  fun getOffendersByCrn(crns: List<String?>) =
    batchRequests(BY_CRN_BATCH_SIZE, crns) { batch ->
      probationSearchApiClient
        .post()
        .uri("/crns")
        .bodyValue(batch)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(typeReference<List<OffenderDetail>>())
        .block()
    }
}
