package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import com.fasterxml.jackson.annotation.JsonView
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.AuthorizationServiceException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Views

@RestControllerAdvice
class ControllerAdvice {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
    log.info("Access denied exception: {}", e.message, e)
    return ResponseEntity
      .status(HttpStatus.FORBIDDEN)
      .body(
        ErrorResponse(
          status = HttpStatus.FORBIDDEN.value(),
          userMessage = "Authentication problem. Check token and roles - ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(AuthorizationServiceException::class)
  fun handleAuthorizationServiceException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
    log.info("Auth service exception: {}", e.message, e)
    return ResponseEntity
      .status(HttpStatus.UNAUTHORIZED)
      .body(
        ErrorResponse(
          status = HttpStatus.UNAUTHORIZED.value(),
          userMessage = "Authentication problem. Check token and roles - ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(RestClientResponseException::class)
  fun handleRestClientException(e: RestClientResponseException): ResponseEntity<ErrorResponse> {
    log.error("RestClientResponseException: {}", e.message, e)
    return ResponseEntity
      .status(e.statusCode)
      .body(
        ErrorResponse(
          status = e.statusCode.value(),
          userMessage = "Rest client exception ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(WebClientResponseException::class)
  fun handleWebClientException(e: WebClientResponseException): ResponseEntity<ErrorResponse> {
    log.error("RestClientResponseException: {}, response: {}", e.message, e.responseBodyAsString, e)
    return ResponseEntity
      .status(e.statusCode)
      .body(
        ErrorResponse(
          status = e.statusCode.value(),
          userMessage = "Rest client exception ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(RestClientException::class)
  fun handleRestClientException(e: RestClientException): ResponseEntity<ErrorResponse> {
    log.error("RestClientException: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
          userMessage = "Rest client exception ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleEntityNotFoundException(e: EntityNotFoundException): ResponseEntity<ErrorResponse> {
    log.info("Entity not found exception: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND.value(),
          userMessage = "Not found: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleRequestUnreadableException(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
    log.info("Message not readable exception: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(
        ErrorResponse(
          status = HttpStatus.BAD_REQUEST.value(),
          userMessage = "Bad request: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(ValidationException::class, MethodArgumentNotValidException::class)
  fun handleValidationException(e: Exception): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(
        ErrorResponse(
          status = HttpStatus.BAD_REQUEST.value(),
          userMessage = "Validation failure: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse?>? {
    log.error("Unexpected exception: {}", e.message, e)
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = HttpStatus.INTERNAL_SERVER_ERROR,
          userMessage = "Unexpected error: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }
}

data class ErrorResponse(
  @get:JsonView(Views.SubjectAccessRequest::class)
  val status: Int,
  @get:JsonView(Views.SubjectAccessRequest::class)
  val errorCode: Int? = null,
  @get:JsonView(Views.SubjectAccessRequest::class)
  val userMessage: String? = null,
  @get:JsonView(Views.SubjectAccessRequest::class)
  val developerMessage: String? = null,
  @get:JsonView(Views.SubjectAccessRequest::class)
  val moreInfo: String? = null,
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    moreInfo: String? = null,
  ) :
    this(status.value(), errorCode, userMessage, developerMessage, moreInfo)
}
