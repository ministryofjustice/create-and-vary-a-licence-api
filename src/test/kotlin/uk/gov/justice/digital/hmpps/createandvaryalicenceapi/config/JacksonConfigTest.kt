package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerHdcStatus

class JacksonConfigTest {

  private val config = JacksonConfig()

  private fun createObjectMapper(): ObjectMapper {
    val builder = JsonMapper.builder()
    config.customizer().customize(builder)
    return builder.build()
  }

  @Test
  fun shouldDeserializeWithoutPassedFieldAndDefaultToFalse() {
    // Given
    val json = """
      [
        {
          "approvalStatus": "APPROVED",
          "approvalStatusDate": "2024-01-01",
          "bookingId": 123,
          "checksPassedDate": "2024-01-02",
          "refusedReason": "NONE"
        }
      ]
    """.trimIndent()

    val objectMapper = createObjectMapper()

    // When
    val result = objectMapper.readValue(
      json,
      object : TypeReference<List<PrisonerHdcStatus>>() {},
    )

    // Then
    assertThat(result).hasSize(1)

    with(result[0]) {
      assertThat(approvalStatus).isEqualTo("APPROVED")
      assertThat(approvalStatusDate).isEqualTo("2024-01-01")
      assertThat(bookingId).isEqualTo(123L)
      assertThat(checksPassedDate).isEqualTo("2024-01-02")
      assertThat(passed).isFalse()
      assertThat(refusedReason).isEqualTo("NONE")
    }
  }

  @Test
  fun shouldDeserializeArrayAndDefaultPassedToFalseForAllItems() {
    // Given
    val json = """
      [
        {
          "approvalStatus": "APPROVED",
          "approvalStatusDate": "2024-01-01",
          "bookingId": 123,
          "checksPassedDate": "2024-01-02",
          "refusedReason": "NONE"
        },
        {
          "approvalStatus": "REFUSED",
          "approvalStatusDate": "2024-02-01",
          "bookingId": 456,
          "checksPassedDate": "2024-02-02",
          "refusedReason": "SOME_REASON"
        }
      ]
    """.trimIndent()

    val objectMapper = createObjectMapper()

    // When
    val result = objectMapper.readValue(
      json,
      object : TypeReference<List<PrisonerHdcStatus>>() {},
    )

    // Then
    assertThat(result).hasSize(2)

    with(result[0]) {
      assertThat(approvalStatus).isEqualTo("APPROVED")
      assertThat(approvalStatusDate).isEqualTo("2024-01-01")
      assertThat(bookingId).isEqualTo(123L)
      assertThat(checksPassedDate).isEqualTo("2024-01-02")
      assertThat(passed).isFalse()
      assertThat(refusedReason).isEqualTo("NONE")
    }

    with(result[1]) {
      assertThat(approvalStatus).isEqualTo("REFUSED")
      assertThat(approvalStatusDate).isEqualTo("2024-02-01")
      assertThat(bookingId).isEqualTo(456L)
      assertThat(checksPassedDate).isEqualTo("2024-02-02")
      assertThat(passed).isFalse()
      assertThat(refusedReason).isEqualTo("SOME_REASON")
    }
  }
}
