package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.CurrentPrisonerHdcStatus

class HdcApiMockServer :
  WireMockExtension(
    extensionOptions()
      .options(wireMockConfig().port(8100)),
  ) {
  fun stubGetHdcLicenceData(bookingId: Long = 54321) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/licence/hdc/$bookingId")).willReturn(
        WireMock.aResponse().withHeader("Content-Type", "application/json").withBody(
          """{
            "licenceId": "1",
            "curfewAddress": {
              "addressLine1": "123 Test Street",
              "addressLine2": null,
              "townOrCity": "Test Area",
              "postcode": "AB1 2CD",
              "curfewAddressType": "RESIDENTIAL"
            },
            "firstNightCurfewHours": {
              "firstNightFrom": "15:00",
              "firstNightUntil": "07:00"
            },
            "curfewTimes": [
              {
                "fromDay": "MONDAY",
                "fromTime": "19:00",
                "untilDay": "TUESDAY",
                "untilTime": "07:00"
              },
              {
                "fromDay": "TUESDAY",
                "fromTime": "19:00",
                "untilDay": "WEDNESDAY",
                "untilTime": "07:00"
              },
              {
                "fromDay": "WEDNESDAY",
                "fromTime": "19:00",
                "untilDay": "THURSDAY",
                "untilTime": "07:00"
              },
              {
                "fromDay": "THURSDAY",
                "fromTime": "19:00",
                "untilDay": "FRIDAY",
                "untilTime": "07:00"
              },
              {
                "fromDay": "FRIDAY",
                "fromTime": "19:00",
                "untilDay": "SATURDAY",
                "untilTime": "07:00"
              },
              {
                "fromDay": "SATURDAY",
                "fromTime": "19:00",
                "untilDay": "SUNDAY",
                "untilTime": "07:00"
              },
              {
                "fromDay": "SUNDAY",
                "fromTime": "19:00",
                "untilDay": "MONDAY",
                "untilTime": "07:00"
              }
            ],
            "status": "APPROVED"
          }
          """.trimMargin(),
        ).withStatus(200),
      ),
    )
  }

  fun stubGetHdcLicenceDataNotFound(bookingId: Long = 54321) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/licence/hdc/$bookingId"))
        .willReturn(WireMock.aResponse().withStatus(404)),
    )
  }

  fun stubGetHdcStatuses(currentPrisonerHdcStatus: List<CurrentPrisonerHdcStatus>) {
    val jsonArray = currentPrisonerHdcStatus.joinToString(
      prefix = "[",
      postfix = "]",
      separator = ",",
    ) { (bookingId, hdcStatus) ->
      """
    {
      "bookingId": $bookingId,
      "status": "${hdcStatus.name}"
    }
      """.trimIndent()
    }

    stubFor(
      WireMock.post(WireMock.urlEqualTo("/licence/hdc/status"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(jsonArray)
            .withStatus(200),
        ),
    )
  }
}
