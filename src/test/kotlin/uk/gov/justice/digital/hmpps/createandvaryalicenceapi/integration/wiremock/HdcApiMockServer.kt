package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

class HdcApiMockServer : WireMockServer(8100) {
  fun stubGetHdcLicenceData(bookingId: Long = 123456) {
    stubFor(
      get(urlEqualTo("/licence/hdc/$bookingId")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          """{
            "licenceId": "1",
  "curfewAddress": {
    "addressLine1": "123 Test Street",
    "addressLine2": null,
    "addressTown": "Test Area",
    "postCode": "AB1 2CD"
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
  ]
}
          """.trimMargin(),
        ).withStatus(200),
      ),
    )
  }

  fun stubGetHdcLicenceDataNotFound(bookingId: Long = 123456) {
    stubFor(
      get(urlEqualTo("/licence/hdc/$bookingId"))
        .willReturn(aResponse().withStatus(404)),
    )
  }
}
