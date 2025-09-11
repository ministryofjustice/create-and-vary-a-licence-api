package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

object ResponseUtils {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun <T> Mono<T>.coerce404ToEmptyOrThrow() = this.onErrorResume {
    when {
      it is WebClientResponseException && it.statusCode == HttpStatus.NOT_FOUND -> {
        val uri = it.request?.uri?.toString() ?: "Unknown"
        log.info("No resource found for URI: $uri")
        Mono.empty()
      }

      else -> Mono.error(it)
    }
  }

  fun <T> Mono<T>.propagateAny404(message: () -> String) = this.onErrorResume {
    when {
      it is WebClientResponseException && it.statusCode == HttpStatus.NOT_FOUND -> {
        log.info("No resource found when calling ${it.request?.uri}")
        Mono.error(EntityNotFoundException(message(), it))
      }

      else -> Mono.error(it)
    }
  }

  fun WebClient.ResponseSpec.rethrowAnyHttpErrorWithContext(message: (response: ClientResponse, body: String?) -> String) = this.onStatus(HttpStatusCode::isError) { response ->
    response.bodyToMono<String>().map { body ->
      RuntimeException(message(response, body))
    }.switchIfEmpty(
      Mono.error {
        RuntimeException(message(response, null))
      },
    )
  }
}
