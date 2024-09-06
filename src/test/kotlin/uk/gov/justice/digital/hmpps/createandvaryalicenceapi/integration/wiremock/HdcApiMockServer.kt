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
            "curfewAddress": {
              "addressLine1": "123 Test Street",
              "addressLine2": null,
              "addressTown": "Test Area",
              "postCode": "AB1 2CD"
            },
            "firstNightCurfewHours": {
              "firstNightFrom": "15:00",
              "firstNightTo": "07:00"
            },
            "curfewHours": {
              "mondayFrom": "19:00",
              "mondayUntil": "07:00",
              "tuesdayFrom": "19:00",
              "tuesdayUntil": "07:00",
              "wednesdayFrom": "19:00",
              "wednesdayUntil": "07:00",
              "thursdayFrom": "19:00",
              "thursdayUntil": "07:00",
              "fridayFrom": "19:00",
              "fridayUntil": "07:00",
              "saturdayFrom": "19:00",
              "saturdayUntil": "07:00",
              "sundayFrom": "19:00",
              "sundayUntil": "07:00"
            }
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
