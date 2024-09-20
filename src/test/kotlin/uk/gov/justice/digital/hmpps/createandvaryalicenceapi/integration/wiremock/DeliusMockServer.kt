package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

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
          """{
            "code": "staff-code-1",
            "id": 125,
            "team": {
              "code": "team-code-1",
              "description": "staff-description-1",
              "borough": { "code": "borough-code-1", "description": "borough-description-1" },
              "district": { "code": "district-code-1", "description": "district-description-1" }
            },
            "provider": { 
              "code": "probationArea-code-1", 
              "description": "probationArea-description-1"
            }
          }""",
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
}
