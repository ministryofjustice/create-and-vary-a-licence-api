package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference

@Component
class PrisonRegisterApiClient(@Qualifier("prisonRegisterApiWebClient") val prisonRegisterApiWebClient: WebClient) {

  fun getPrisonIds(): List<Prison> {
    return prisonRegisterApiWebClient
      .get()
      .uri("/prisons")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(typeReference<List<Prison>>())
      .block() ?: emptyList()
  }
}
