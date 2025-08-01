package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class OsPlacesApiClient(
  @param:Qualifier("osPlacesClient") private val osPlacesApiWebClient: WebClient,
  @param:Value("\${os.places.api.key}") private val apiKey: String,
) {
  fun searchForAddressesByText(pageable: PageRequest, searchQuery: String): List<DeliveryPointAddress> {
    val searchResult = osPlacesApiWebClient
      .get()
      .uri("/find?query=$searchQuery&key=$apiKey&offset=${pageable.offset}&maxresults=${pageable.pageSize}")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
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
