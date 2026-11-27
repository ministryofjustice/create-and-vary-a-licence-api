package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.hdc

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceLinkType.CRD_REPLACEMENT_FOR_OPTED_OUT_LICENCE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceLinkType.CRD_REPLACEMENT_FOR_REFUSED_LICENCE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceLinkType.HDC_REPLACEMENT_FOR_OPTED_IN_LICENCE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.DayOfWeek
import java.time.LocalTime

class HdcConversionRoundTripIntegrationTest : HdcEventsIntegrationTestBase() {

  @Sql(
    "classpath:test_data/seed-completed-hdc-licence-1.sql",
    "classpath:test_data/seed-hdc-curfew-times-for-conversion.sql",
  )
  @ParameterizedTest
  @EnumSource(value = HdcCvlEventType::class, names = ["OPT_OUT", "REFUSE"])
  fun `opt in restores curfew times after HDC was replaced by CRD`(eventType: HdcCvlEventType) {
    // Given
    val originalLicenceId = 1L
    val event = createHdcStatusChangedEvent()
    val eventJson = mapper.writeValueAsString(event)

    sendMessage(eventJson, eventType.name)
    assertSqsProcessed()

    val crdLicence = testRepository.findAllLicence().single { it.kind == LicenceKind.CRD }
    val replacementType = if (eventType == HdcCvlEventType.OPT_OUT) {
      CRD_REPLACEMENT_FOR_OPTED_OUT_LICENCE
    } else {
      CRD_REPLACEMENT_FOR_REFUSED_LICENCE
    }
    assertThat(crdLicence.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(testRepository.findAllLicenceLinks())
      .extracting("fromLicenceId", "toLicenceId", "linkType")
      .containsExactly(tuple(originalLicenceId, crdLicence.id, replacementType))

    hdcApiMockServer.stubGetHdcLicenceData(12347L)

    // When
    sendMessage(eventJson, HdcCvlEventType.OPT_IN.name)

    // Then
    assertSqsProcessed(timesProcessed = 2)

    val licences = testRepository.findAllLicence()
    assertThat(licences).hasSize(3)
    assertThat(licences.single { it.id == originalLicenceId }.statusCode).isEqualTo(LicenceStatus.INACTIVE)
    assertThat(licences.single { it.id == crdLicence.id }.statusCode).isEqualTo(LicenceStatus.INACTIVE)
    val newHdc = licences.single { it.kind == LicenceKind.HDC && it.statusCode == LicenceStatus.IN_PROGRESS }

    assertThat(testRepository.findAllLicenceLinks())
      .extracting("fromLicenceId", "toLicenceId", "linkType")
      .containsExactlyInAnyOrder(
        tuple(originalLicenceId, crdLicence.id, replacementType),
        tuple(crdLicence.id, newHdc.id, HDC_REPLACEMENT_FOR_OPTED_IN_LICENCE),
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
