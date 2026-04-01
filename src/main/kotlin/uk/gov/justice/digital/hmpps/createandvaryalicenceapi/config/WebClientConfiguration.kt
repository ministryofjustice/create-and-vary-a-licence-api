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
private const val MAX_IN_MEMORY_SIZE = 10485760 // 10 MB
private const val MAX_IN_MEMORY_PRISONER_SEARCH_SIZE = 12582912 // 12 MB

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
  @param:Value("\${hmpps.corepersonrecord.api.url}") private val corePersonRecordApiUrl: String,
  @param:Value("\${os.places.api.url}") private val osPlacesApiUrl: String,
) {

  @Bean
  fun oauthApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.baseUrl(oauthApiUrl)
    .codecs { it.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE) }
    .build()

  @Bean
  fun govUkWebClient(builder: WebClient.Builder): WebClient = builder.baseUrl(govUkApiUrl)
    .codecs { it.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE) }
    .build()

  @Bean
  fun prisonRegisterApiWebClient(builder: WebClient.Builder): WebClient = builder.baseUrl(prisonRegisterApiUrl)
    .codecs { it.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE) }
    .build()

  @Bean
  fun osPlacesClient(builder: WebClient.Builder): WebClient = builder.baseUrl(osPlacesApiUrl)
    .codecs { it.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE) }
    .build()

  @Bean
  fun oauthPrisonClient(
    authorizedClientManagerCvl: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = getWebClient(prisonApiUrl, authorizedClientManagerCvl, builder)

  @Bean
  fun oauthManageUsersWebClient(
    authorizedClientManagerCvl: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = getWebClient(manageUsersApiUrl, authorizedClientManagerCvl, builder)

  @Bean
  fun oauthPrisonerSearchClient(
    authorizedClientManagerCvl: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = getWebClient(
    prisonerSearchApiUrl,
    authorizedClientManagerCvl,
    builder,
    maxInMemorySize = MAX_IN_MEMORY_PRISONER_SEARCH_SIZE,
  )

  @Bean
  fun oauthDocumentApiClient(
    authorizedClientManagerCvl: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = getWebClient(documentApiUrl, authorizedClientManagerCvl, builder)

  @Bean
  fun oauthDeliusApiClient(
    authorizedClientManagerCvl: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = getWebClient(deliusApiUrl, authorizedClientManagerCvl, builder)

  @Bean
  fun oauthWorkLoadApiClient(
    authorizedClientManagerCvl: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = getWebClient(workLoadApiUrl, authorizedClientManagerCvl, builder)

  @Bean
  fun oauthHdcApiClient(
    authorizedClientManagerCvl: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = getWebClient(hdcApiUrl, authorizedClientManagerCvl, builder)

  @Bean
  fun oauthCorePersonRecordApiClient(
    authorizedClientManagerCvl: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = getWebClient(corePersonRecordApiUrl, authorizedClientManagerCvl, builder)

  @Bean
  fun authorizedClientManagerCvl(
    clientRegistrationRepository: ClientRegistrationRepository,
  ): OAuth2AuthorizedClientManager {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
      .clientCredentials()
      .build()
    val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
      clientRegistrationRepository,
      GlobalPrincipalOAuth2AuthorizedClientService(clientRegistrationRepository),
    )
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }

  private fun getWebClient(
    url: String,
    authorizedClientManagerCvl: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    maxInMemorySize: Int = MAX_IN_MEMORY_SIZE,
  ): WebClient = builder
    .authorisedWebClient(
      authorizedClientManager = authorizedClientManagerCvl,
      url = url,
      registrationId = HMPPS_AUTH,
    )
    .mutate()
    .codecs { it.defaultCodecs().maxInMemorySize(maxInMemorySize) }
    .build()
}
