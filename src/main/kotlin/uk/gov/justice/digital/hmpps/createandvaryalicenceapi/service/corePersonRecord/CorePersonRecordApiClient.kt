package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.corePersonRecord

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class CorePersonRecordApiClient(@param:Qualifier("oauthCorePersonRecordApiClient") val corePersonRecordApiClient: WebClient) {
  fun getPersonRecord(prisonNumber: String) = corePersonRecordApiClient
    .get()
    .uri("/person/prison/$prisonNumber")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    // .bodyToMono<String>()
    .bodyToMono<PrisonCanonicalRecord>()
    .block() ?: error("null response while getting core person record for $prisonNumber")
  // println(personRecord)
  // return PrisonCanonicalRecord(identifiers = CanonicalIdentifiers(prisonNumbers = listOf(prisonNumber)))
  // return personRecord
}
