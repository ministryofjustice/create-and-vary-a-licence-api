package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

private const val HMPPS_AUTH = "hmpps-auth"

@Configuration
class WebClientConfiguration(
  @Value("\${hmpps.auth.url}") private val oauthApiUrl: String,
  @Value("\${hmpps.prison.api.url}") private val prisonApiUrl: String,
  @Value("\${hmpps.prisonregister.api.url}") private val prisonRegisterApiUrl: String,
  @Value("\${hmpps.delius.api.url}") private val deliusApiUrl: String,
  @Value("\${hmpps.prisonersearch.api.url}") private val prisonerSearchApiUrl: String,
  @Value("\${hmpps.document.api.url}") private val documentApiUrl: String,
  @Value("\${hmpps.govuk.api.url}") private val govUkApiUrl: String,
  @Value("\${hmpps.hdc.api.url}") private val hdcApiUrl: String,
  @Value("\${os.places.api.url}") private val osPlacesApiUrl: String,
) {
  @Bean
  fun oauthApiHealthWebClient(): WebClient = WebClient.builder().baseUrl(oauthApiUrl).build()

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService,
  ): OAuth2AuthorizedClientManager? {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager =
      AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, oAuth2AuthorizedClientService)
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }

  @Bean
  fun oauthPrisonClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = getOAuthClient(authorizedClientManager)
    return getWebClient(prisonApiUrl, oauth2Client)
  }

  @Bean
  fun oauthPrisonerSearchClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = getOAuthClient(authorizedClientManager)
    return getWebClient(prisonerSearchApiUrl, oauth2Client)
  }

  @Bean
  fun oauthDocumentApiClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = getOAuthClient(authorizedClientManager)
    return getWebClient(documentApiUrl, oauth2Client)
  }

  @Bean
  fun prisonRegisterApiWebClient(): WebClient = getWebClient(prisonRegisterApiUrl)

  @Bean
  fun oauthDeliusApiClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = getOAuthClient(authorizedClientManager)
    return getWebClient(deliusApiUrl, oauth2Client)
  }

  @Bean
  fun govUkWebClient(): WebClient = WebClient.builder().baseUrl(govUkApiUrl).build()

  @Bean
  fun oauthHdcApiClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = getOAuthClient(authorizedClientManager)
    return getWebClient(hdcApiUrl, oauth2Client)
  }

  @Bean
  fun osPlacesClient(): WebClient = getWebClient(osPlacesApiUrl)

  private fun getOAuthClient(authorizedClientManager: OAuth2AuthorizedClientManager): ServletOAuth2AuthorizedClientExchangeFilterFunction {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId(HMPPS_AUTH)
    return oauth2Client
  }

  private fun getWebClient(url: String, oauth2Client: ServletOAuth2AuthorizedClientExchangeFilterFunction): WebClient = WebClient.builder()
    .baseUrl(url)
    .apply(oauth2Client.oauth2Configuration())
    .exchangeStrategies(
      ExchangeStrategies.builder()
        .codecs { configurer ->
          configurer.defaultCodecs()
            .maxInMemorySize(-1)
        }
        .build(),
    ).build()

  private fun getWebClient(url: String): WebClient = WebClient.builder()
    .baseUrl(url)
    .exchangeStrategies(
      ExchangeStrategies.builder()
        .codecs { configurer ->
          configurer.defaultCodecs()
            .maxInMemorySize(-1)
        }
        .build(),
    ).build()
}
