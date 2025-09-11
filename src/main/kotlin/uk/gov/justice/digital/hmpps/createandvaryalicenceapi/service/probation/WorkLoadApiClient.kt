package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.WorkLoadAllocationResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util.ResponseUtils.coerce404ToEmptyOrThrow

@Component
class WorkLoadApiClient(@param:Qualifier("oauthWorkLoadApiClient") val workLoadApiClient: WebClient) {

  fun getStaffDetails(personUuid: String): WorkLoadAllocationResponse? = workLoadApiClient
    .get()
    .uri("/allocation/person/$personUuid", personUuid)
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono(WorkLoadAllocationResponse::class.java)
    .coerce404ToEmptyOrThrow()
    .block()!!
}
