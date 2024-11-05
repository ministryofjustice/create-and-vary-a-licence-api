package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.web.reactive.function.client.WebClient

class ProbationSearchApiClientTest {

  private val client = mock<WebClient>()
  private val probationSearchApiClient = ProbationSearchApiClient(client)

  @BeforeEach
  fun reset() {
    reset(client)
  }

  @Test
  fun `search by teams endpoint not called if no teams`() {
    probationSearchApiClient.searchLicenceCaseloadByTeam("query", emptyList())

    verifyNoInteractions(client)
  }

  @Test
  fun `search by noms number endpoint not called if no teams`() {
    probationSearchApiClient.searchForPeopleByNomsNumber(emptyList())

    verifyNoInteractions(client)
  }

  @Test
  fun `get by CRN endpoint not called if no teams`() {
    probationSearchApiClient.getOffendersByCrn(emptyList())

    verifyNoInteractions(client)
  }
}
