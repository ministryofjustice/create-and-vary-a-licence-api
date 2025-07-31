package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

object ResponseUtils {

  private val log = LoggerFactory.getLogger(this::class.java)

  @JvmStatic
  fun <T> coerce404ToEmptyOrThrow(it: Throwable): Mono<T> = when {
    it is WebClientResponseException && it.statusCode == HttpStatus.NOT_FOUND -> {
      val uri = it.request?.uri?.toString() ?: "Unknown"
      log.info("No resource found for URI: $uri")
      Mono.empty()
    }

    else -> Mono.error(it)
  }
}
