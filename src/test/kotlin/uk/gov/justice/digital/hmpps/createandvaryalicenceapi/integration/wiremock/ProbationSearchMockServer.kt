package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

class ProbationSearchMockServer : WireMockServer(8094) {
  fun stubPostLicenceCaseloadByTeam(aLicenceCaseloadSearchRequest: String) {
    stubFor(
      post(urlEqualTo("/licence-caseload/by-team"))
        .withRequestBody(equalToJson(aLicenceCaseloadSearchRequest))
        .willReturn(
          aResponse().withHeader(
            "Content-Type",
            "application/json",
          )
            .withBody(
              """{
                    "content": [
                        {
                            "name": {
                                "surname": "Surname",
                                "forename": "Test",
                                "middleName": ""
                            },
                            "identifiers": {
                                "crn": "CRN1",
                                "noms": "A1234AA"
                            },
                            "manager": {
                                "code": "A01B02C",
                                "name": {
                                    "surname": "Surname",
                                    "forename": "Staff"
                                },
                                "team": {
                                    "code": "A01B02",
                                    "description": "Test Team",
                                    "borough": {
                                        "code": "A01B02",
                                        "description": "description"
                                    },
                                    "district": {
                                        "code": "A01B02",
                                        "description": "description"
                                    }
                                },
                                "probationArea": {
                                    "code": "N01",
                                    "description": "description"
                                }
                            },
                            "allocationDate": "2023-05-24"
                        },                        
                        {
                            "name": {
                                "surname": "Surname",
                                "forename": "Test",
                                "middleName": ""
                            },
                            "identifiers": {
                                "crn": "CRN2",
                                "noms": "A1234AD"
                            },
                            "manager": {
                                "code": "A01B02C",
                                "name": {
                                    "surname": "Surname",
                                    "forename": "Staff"
                                },
                                "team": {
                                    "code": "A01B02",
                                    "description": "Test Team",
                                    "borough": {
                                        "code": "A01B02",
                                        "description": "description"
                                    },
                                    "district": {
                                        "code": "A01B02",
                                        "description": "description"
                                    }
                                },
                                "probationArea": {
                                    "code": "N01",
                                    "description": "description"
                                }
                            },
                            "allocationDate": "2023-05-24"
                        }
                    ],
                    "pageable": {
                        "pageSize": 100,
                        "offset": 0,
                        "sort": {
                            "empty": false,
                            "unsorted": false,
                            "sorted": true
                        },
                        "pageNumber": 0,
                        "paged": true,
                        "unpaged": false
                    },
                    "totalElements": 2,
                    "totalPages": 1,
                    "last": true,
                    "size": 100,
                    "number": 0,
                    "sort": {
                        "empty": false,
                        "unsorted": false,
                        "sorted": true
                    },
                    "first": true,
                    "numberOfElements": 2,
                    "empty": false
                }""",
            ).withStatus(200),
        ),
    )
  }

  fun stubPostLicenceCaseloadByTeamNoResult(aLicenceCaseloadSearchRequest: String) {
    stubFor(
      post(urlEqualTo("/licence-caseload/by-team"))
        .withRequestBody(equalToJson(aLicenceCaseloadSearchRequest))
        .willReturn(
          aResponse().withHeader(
            "Content-Type",
            "application/json",
          )
            .withBody(
              """{
                    "content": [],
                    "pageable": {
                        "pageSize": 100,
                        "offset": 0,
                        "sort": {
                            "empty": false,
                            "unsorted": false,
                            "sorted": true
                        },
                        "pageNumber": 0,
                        "paged": true,
                        "unpaged": false
                    },
                    "totalElements": 0,
                    "totalPages": 0,
                    "last": true,
                    "size": 100,
                    "number": 0,
                    "sort": {
                        "empty": false,
                        "unsorted": false,
                        "sorted": true
                    },
                    "first": true,
                    "numberOfElements": 0,
                    "empty": true
                }""",
            ).withStatus(200),
        ),
    )
  }

  fun stubSearchForPersonOnProbation() {
    stubFor(
      post(urlEqualTo("/search"))
        .willReturn(
          aResponse().withHeader(
            "Content-Type",
            "application/json",
          )
            .withBody(
              """[
                {
                 "offenderId": 1,
                 "otherIds": { "crn": "X12345" },
                 "offenderManagers": [
                    {
                     "active": true,
                     "staff": { "code": "staff-code-1"}
                    } 
                 ]
                }
                ]
              """.trimMargin(),
            ).withStatus(200),
        ),
    )
  }
}
