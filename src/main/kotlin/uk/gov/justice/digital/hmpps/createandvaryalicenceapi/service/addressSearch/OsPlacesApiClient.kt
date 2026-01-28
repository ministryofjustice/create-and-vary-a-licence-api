package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class OsPlacesApiClient(
  @param:Qualifier("osPlacesClient") private val osPlacesApiWebClient: WebClient,
  @param:Value("\${os.places.api.key}") private val apiKey: String,
) {
  fun searchForAddressesByText(pageable: PageRequest, searchQuery: String): List<DeliveryPointAddress> {
    val escapedSearchQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8)

    val searchResult = osPlacesApiWebClient
      .get()
      .uri("/find?query=$escapedSearchQuery&key=$apiKey&offset=${pageable.offset}&maxresults=${pageable.pageSize}&lr=EN")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .onStatus({ it.is4xxClientError }) { response ->
        response.bodyToMono(String::class.java)
          .flatMap { body ->
            Mono.error(RuntimeException("400 from OS Places: $body"))
          }
      }
      .bodyToMono(OsPlacesApiResponse::class.java)
      .block()

    return searchResult?.results?.map { it.dpa } ?: emptyList()
  }

  fun searchForAddressByReference(reference: String): DeliveryPointAddress {
    val searchResult = osPlacesApiWebClient
      .get()
      .uri("/uprn?uprn=$reference&key=$apiKey")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(OsPlacesApiResponse::class.java)
      .block()
    return searchResult?.results?.map { it.dpa }?.get(0) ?: error("Could not find an address with uprn: $reference")
  }
}
