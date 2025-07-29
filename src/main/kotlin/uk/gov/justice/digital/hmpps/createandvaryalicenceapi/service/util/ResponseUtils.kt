package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

class ResponseUtils {
  companion object {

    private val log = LoggerFactory.getLogger(this::class.java)

    @JvmStatic
    fun <T> processErrors(it: Throwable): Mono<T> = when {
      it is WebClientResponseException && it.statusCode == HttpStatus.NOT_FOUND -> {
        log.info("No resource found ${it.message}")
        Mono.empty()
      }
      else -> Mono.error(it)
    }
  }
}
