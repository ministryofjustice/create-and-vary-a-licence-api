package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.AuthorizationServiceException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.method.annotation.HandlerMethodValidationException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.InvalidStateException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.ResourceAlreadyExistsException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.DetailedValidationException

@RestControllerAdvice
class ControllerAdvice {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
    log.error("Method argument exception, validation failed: {}", ex.stackTraceToString())
    val message = ex.bindingResult.allErrors.map { it.defaultMessage }.filterNotNull().sorted().joinToString("; ")
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST.value(),
          userMessage = "Validation failed for one or more fields.",
          developerMessage = message,
        ),
      )
  }

  @ExceptionHandler(HandlerMethodValidationException::class)
  fun handleHandlerMethodValidationException(e: HandlerMethodValidationException): ResponseEntity<ErrorResponse> {
    val messages = e.parameterValidationResults
      .flatMap { it.resolvableErrors }
      .map { error ->
        val field = error.codes?.firstOrNull()?.substringAfterLast('.') ?: "unknown"
        "$field: ${error.defaultMessage}"
      }
    log.info("Handler method validation failed: {}", messages)

    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST.value(),
          userMessage = "Validation failed, One or more request fields are invalid",
          developerMessage = messages.joinToString("; "),
        ),
      )
  }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
    log.info("Access denied exception: {}", e.message)
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
    log.info("Auth service exception: {}", e.message)
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
    log.error("RestClientResponseException: {}", e.message)
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
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST.value(),
          userMessage = "Bad request: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST.value(),
          userMessage = "Validation failure: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(ResourceAlreadyExistsException::class)
  fun handleResourceAlreadyExists(e: ResourceAlreadyExistsException): ResponseEntity<EntityAlreadyExistsResponse> {
    log.info("ResourceAlreadyExistsException: {}, resource: {}", e.message, e.existingResourceId)
    return ResponseEntity
      .status(HttpStatus.CONFLICT)
      .body(
        EntityAlreadyExistsResponse(
          status = HttpStatus.CONFLICT.value(),
          userMessage = "Validation failure: ${e.message}",
          developerMessage = e.message,
          existingResourceId = e.existingResourceId,
        ),
      )
  }

  @ExceptionHandler(InvalidStateException::class)
  fun handleInvalidStateException(e: Exception): ResponseEntity<ErrorResponse?>? {
    log.error("InvalidStateException: ${e.message}")
    return ResponseEntity
      .status(HttpStatus.UNPROCESSABLE_ENTITY)
      .body(
        ErrorResponse(
          status = HttpStatus.UNPROCESSABLE_ENTITY,
          userMessage = "Unexpected error: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(DetailedValidationException::class)
  fun handleDetailedValidationException(exception: DetailedValidationException): ResponseEntity<ProblemDetail> {
    val problemDetail = ProblemDetail.forStatus(BAD_REQUEST)
    problemDetail.title = exception.title
    problemDetail.detail = exception.message
    exception.errors.forEach { problemDetail.setProperty(it.key, it.value) }
    return ResponseEntity.status(BAD_REQUEST).body(problemDetail)
  }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse?>? {
    log.error("Unexpected exception: {}", e.stackTraceToString())
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
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
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

data class EntityAlreadyExistsResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
  val moreInfo: String? = null,
  val existingResourceId: Long,

) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    moreInfo: String? = null,
    existingResourceId: Long,
  ) :
    this(status.value(), errorCode, userMessage, developerMessage, moreInfo, existingResourceId)
}
