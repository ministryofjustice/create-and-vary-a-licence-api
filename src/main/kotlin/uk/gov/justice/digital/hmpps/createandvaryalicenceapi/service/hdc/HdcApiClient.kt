package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference

@Service
class HdcApiClient(@Qualifier("oauthHdcApiClient") val hdcApiWebClient: WebClient) {

  fun getByBookingId(bookingId: Long): HdcLicence? {
    return hdcApiWebClient
      .get()
      .uri("/licence/hdc/$bookingId")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(typeReference<HdcLicence>())
      .block()
  }
}
