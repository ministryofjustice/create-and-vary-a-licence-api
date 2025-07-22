package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching

class DeliusMockServer : WireMockServer(8093) {

  fun stubGetTeamCodesForUser(staffIdentifier: Long = 1L) {
    stubFor(
      get(urlEqualTo("/staff/byid/$staffIdentifier")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          """{
                  "username": "Test User",
                  "email": "testUser@test.justice.gov.uk",
                  "code": "AB00001",
                  "id": 123456,
                  "name": {
                    "forename": "Test",
                    "surname": "User"
                  },
                  "teams": [
                    {
                      "code": "A01B02",
                      "description": "description",
                      "telephone": "0123456789",
                      "emailAddress": "first.last@digital.justice.gov.uk",
                      "localDeliveryUnit": {
                        "Code": "ABC123",
                        "Description": "Some description"
                      },
                      "teamType": {
                        "Code": "ABC123",
                        "Description": "Some description"
                      },
                      "district": {
                        "Code": "ABC123",
                        "Description": "Some description"
                      },
                      "borough": {
                        "Code": "ABC123",
                        "Description": "Some description"
                      },
                      "startDate": "2023-05-18",
                      "endDate": "2023-05-18"
                    }
                  ]
               }""",
        ).withStatus(200),
      ),
    )
  }

  fun stubGetOffenderManager(crn: String = "X12345") {
    stubFor(
      get(urlEqualTo("/probation-case/$crn/responsible-community-manager")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          // language=json
          """{
            "code": "staff-code-1",
            "id": 125,
            "case": { "crn": "$crn" },
            "name": { "forename": "Test", "surname": "Test" },
            "allocationDate": "2022-01-02",
            "team": {
              "code": "team-code-1",
              "description": "staff-description-1",
              "borough": { "code": "borough-code-1", "description": "borough-description-1" },
              "district": { "code": "district-code-1", "description": "district-description-1" }
            },
            "provider": { 
              "code": "probationArea-code-1", 
              "description": "probationArea-description-1"
            },
            "email": "user@test.com"
          }""",
        ).withStatus(200),
      ),
    )
  }

  fun stubGetOffenderManagerError(crn: String = "X12345") {
    stubFor(
      get(urlEqualTo("/probation-case/$crn/responsible-community-manager")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(byteArrayOf()).withStatus(200),
      ),
    )
  }

  fun stubGetManagersForGetApprovalCaseload() {
    stubFor(
      post(urlEqualTo("/probation-case/responsible-community-manager"))
        .willReturn(
          aResponse().withHeader("Content-Type", "application/json")
            .withBody(
              // language=json
              """[
                {
                  "code": "staff-code-1",
                  "case": {
                    "crn": "A12345",
                    "nomisId": "A1234AB"
                  },
                  "name": {
                    "forename": "Test1",
                    "surname": "Test1"
                  },
                  "allocationDate": "2022-01-02",
                  "team": {
                    "code": "team-code-1",
                    "description": "staff-description-1",
                    "borough": { "code": "borough-code-1", "description": "borough-description-1" },
                    "district": { "code": "district-code-1", "description": "district-description-1" }
                  },
                  "provider": { 
                    "code": "probationArea-code-1", 
                    "description": "probationArea-description-1"
                  },
                  "unallocated": false,
                  "email": "user@test.com"
                },
                {
                  "code": "staff-code-2",
                  "case": {
                    "crn": "B12345",
                    "nomisId": "A1234BC"
                  },
                  "name": {
                    "forename": "Test2",
                    "surname": "Test2"
                  },
                  "allocationDate": "2022-01-02",
                  "team": {
                    "code": "team-code-2",
                    "description": "staff-description-2",
                    "borough": { "code": "borough-code-2", "description": "borough-description-2" },
                    "district": { "code": "district-code-2", "description": "district-description-2" }
                  },
                  "provider": { 
                    "code": "probationArea-code-2", 
                    "description": "probationArea-description-2"
                  },
                  "unallocated": false
                },
                {
                  "code": "staff-code-3",
                  "case": {
                    "crn": "C12345",
                    "nomisId": "B1234BC"
                  },
                  "name": {
                    "forename": "Test3",
                    "surname": "Test3"
                  },
                  "allocationDate": "2022-01-02",
                  "team": {
                    "code": "team-code-3",
                    "description": "staff-description-3",
                    "borough": { "code": "borough-code-3", "description": "borough-description-3" },
                    "district": { "code": "district-code-3", "description": "district-description-3" }
                  },
                  "provider": { 
                    "code": "probationArea-code-3", 
                    "description": "probationArea-description-3"
                  }
                }]
              """.trimMargin(),
            ).withStatus(200),
        ),
    )
  }

  fun stubGetManagersForRecentlyApprovedCaseload() {
    stubFor(
      post(urlEqualTo("/probation-case/responsible-community-manager"))
        .willReturn(
          aResponse().withHeader("Content-Type", "application/json")
            .withBody(
              // language=json
              """[
              {
                "code": "staff-code-1",
                "case": {
                  "crn": "A12345",
                  "nomisId": "B1234BB"
                },
                "name": {
                  "forename": "Test1",
                  "surname": "Test1"
                },
                "allocationDate": "2022-01-02",
                "team": {
                  "code": "team-code-1",
                  "description": "staff-description-1",
                  "borough": {
                    "code": "borough-code-1",
                    "description": "borough-description-1"
                  },
                  "district": {
                    "code": "district-code-1",
                    "description": "district-description-1"
                  }
                },
                "provider": {
                  "code": "probationArea-code-1",
                  "description": "probationArea-description-1"
                },
                "unallocated": false,
                "email": "user@test.com"
              },
              {
                "code": "staff-code-2",
                "case": {
                  "crn": "B12345",
                  "nomisId": "F2504MG"
                },
                "name": {
                  "forename": "Test2",
                  "surname": "Test2"
                },
                "allocationDate": "2022-01-02",
                "team": {
                  "code": "team-code-2",
                  "description": "staff-description-2",
                  "borough": {
                    "code": "borough-code-2",
                    "description": "borough-description-2"
                  },
                  "district": {
                    "code": "district-code-2",
                    "description": "district-description-2"
                  }
                },
                "provider": {
                  "code": "probationArea-code-2",
                  "description": "probationArea-description-2"
                },
                "unallocated": false
              }
            ]
              """.trimMargin(),
            ).withStatus(200),
        ),
    )
  }

  fun stubGetStaffDetailsByUsername() {
    stubFor(
      post(urlEqualTo("/staff"))
        .willReturn(
          aResponse().withHeader(
            "Content-Type",
            "application/json",
          ).withBody(
            """
              [
                {
                  "code": "AB00001",
                  "id": 123456,
                  "name": {
                    "forename": "Test1 Firstname",
                    "surname": "Test1 Surname"
                  }
                }
              ]
            """.trimIndent(),
          ).withStatus(200),
        ),
    )
  }

  fun stubGetManagedOffenders(staffIdentifier: Long = 1L) {
    stubFor(
      get(urlEqualTo("/staff/byid/$staffIdentifier/caseload/managed-offenders")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          """
            [
              {
                "crn": "X12348",
                "nomisId": "AB1234E",
                "allocationDate": null,
                "staff": {
                  "id": null,
                  "code": "X1234",
                  "name": { 
                    "forename": "Joe",
                    "middleName": "Steve",
                    "surname": "Bloggs"
                  },
                  "unallocated": null
                },
                "team": null
              },
              {
                "crn": "X12349",
                "nomisId": "AB1234F",
                "allocationDate": null,
                "staff": {
                  "id": null,
                  "code": "X1234",
                  "name": { 
                    "forename": "Joe",
                    "middleName": "Steve",
                    "surname": "Bloggs"
                  },
                  "unallocated": null
                },
                "team": null
              },
              {
                "crn": "X12350",
                "nomisId": "AB1234G",
                "allocationDate": null,
                "staff": {
                  "id": null,
                  "code": "X1234",
                  "name": { 
                    "forename": "Joe",
                    "middleName": "Steve",
                    "surname": "Bloggs"
                  },
                  "unallocated": null
                },
                "team": null
              },
              {
                "crn": "X12351",
                "nomisId": "AB1234H",
                "allocationDate": null,
                "staff": {
                  "id": null,
                  "code": "X1234",
                  "name": { 
                    "forename": "Joe",
                    "middleName": "Steve",
                    "surname": "Bloggs"
                  },
                  "unallocated": null
                },
                "team": null
              },
              {
                "crn": "X12352",
                "nomisId": "AB1234I",
                "allocationDate": null,
                "staff": {
                  "id": null,
                  "code": "X1234",
                  "name": { 
                    "forename": "Joe",
                    "middleName": "Steve",
                    "surname": "Bloggs"
                  },
                  "unallocated": null
                },
                "team": null
              }
            ]
          """.trimIndent(),
        ).withStatus(200),
      ),
    )
  }

  fun stubGetManagedOffendersByTeam(teamCode: String) {
    stubFor(
      get(urlEqualTo("/team/$teamCode/caseload/managed-offenders")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          """
            [
              {
                "crn": "X12348",
                "nomisId": "AB1234E",
                "allocationDate": null,
                "staff": {
                  "id": null,
                  "code": "X1234",
                  "name": { 
                    "forename": "Joe",
                    "middleName": "Steve",
                    "surname": "Bloggs"
                  },
                  "unallocated": null
                },
                "team": null
              },
              {
                "crn": "X12349",
                "nomisId": "AB1234F",
                "allocationDate": null,
                "staff": {
                  "id": null,
                  "code": "X1234",
                  "name": { 
                    "forename": "Joe",
                    "middleName": "Steve",
                    "surname": "Bloggs"
                  },
                  "unallocated": null
                },
                "team": null
              }
            ]
          """.trimIndent(),
        ).withStatus(200),
      ),
    )
  }

  fun stubGetProbationCase() {
    stubFor(
      get(urlPathMatching("/probation-case/.+"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("""{ "crn": "X12345", "nomisId": "A1234AA" }""")
            .withStatus(200),
        ),
    )
  }

  fun stubGetProbationCases(response: String? = null) {
    // language=json
    val offendersJson = response ?: """[
      {
        "crn": "X12348",
        "croNumber": null,
        "pncNumber": null,
        "nomisId": "AB1234E"
      },
      {
        "crn": "X12349",
        "croNumber": null,
        "pncNumber": null,
        "nomisId": "AB1234F"
      },
      {
        "crn": "X12350",
        "croNumber": null,
        "pncNumber": null,
        "nomisId": "AB1234G"
      },
      {
        "crn": "X12351",
        "croNumber": null,
        "pncNumber": null,
        "nomisId": "AB1234H"
      },
      {
        "crn": "X12352",
        "croNumber": null,
        "pncNumber": null,
        "nomisId": "AB1234I"
      }
    ]
    """.trimIndent()
    stubFor(
      post(urlEqualTo("/probation-case"))
        .willReturn(
          aResponse().withHeader("Content-Type", "application/json")
            .withBody(offendersJson)
            .withStatus(200),
        ),
    )
  }

  fun stubGetTeamManagedCases() {
    stubFor(
      post(urlPathMatching("/staff/byid/.+/caseload/team-managed-offenders"))
        .willReturn(
          aResponse().withHeader("Content-Type", "application/json")
            .withBody(
              // language=json
              """{
                "content": [
                  {
                    "crn": "CRN1",
                    "nomisId": "A1234AA",
                    "name": {
                      "surname": "Surname",
                      "forename": "Test",
                      "middleName": ""
                    },
                    "staff": {
                      "code": "A01B02C",
                      "name": {
                        "surname": "Surname",
                        "forename": "Staff"
                      }
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
                    "allocationDate": "2023-05-24"
                  },
                  {
                    "crn": "CRN2",
                    "nomisId": "A1234AD",
                    "name": {
                      "surname": "Surname",
                      "forename": "Test",
                      "middleName": ""
                    },
                    "staff": {
                      "code": "A01B02C",
                      "name": {
                        "surname": "Surname",
                        "forename": "Staff"
                      }
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
                    "allocationDate": "2023-05-24"
                  }
                ],
                "page": {
                  "number": 0,
                  "size": 100,
                  "totalPages": 1,
                  "totalElements": 2
                }
              }""",
            ).withStatus(200),
        ),
    )
  }
}
