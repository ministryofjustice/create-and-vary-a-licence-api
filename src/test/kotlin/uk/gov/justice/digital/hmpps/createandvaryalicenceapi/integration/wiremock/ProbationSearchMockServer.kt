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

  fun stubSearchForPersonByNomsNumberForGetApprovalCaseload() {
    stubFor(
      post(urlEqualTo("/nomsNumbers"))
        .willReturn(
          aResponse().withHeader(
            "Content-Type",
            "application/json",
          )
            .withBody(
              """[
                {
                  "offenderId": 1,
                  "otherIds": {
                    "crn": "A12345",
                    "nomsNumber": "A1234AB"
                  },
                  "offenderManagers": [
                    {
                      "active": true,
                      "staff": {
                        "code": "staff-code-1",
                        "forenames": "Test1",
                        "surname": "Test1",
                        "unallocated": false
                      }
                    }
                  ]
                },
                {
                  "offenderId": 2,
                  "otherIds": {
                    "crn": "B12345",
                    "nomsNumber": "A1234BC"
                  },
                  "offenderManagers": [
                    {
                      "active": true,
                      "staff": {
                        "code": "staff-code-2",
                        "forenames": "Test2",
                        "surname": "Test2",
                        "unallocated": false
                      }
                    }
                  ]
                },
                {
                  "offenderId": 3,
                  "otherIds": {
                    "crn": "C12345",
                    "nomsNumber": "B1234BC"
                  },
                  "offenderManagers": [
                    {
                      "active": true,
                      "staff": {
                        "code": "staff-code-3",
                        "forenames": "Test3",
                        "surname": "Test3",
                        "unallocated": false
                      }
                    }
                  ]
                }          
                ]
              """.trimMargin(),
            ).withStatus(200),
        ),
    )
  }

  fun stubSearchForPersonByNomsNumberForGetRecentlyApprovedCaseload() {
    stubFor(
      post(urlEqualTo("/nomsNumbers"))
        .willReturn(
          aResponse().withHeader(
            "Content-Type",
            "application/json",
          )
            .withBody(
              """[
                {
                  "offenderId": 1,
                  "otherIds": {
                    "crn": "A12345",
                    "nomsNumber": "B1234BB"
                  },
                  "offenderManagers": [
                    {
                      "active": true,
                      "staff": {
                        "code": "staff-code-1",
                        "forenames": "Test1",
                        "surname": "Test1",
                        "unallocated": false
                      }
                    }
                  ]
                },
                {
                  "offenderId": 2,
                  "otherIds": {
                    "crn": "B12345",
                    "nomsNumber": "F2504MG"
                  },
                  "offenderManagers": [
                    {
                      "active": true,
                      "staff": {
                        "code": "staff-code-2",
                        "forenames": "Test2",
                        "surname": "Test2",
                        "unallocated": false
                      }
                    }
                  ]
                }   
                ]
              """.trimMargin(),
            ).withStatus(200),
        ),
    )
  }

  fun stubGetOffendersByCrn(response: String? = null) {
    val offendersJson = response ?: """[
                {
                  "offenderId": 1,
                  "otherIds": {
                    "crn": "X12348",
                    "croNumber": null,
                    "pncNumber": null,
                    "nomsNumber": "AB1234E"
                  },
                  "offenderManagers": []
                },
                {
                  "offenderId": 3,
                  "otherIds": {
                    "crn": "X12349",
                    "croNumber": null,
                    "pncNumber": null,
                    "nomsNumber": "AB1234F"
                  },
                  "offenderManagers": []
                },
                {
                  "offenderId": 5,
                  "otherIds": {
                    "crn": "X12350",
                    "croNumber": null,
                    "pncNumber": null,
                    "nomsNumber": "AB1234G"
                  },
                  "offenderManagers": []
                },
                {
                  "offenderId": 6,
                  "otherIds": {
                    "crn": "X12351",
                    "croNumber": null,
                    "pncNumber": null,
                    "nomsNumber": "AB1234H"
                  },
                  "offenderManagers": []
                },
                {
                  "offenderId": 7,
                  "otherIds": {
                    "crn": "X12352",
                    "croNumber": null,
                    "pncNumber": null,
                    "nomsNumber": "AB1234I"
                  },
                  "offenderManagers": []
                }
              ]
      
    """.trimIndent()
    stubFor(
      post(urlEqualTo("/crns"))
        .willReturn(
          aResponse().withHeader(
            "Content-Type",
            "application/json",
          )
            .withBody(offendersJson)
            .withStatus(200),
        ),
    )
  }
}
