package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock

class GovUkMockServer : WireMockServer(8095) {

  fun stubGetBankHolidaysForEnglandAndWales() {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/bank-holidays.json"))
        .willReturn(
          WireMock.aResponse().withHeader(
            "Content-Type",
            "application/json",
          )
            .withBody(
              """{
                  "england-and-wales": {
                  "division": "england-and-wales",
                  "events": [
                      {
                          "title": "New Year’s Day",
                          "date": "2018-01-01",
                          "notes": "",
                          "bunting": true
                      },
                      {
                          "title": "Good Friday",
                          "date": "2018-03-30",
                          "notes": "",
                          "bunting": false
                      },
                      {
                          "title": "Easter Monday",
                          "date": "2018-04-02",
                          "notes": "",
                          "bunting": true
                      },
                      {
                          "title": "Early May bank holiday",
                          "date": "2018-05-07",
                          "notes": "",
                          "bunting": true
                      }
                    ]
                  }
                }""",
            ).withStatus(200),
        ),
    )
  }
}
