package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class ControllerAdviceTest {
  private val controllerAdvice = ControllerAdvice()

  @Test
  fun handleInvalidStateException() {
    val exception = IllegalStateException("Delius record not found for nomisId: 1234567890")

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
}
