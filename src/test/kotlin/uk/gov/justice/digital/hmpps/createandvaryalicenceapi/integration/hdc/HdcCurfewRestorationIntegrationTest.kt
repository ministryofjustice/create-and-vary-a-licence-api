package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.hdc

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.Test
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceLinkType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceLinkType.CRD_REPLACEMENT_FOR_OPTED_OUT_LICENCE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceLinkType.CRD_REPLACEMENT_FOR_REFUSED_LICENCE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceLinkType.HDC_REPLACEMENT_FOR_OPTED_IN_LICENCE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.DayOfWeek
import java.time.LocalTime

class HdcCurfewRestorationIntegrationTest : HdcEventsIntegrationTestBase() {

  @Sql("classpath:test_data/seed-opted-out-hdc-with-curfew-times.sql")
  @Test
  fun `opt in restores curfew times from opted out link when there is no previous opt in link`() {
    testCurfewTimesRestored(CRD_REPLACEMENT_FOR_OPTED_OUT_LICENCE, originalLicenceId = 1L, crdLicenceId = 2L, expectedLicenceCount = 3)
  }

  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
    "classpath:test_data/seed-hdc-conversion-history.sql",
    "classpath:test_data/seed-crd-refused-link-for-hdc-conversion.sql",
  )
  @Test
  fun `opt in restores curfew times from refused link when there is no previous opt in link`() {
    testCurfewTimesRestored(CRD_REPLACEMENT_FOR_REFUSED_LICENCE, originalLicenceId = 2L, crdLicenceId = 1L, expectedLicenceCount = 8)
  }

  private fun testCurfewTimesRestored(
    replacementType: LicenceLinkType,
    originalLicenceId: Long,
    crdLicenceId: Long,
    expectedLicenceCount: Int,
  ) {
    // Given
    val event = createHdcStatusChangedEvent(bookingId = 123L, nomsNumber = "A1234AA")
    val eventJson = mapper.writeValueAsString(event)

    // When
    sendMessage(eventJson, HdcCvlEventType.OPT_IN.name)

    // Then
    assertSqsProcessed()

    val licences = testRepository.findAllLicence()
    assertThat(licences).hasSize(expectedLicenceCount)
    assertThat(licences.single { it.id == originalLicenceId }.statusCode).isEqualTo(LicenceStatus.INACTIVE)
    assertThat(licences.single { it.id == crdLicenceId }.statusCode).isEqualTo(LicenceStatus.INACTIVE)
    val newHdc = licences.single { it.kind == LicenceKind.HDC && it.statusCode == LicenceStatus.IN_PROGRESS }

    assertThat(testRepository.findAllLicenceLinks())
      .extracting("fromLicenceId", "toLicenceId", "linkType")
      .containsExactlyInAnyOrder(
        tuple(originalLicenceId, crdLicenceId, replacementType),
        tuple(crdLicenceId, newHdc.id, HDC_REPLACEMENT_FOR_OPTED_IN_LICENCE),
      )

    val weeklyTimes = testRepository.findWeeklyCurfewTimes(newHdc.id)
    assertThat(weeklyTimes).extracting("curfewTimesSequence", "fromDay", "fromTime", "untilDay", "untilTime")
      .containsExactly(
        tuple(1, DayOfWeek.MONDAY, LocalTime.of(19, 0), DayOfWeek.TUESDAY, LocalTime.of(7, 0)),
        tuple(2, DayOfWeek.TUESDAY, LocalTime.of(20, 0), DayOfWeek.WEDNESDAY, LocalTime.of(8, 0)),
      )
    assertThat(weeklyTimes.map { it.id }).doesNotContain(1L, 2L)
    val firstNight = requireNotNull(testRepository.findFirstNightCurfewTimes(newHdc.id))
    assertThat(firstNight.fromTime).isEqualTo(LocalTime.of(21, 0))
    assertThat(firstNight.untilTime).isEqualTo(LocalTime.of(6, 0))
    assertThat(firstNight.id).isNotEqualTo(3L)
  }
}
