package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util.ResponseUtils.propagateAny404

@Service
class HdcApiClient(@param:Qualifier("oauthHdcApiClient") val hdcApiWebClient: WebClient) {

  fun getByBookingId(bookingId: Long): HdcLicenceData = hdcApiWebClient
    .get()
    .uri("/licence/hdc/$bookingId")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono(HdcLicenceData::class.java)
    .propagateAny404 { "No licence data found for $bookingId" }
    .block()!!
}
