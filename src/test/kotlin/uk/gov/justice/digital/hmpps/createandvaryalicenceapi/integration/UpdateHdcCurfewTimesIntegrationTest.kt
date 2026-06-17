package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions.HdcApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateFirstNightCurfewTimesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateWeeklyCurfewTimesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import java.time.DayOfWeek
import java.time.LocalTime

class UpdateHdcCurfewTimesIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Test
  @Sql(
    "classpath:test_data/seed-hdc-licence-id-1.sql",
    "classpath:test_data/seed-hdc-curfew-hours.sql",
  )
  fun `Update the weekly curfew times`() {
    hdcApiMockServer.stubGetHdcLicenceData(54321L)

    webTestClient.put()
      .uri("/licence/id/1/hdc-weekly-curfew-times")
      .bodyValue(anUpdateWeeklyCurfewTimesRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = testRepository.findWeeklyCurfewTimes(1)

    assertThat(result)
      .extracting<Tuple> { tuple(it.fromDay, it.fromTime, it.untilDay, it.untilTime) }
      .containsAll(
        listOf(
          tuple(DayOfWeek.MONDAY, LocalTime.of(20, 0), DayOfWeek.TUESDAY, LocalTime.of(8, 0)),
          tuple(DayOfWeek.TUESDAY, LocalTime.of(20, 0), DayOfWeek.WEDNESDAY, LocalTime.of(8, 0)),
          tuple(DayOfWeek.WEDNESDAY, LocalTime.of(20, 0), DayOfWeek.THURSDAY, LocalTime.of(8, 0)),
          tuple(DayOfWeek.THURSDAY, LocalTime.of(20, 0), DayOfWeek.FRIDAY, LocalTime.of(8, 0)),
          tuple(DayOfWeek.FRIDAY, LocalTime.of(20, 0), DayOfWeek.SATURDAY, LocalTime.of(8, 0)),
          tuple(DayOfWeek.SATURDAY, LocalTime.of(20, 0), DayOfWeek.SUNDAY, LocalTime.of(8, 0)),
          tuple(DayOfWeek.SUNDAY, LocalTime.of(20, 0), DayOfWeek.MONDAY, LocalTime.of(8, 0)),
        ),
      )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-hdc-variation-licence-id-1.sql",
    "classpath:test_data/seed-hdc-curfew-hours.sql",
  )
  fun `Update curfew times for HdcVariationLicence`() {
    hdcApiMockServer.stubGetHdcLicenceData(54321L)

    webTestClient.put()
      .uri("/licence/id/1/hdc-weekly-curfew-times")
      .bodyValue(anUpdateWeeklyCurfewTimesRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = testRepository.findWeeklyCurfewTimes(1)

    assertThat(result)
      .extracting<Tuple> { tuple(it.fromDay, it.fromTime, it.untilDay, it.untilTime) }
      .containsAll(
        listOf(
          tuple(DayOfWeek.MONDAY, LocalTime.of(20, 0), DayOfWeek.TUESDAY, LocalTime.of(8, 0)),
          tuple(DayOfWeek.TUESDAY, LocalTime.of(20, 0), DayOfWeek.WEDNESDAY, LocalTime.of(8, 0)),
          tuple(DayOfWeek.WEDNESDAY, LocalTime.of(20, 0), DayOfWeek.THURSDAY, LocalTime.of(8, 0)),
          tuple(DayOfWeek.THURSDAY, LocalTime.of(20, 0), DayOfWeek.FRIDAY, LocalTime.of(8, 0)),
          tuple(DayOfWeek.FRIDAY, LocalTime.of(20, 0), DayOfWeek.SATURDAY, LocalTime.of(8, 0)),
          tuple(DayOfWeek.SATURDAY, LocalTime.of(20, 0), DayOfWeek.SUNDAY, LocalTime.of(8, 0)),
          tuple(DayOfWeek.SUNDAY, LocalTime.of(20, 0), DayOfWeek.MONDAY, LocalTime.of(8, 0)),
        ),
      )
  }

  @Test
  @Sql("classpath:test_data/seed-licence-id-1.sql")
  fun `Should return 500 with error message when licence does not support curfew updates`() {
    webTestClient.put()
      .uri("/licence/id/1/hdc-weekly-curfew-times")
      .bodyValue(anUpdateWeeklyCurfewTimesRequest)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().is5xxServerError
      .expectBody()
      .jsonPath("$.userMessage")
      .value<String> { assertThat(it).isEqualTo("Unexpected error: Licence CrdLicence does not support weekly curfew updates") }
  }

  @Test
  fun `Should return 404 when licence not found`() {
    webTestClient.put()
      .uri("/licence/id/9999/hdc-weekly-curfew-times")
      .bodyValue(anUpdateWeeklyCurfewTimesRequest)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  @Sql(
    "classpath:test_data/seed-hdc-licence-id-1.sql",
    "classpath:test_data/seed-hdc-curfew-hours.sql",
  )
  fun `Update the first night curfew times`() {
    hdcApiMockServer.stubGetHdcLicenceData(54321L)
    hdcApiMockServer.stubGetHdcLicenceData(54322L)

    webTestClient.put()
      .uri("/licence/id/1/hdc-first-night-curfew-times")
      .bodyValue(anUpdateFirstNightCurfewTimesRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = testRepository.findFirstNightCurfewTimes(1)

    assertThat(result!!)
      .extracting("fromTime", "untilTime")
      .containsExactly(LocalTime.of(20, 0), LocalTime.of(8, 0))
  }

  private companion object {
    val anUpdateWeeklyCurfewTimesRequest = UpdateWeeklyCurfewTimesRequest(
      listOf(
        CurfewTimes(
          curfewTimesSequence = 1,
          fromDay = DayOfWeek.MONDAY,
          fromTime = LocalTime.of(20, 0),
          untilDay = DayOfWeek.TUESDAY,
          untilTime = LocalTime.of(8, 0),
        ),
        CurfewTimes(
          curfewTimesSequence = 2,
          fromDay = DayOfWeek.TUESDAY,
          fromTime = LocalTime.of(20, 0),
          untilDay = DayOfWeek.WEDNESDAY,
          untilTime = LocalTime.of(8, 0),
        ),
        CurfewTimes(
          curfewTimesSequence = 3,
          fromDay = DayOfWeek.WEDNESDAY,
          fromTime = LocalTime.of(20, 0),
          untilDay = DayOfWeek.THURSDAY,
          untilTime = LocalTime.of(8, 0),
        ),
        CurfewTimes(
          curfewTimesSequence = 4,
          fromDay = DayOfWeek.THURSDAY,
          fromTime = LocalTime.of(20, 0),
          untilDay = DayOfWeek.FRIDAY,
          untilTime = LocalTime.of(8, 0),
        ),
        CurfewTimes(
          curfewTimesSequence = 5,
          fromDay = DayOfWeek.FRIDAY,
          fromTime = LocalTime.of(20, 0),
          untilDay = DayOfWeek.SATURDAY,
          untilTime = LocalTime.of(8, 0),
        ),
        CurfewTimes(
          curfewTimesSequence = 6,
          fromDay = DayOfWeek.SATURDAY,
          fromTime = LocalTime.of(20, 0),
          untilDay = DayOfWeek.SUNDAY,
          untilTime = LocalTime.of(8, 0),
        ),
        CurfewTimes(
          curfewTimesSequence = 7,
          fromDay = DayOfWeek.SUNDAY,
          fromTime = LocalTime.of(20, 0),
          untilDay = DayOfWeek.MONDAY,
          untilTime = LocalTime.of(8, 0),
        ),
      ),
    )

    val anUpdateFirstNightCurfewTimesRequest = UpdateFirstNightCurfewTimesRequest(
      CurfewTimes(
        curfewTimesSequence = 1,
        fromDay = DayOfWeek.MONDAY,
        fromTime = LocalTime.of(20, 0),
        untilDay = DayOfWeek.TUESDAY,
        untilTime = LocalTime.of(8, 0),
      ),
    )

    @RegisterExtension
    val hdcApiMockServer = HdcApiMockServer()
  }
}
