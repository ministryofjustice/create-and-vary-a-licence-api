package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.HdcApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcCurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateCurfewTimesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcLicenceData
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
  fun `Update the curfew times`() {
    hdcApiMockServer.stubGetHdcLicenceData(54321L)

    webTestClient.put()
      .uri("/licence/id/1/curfew-times")
      .bodyValue(anUpdateCurfewTimesRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/hdc/curfew/licenceId/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(HdcLicenceData::class.java)
      .returnResult().responseBody

    assertThat(result?.curfewTimes)
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

  private companion object {
    val anUpdateCurfewTimesRequest = UpdateCurfewTimesRequest(
      listOf(
        HdcCurfewTimes(
          curfewTimesSequence = 1,
          fromDay = DayOfWeek.MONDAY,
          fromTime = LocalTime.of(20, 0),
          untilDay = DayOfWeek.TUESDAY,
          untilTime = LocalTime.of(8, 0),
        ),
        HdcCurfewTimes(
          curfewTimesSequence = 2,
          fromDay = DayOfWeek.TUESDAY,
          fromTime = LocalTime.of(20, 0),
          untilDay = DayOfWeek.WEDNESDAY,
          untilTime = LocalTime.of(8, 0),
        ),
        HdcCurfewTimes(
          curfewTimesSequence = 3,
          fromDay = DayOfWeek.WEDNESDAY,
          fromTime = LocalTime.of(20, 0),
          untilDay = DayOfWeek.THURSDAY,
          untilTime = LocalTime.of(8, 0),
        ),
        HdcCurfewTimes(
          curfewTimesSequence = 4,
          fromDay = DayOfWeek.THURSDAY,
          fromTime = LocalTime.of(20, 0),
          untilDay = DayOfWeek.FRIDAY,
          untilTime = LocalTime.of(8, 0),
        ),
        HdcCurfewTimes(
          curfewTimesSequence = 5,
          fromDay = DayOfWeek.FRIDAY,
          fromTime = LocalTime.of(20, 0),
          untilDay = DayOfWeek.SATURDAY,
          untilTime = LocalTime.of(8, 0),
        ),
        HdcCurfewTimes(
          curfewTimesSequence = 6,
          fromDay = DayOfWeek.SATURDAY,
          fromTime = LocalTime.of(20, 0),
          untilDay = DayOfWeek.SUNDAY,
          untilTime = LocalTime.of(8, 0),
        ),
        HdcCurfewTimes(
          curfewTimesSequence = 7,
          fromDay = DayOfWeek.SUNDAY,
          fromTime = LocalTime.of(20, 0),
          untilDay = DayOfWeek.MONDAY,
          untilTime = LocalTime.of(8, 0),
        ),
      ),
    )

    val govUkApiMockServer = GovUkMockServer()
    val hdcApiMockServer = HdcApiMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkApiMockServer.start()
      hdcApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      govUkApiMockServer.stop()
      hdcApiMockServer.stop()
    }
  }
}
