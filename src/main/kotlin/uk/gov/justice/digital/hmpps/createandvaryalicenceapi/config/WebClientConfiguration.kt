package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(
  @Value("\${hmpps.auth.url}") private val oauthApiUrl: String,
  @Value("\${hmpps.prison.api.url}") private val prisonApiUrl: String,
  @Value("\${oauth.client.token-url}") private val oauthTokenUrl: String,
  @Value("\${oauth.client.id}") private val oauthClientId: String,
  @Value("\${oauth.client.secret}") private val oauthSecret: String,
) {

  @Bean
  fun oauthApiHealthWebClient(): WebClient {
    return WebClient.builder().baseUrl(oauthApiUrl).build()
  }

  @Bean
  fun oauthPrisonClient(): WebClient {
    val clientRegistryRepo = InMemoryReactiveClientRegistrationRepository(
      ClientRegistration
        .withRegistrationId("hmpps-auth")
        .tokenUri(oauthTokenUrl)
        .clientId(oauthClientId)
        .clientSecret(oauthSecret)
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .build()
    )

    val clientService = InMemoryReactiveOAuth2AuthorizedClientService(clientRegistryRepo)

    val authorizedClientManager =
      AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistryRepo, clientService)

    val oauthFilter = ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauthFilter.setDefaultClientRegistrationId("hmpps-auth")

    return WebClient.builder().baseUrl(prisonApiUrl)
      .filter(oauthFilter)
      .build()
  }
}
