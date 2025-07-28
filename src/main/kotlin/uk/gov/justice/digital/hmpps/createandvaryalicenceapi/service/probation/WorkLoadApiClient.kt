package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.WorkLoadAllocationResponse

@Component
class WorkLoadApiClient(@Qualifier("oauthWorkLoadApiClient") val workLoadApiClient: WebClient) {

  fun getStaffDetails(personUuid: String): WorkLoadAllocationResponse? = workLoadApiClient
    .get()
    .uri("/allocation/person/$personUuid", personUuid)
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono(WorkLoadAllocationResponse::class.java)
    .onErrorResume {
      processErrors(it)
    }
    .block()

  private fun <T> processErrors(it: Throwable): Mono<T> = when {
    it is WebClientResponseException && it.statusCode == HttpStatus.NOT_FOUND -> {
      Mono.empty()
    }
    else -> Mono.error(it)
  }
}
