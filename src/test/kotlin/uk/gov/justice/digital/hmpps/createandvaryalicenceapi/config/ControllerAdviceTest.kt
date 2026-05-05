package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.noRetryExceptions.ExistingCvlLicenceException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.noRetryExceptions.LicenceAlreadyMigratedException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.noRetryExceptions.OffenderManagerNotFoundException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.InvalidStateException

class ControllerAdviceTest {
  private val controllerAdvice = ControllerAdvice()

  @Test
  fun handleInvalidStateException() {
    val exception = InvalidStateException("Delius record not found for nomisId: 1234567890")

    val response = controllerAdvice.handleInvalidStateException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
    assertThat(response.body).isEqualTo(
      ErrorResponse(
        status = HttpStatus.UNPROCESSABLE_ENTITY,
        userMessage = "Unexpected error: ${exception.message}",
        developerMessage = exception.message,
      ),
    )
  }

  @Test
  fun handleExistingCvlLicenceException() {
    val exception = ExistingCvlLicenceException("A2345BC")

    val response = controllerAdvice.handleNoRetryMigrationLicenceException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(response.body).isEqualTo(
      ErrorResponse(
        status = HttpStatus.BAD_REQUEST,
        userMessage = "Unexpected error: ${exception.message}",
        developerMessage = exception.message,
      ),
    )
  }

  @Test
  fun handleLicenceAlreadyMigratedException() {
    val exception = LicenceAlreadyMigratedException(123)

    val response = controllerAdvice.handleNoRetryMigrationLicenceException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(response.body).isEqualTo(
      ErrorResponse(
        status = HttpStatus.BAD_REQUEST,
        userMessage = "Unexpected error: ${exception.message}",
        developerMessage = exception.message,
      ),
    )
  }

  @Test
  fun handleOffenderManagerNotFoundException() {
    val exception = OffenderManagerNotFoundException("A2345BC")

    val response = controllerAdvice.handleNoRetryMigrationLicenceException(exception)

    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(response.body).isEqualTo(
      ErrorResponse(
        status = HttpStatus.BAD_REQUEST,
        userMessage = "Unexpected error: ${exception.message}",
        developerMessage = exception.message,
      ),
    )
  }
}
