package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.service.GlobalPrincipalOAuth2AuthorizedClientService

private const val HMPPS_AUTH = "hmpps-auth"

@Configuration
class WebClientConfiguration(
  @param:Value("\${hmpps.auth.url}") private val oauthApiUrl: String,
  @param:Value("\${hmpps.prison.api.url}") private val prisonApiUrl: String,
  @param:Value("\${hmpps.prisonregister.api.url}") private val prisonRegisterApiUrl: String,
  @param:Value("\${hmpps.delius.api.url}") private val deliusApiUrl: String,
  @param:Value("\${hmpps.workload.api.url}") private val workLoadApiUrl: String,
  @param:Value("\${hmpps.prisonersearch.api.url}") private val prisonerSearchApiUrl: String,
  @param:Value("\${hmpps.manageusers.api.url}") private val manageUsersApiUrl: String,
  @param:Value("\${hmpps.document.api.url}") private val documentApiUrl: String,
  @param:Value("\${hmpps.govuk.api.url}") private val govUkApiUrl: String,
  @param:Value("\${hmpps.hdc.api.url}") private val hdcApiUrl: String,
  @param:Value("\${os.places.api.url}") private val osPlacesApiUrl: String,
) {
  @Bean
  fun oauthApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.baseUrl(oauthApiUrl).build()

  @Bean
  fun govUkWebClient(builder: WebClient.Builder): WebClient = builder.baseUrl(govUkApiUrl).build()

  @Bean
  fun prisonRegisterApiWebClient(builder: WebClient.Builder): WebClient = builder.baseUrl(prisonRegisterApiUrl).build()

  @Bean
  fun osPlacesClient(builder: WebClient.Builder): WebClient = builder.baseUrl(osPlacesApiUrl).build()

  @Bean
  fun oauthPrisonClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = getWebClient(prisonApiUrl, authorizedClientManager, builder)

  @Bean
  fun oauthManageUsersWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = getWebClient(manageUsersApiUrl, authorizedClientManager, builder)

  @Bean
  fun oauthPrisonerSearchClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = getWebClient(prisonerSearchApiUrl, authorizedClientManager, builder)

  @Bean
  fun oauthDocumentApiClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = getWebClient(documentApiUrl, authorizedClientManager, builder)

  @Bean
  fun oauthDeliusApiClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = getWebClient(deliusApiUrl, authorizedClientManager, builder)

  @Bean
  fun oauthWorkLoadApiClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = getWebClient(workLoadApiUrl, authorizedClientManager, builder)

  @Bean
  fun oauthHdcApiClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = getWebClient(hdcApiUrl, authorizedClientManager, builder)

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository,
  ): OAuth2AuthorizedClientManager {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
      clientRegistrationRepository,
      GlobalPrincipalOAuth2AuthorizedClientService(clientRegistrationRepository),
    )
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }

  private fun getWebClient(
    url: String,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager = authorizedClientManager,
    url = url,
    registrationId = HMPPS_AUTH,
  )
}
