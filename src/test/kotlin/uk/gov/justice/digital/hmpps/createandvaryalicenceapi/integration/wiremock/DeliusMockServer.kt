package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching

class DeliusMockServer : WireMockServer(8093) {

  private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())
    .registerKotlinModule()

  fun stubAssignDeliusRole(userName: String) {
    stubFor(
      put(urlEqualTo("/users/$userName/roles")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(200),
      ),
    )
  }

  fun stubGetStaffByCode(
    staffCode: String,
    userName: String = "userName",
    teamCode: String = "teamCode",
    firstName: String = "firstName",
    lastName: String = "lastName",
  ) {
    stubFor(
      get(urlEqualTo("/staff/bycode/$staffCode")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          """{
          "id": 123456,
          "code": "AB00001",
          "name": { "forename": "$firstName", "surname": "$lastName" },
          "teams": [
            {
              "code": "$teamCode",
              "description": "Team description",
              "telephone": "0123456789",
              "emailAddress": "first.last@digital.justice.gov.uk",
              "localDeliveryUnit": {
                "code": "LBC123",
                "description": "local description"
              },
              "teamType": {
                "code": "TT123",
                "description": "Type description"
              },
              "district": {
                "code": "DBC123",
                "description": "District description"
              },
              "borough": {
                "code": "BC123",
                "description": "Borough description"
              },
              "provider": {
                "code": "PBC123",
                "description": "Provider description"
              },
              "startDate": "2023-05-18",
              "endDate": "2023-05-18"
            }
          ],
          "provider": {
            "code": "PBC123",
            "description": "Some Provider"
          },
          "username": "$userName",
          "email": "testUser@test.justice.gov.uk",
          "telephoneNumber": "0123456789",
          "unallocated": false
        }""",
        ).withStatus(200),
      ),
    )
  }

  fun stubGetTeamCodesForUser(
    staffIdentifier: Long = 1L,
    userName: String = "Test User",
    firstName: String = "firstName",
    lastName: String = "lastName",
  ) {
    stubFor(
      get(urlEqualTo("/staff/byid/$staffIdentifier")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          """{
          "id": 123456,
          "code": "AB00001",
          "name": { "forename": "$firstName", "surname": "$lastName" },
          "teams": [
            {
              "code": "A01B02",
              "description": "Team description",
              "telephone": "0123456789",
              "emailAddress": "first.last@digital.justice.gov.uk",
              "localDeliveryUnit": {
                "code": "LBC123",
                "description": "local description"
              },
              "teamType": {
                "code": "TT123",
                "description": "Type description"
              },
              "district": {
                "code": "DBC123",
                "description": "District description"
              },
              "borough": {
                "code": "BC123",
                "description": "Borough description"
              },
              "provider": {
                "code": "PBC123",
                "description": "Some Provider"
              },
              "startDate": "2023-05-18",
              "endDate": "2023-05-18"
            }
          ],
          "provider": {
            "code": "PBC123",
            "description": "Some Provider"
          },
          "username": "$userName",
          "email": "testUser@test.justice.gov.uk",
          "telephoneNumber": "0123456789",
          "unallocated": false
        }""",
        ).withStatus(200),
      ),
    )
  }

  fun stubGetOffenderManager(
    crn: String = "X12345",
    userName: String = "AZ12345",
    staffCode: String = "staff-1",
    emailAddress: String = "user@test.com",
    staffIdentifier: Long = 125,
    firstName: String = "firstName",
    lastName: String = "lastName",
    regionCode: String = "probationArea-code-1",
  ) {
    stubFor(
      get(urlEqualTo("/probation-case/$crn/responsible-community-manager")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          // language=json
          """{
            "code": "$staffCode",
            "id": $staffIdentifier,
            "case": { "crn": "$crn" },
            "name": { "forename": "$firstName", "surname": "$lastName" },
            "allocationDate": "2022-01-02",
            "team": {
              "code": "team-code-1",
              "description": "staff-description-1",
              "borough": { "code": "borough-code-1", "description": "borough-description-1" },
              "district": { "code": "district-code-1", "description": "district-description-1" },
              "provider": { "code": "$regionCode", "description": "probationArea-description-1" }
            },
            "provider": { 
              "code": "$regionCode", 
              "description": "probationArea-description-1"
            },
            "email": "$emailAddress",
            "username" : "$userName"
          }""",
        ).withStatus(200),
      ),
    )
  }

  fun stubGetOffenderManagerWithNomsId(
    nomsId: String = "A1234AA",
    userName: String = "AZ12345",
    staffCode: String = "staff-1",
    emailAddress: String = "user@test.com",
    staffIdentifier: Long = 125,
    firstName: String = "firstName",
    lastName: String = "lastName",
  ) {
    stubFor(
      get(urlEqualTo("/probation-case/$nomsId/responsible-community-manager")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          // language=json
          """{
            "code": "$staffCode", 
            "id": $staffIdentifier,
            "case": { "crn": "X12345", "nomisId": "$nomsId" },
            "name": { "forename": "$firstName", "surname": "$lastName" },
            "allocationDate": "2022-01-02",
            "team": {
              "code": "team-code-1",
              "description": "staff-description-1",
              "borough": { "code": "borough-code-1", "description": "borough-description-1" },
              "district": { "code": "district-code-1", "description": "district-description-1" },
              "provider": { "code": "probationArea-code-1", "description": "probationArea-description-1" }
            },
            "provider": { 
              "code": "probationArea-code-1", 
              "description": "probationArea-description-1"
            },
            "email": "$emailAddress",
            "username" : "$userName"
          }""",
        ).withStatus(200),
      ),
    )
  }

  fun stubGetManagers() {
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
                    "district": { "code": "district-code-1", "description": "district-description-1" },
                    "provider": { "code": "probationArea-code-1", "description": "probationArea-description-1" }
                  },
                  "provider": { 
                    "code": "probationArea-code-1", 
                    "description": "probationArea-description-1"
                  },
                  "unallocated": false,
                  "email": "user@test.com"
                },
                {
                  "code": "staff-code-1",
                  "case": {
                    "crn": "D12345",
                    "nomisId": "C1234BC"
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
                    "district": { "code": "district-code-1", "description": "district-description-1" },
                    "provider": { "code": "probationArea-code-1", "description": "probationArea-description-1" }
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
                    "district": { "code": "district-code-2", "description": "district-description-2" },
                    "provider": { "code": "probationArea-code-2", "description": "probationArea-description-2" }
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
                    "district": { "code": "district-code-3", "description": "district-description-3" },
                    "provider": { "code": "probationArea-code-3", "description": "probationArea-description-3" }
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

  fun stubGetManagersWithoutUserDetails() {
    stubFor(
      post(urlEqualTo("/probation-case/responsible-community-manager?includeEmail=false"))
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
                    "district": { "code": "district-code-1", "description": "district-description-1" },
                    "provider": { "code": "probationArea-code-1", "description": "probationArea-description-1" }
                  },
                  "provider": { 
                    "code": "probationArea-code-1", 
                    "description": "probationArea-description-1"
                  },
                  "unallocated": false
                },
                {
                  "code": "staff-code-1",
                  "case": {
                    "crn": "D12345",
                    "nomisId": "C1234BC"
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
                    "district": { "code": "district-code-1", "description": "district-description-1" },
                    "provider": { "code": "probationArea-code-1", "description": "probationArea-description-1" }
                  },
                  "provider": { 
                    "code": "probationArea-code-1", 
                    "description": "probationArea-description-1"
                  },
                  "unallocated": false
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
                    "district": { "code": "district-code-2", "description": "district-description-2" },
                    "provider": { "code": "probationArea-code-2", "description": "probationArea-description-2" }
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
                    "district": { "code": "district-code-3", "description": "district-description-3" },
                    "provider": { "code": "probationArea-code-3", "description": "probationArea-description-3" }
                  },
                  "provider": { 
                    "code": "probationArea-code-3", 
                    "description": "probationArea-description-3"
                  }
                },
                {
                  "code": "staff-code-4",
                  "case": {
                    "crn": "B12345",
                    "nomisId": "A1234AG"
                  },
                  "name": {
                    "forename": "Test4",
                    "surname": "Test4"
                  },
                  "allocationDate": "2022-01-02",
                  "team": {
                    "code": "team-code-4",
                    "description": "staff-description-4",
                    "borough": { "code": "borough-code-4", "description": "borough-description-4" },
                    "district": { "code": "district-code-4", "description": "district-description-4" },
                    "provider": { "code": "probationArea-code-4", "description": "probationArea-description-4" }
                  },
                  "provider": { 
                    "code": "probationArea-code-4", 
                    "description": "probationArea-description-4"
                  },
                  "unallocated": false
                }]
              """.trimMargin(),
            ).withStatus(200),
        ),
    )
  }

  fun stubGetManagers(
    managers: List<Any>,
    excludeUserInfo: Boolean = false,
  ) {
    val jsonBody = objectMapper.writeValueAsString(managers)

    stubFor(
      post(urlEqualTo("/probation-case/responsible-community-manager${if (excludeUserInfo) "?includeEmail=false" else ""}"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(jsonBody)
            .withStatus(200),
        ),
    )
  }

  fun stubGetManagersForPromptComJob() {
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
                    "district": { "code": "district-code-1", "description": "district-description-1" },
                    "provider": { "code": "probationArea-code-1", "description": "probationArea-description-1" }
                  },
                  "provider": { 
                    "code": "probationArea-code-1", 
                    "description": "probationArea-description-1"
                  },
                  "unallocated": false,
                  "email": "user@test.com"
                },
                {
                  "code": "staff-code-1",
                  "case": {
                    "crn": "D12345",
                    "nomisId": "C1234BC"
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
                    "district": { "code": "district-code-1", "description": "district-description-1" },
                    "provider": { "code": "probationArea-code-1", "description": "probationArea-description-1" }
                  },
                  "provider": { 
                    "code": "probationArea-code-1", 
                    "description": "probationArea-description-1"
                  },
                  "unallocated": false,
                  "email": "user@test.com"
                },
                {
                  "code": "staff-code-1",
                  "case": {
                    "crn": "C12346",
                    "nomisId": "A1234AF"
                  },
                  "name": {
                    "forename": "Test4",
                    "surname": "Test4"
                  },
                  "allocationDate": "2022-01-02",
                  "team": {
                    "code": "team-code-4",
                    "description": "staff-description-4",
                    "borough": { "code": "borough-code-4", "description": "borough-description-4" },
                    "district": { "code": "district-code-4", "description": "district-description-4" },
                    "provider": { "code": "REGION1", "description": "probationArea-description-4" }
                  },
                  "provider": { 
                    "code": "probationArea-code-4", 
                    "description": "probationArea-description-4"
                  },
                  "unallocated": false,
                  "email": "user@test.com"

                }]
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
              },
              {
                "crn": "X12353",
                "nomisId": "AB1234J",
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
                "team": {
                  "id": 1,
                  "code": "TEAM1",
                  "description": "Team 1",
                  "district": {
                    "code": "DISTRICT1",
                    "description": "District 1"
                  },
                  "borough": {
                    "code": "BOROUGH1",
                    "description": "Borough 1"
                  },
                  "provider": {
                    "code": "REGION1",
                    "description": "Region 1"
                  }
                }
              },
              {
                "crn": "X12354",
                "nomisId": "AB1234K",
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
                "team": {
                  "id": 2,
                  "code": "TEAM2",
                  "description": "Team 2",
                  "district": {
                    "code": "DISTRICT2",
                    "description": "District 2"
                  },
                  "borough": {
                    "code": "BOROUGH2",
                    "description": "Borough 2"
                  },
                  "provider": {
                    "code": "REGION2",
                    "description": "Region 2"
                  }
                }
              },
              {
                "crn": "X12355",
                "nomisId": "AB1234L",
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
                "team": {
                  "id": 2,
                  "code": "TEAM2",
                  "description": "Team 2",
                  "district": {
                    "code": "DISTRICT2",
                    "description": "District 2"
                  },
                  "borough": {
                    "code": "BOROUGH2",
                    "description": "Borough 2"
                  },
                  "provider": {
                    "code": "REGION2",
                    "description": "Region 2"
                  }
                }
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
              },
              {
                "crn": "X12353",
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
                "team": {
                  "id": 1,
                  "code": "TEAM1",
                  "description": "Team 1",
                  "district": {
                    "code": "DISTRICT1",
                    "description": "District 1"
                  },
                  "borough": {
                    "code": "BOROUGH1",
                    "description": "Borough 1"
                  },
                  "provider": {
                    "code": "REGION1",
                    "description": "Region 1"
                  }
                }
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
        "nomisId": "A1234AB"
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

  fun stubGetTeamManagedCases(response: String? = null) {
    // language=json
    val offendersJson = response ?: """{
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
            },
            "provider": { 
              "code": "probationArea-code-1", 
              "description": "probationArea-description-1"
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
            },
            "provider": { 
              "code": "probationArea-code-1",
              "description": "probationArea-description-1"
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
    }
    """.trimIndent()
    stubFor(
      post(urlPathMatching("/staff/byid/.+/caseload/team-managed-offenders"))
        .willReturn(
          aResponse().withHeader("Content-Type", "application/json")
            .withBody(offendersJson)
            .withStatus(200),
        ),
    )
  }

  fun stubGetTeamManagedUnallocatedCases() {
    stubFor(
      post(urlPathMatching("/staff/byid/.+/caseload/team-managed-offenders"))
        .willReturn(
          aResponse().withHeader("Content-Type", "application/json")
            .withBody(
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
                      "unallocated": true,
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
                      },
                      "provider": { 
                        "code": "probationArea-code-1", 
                        "description": "probationArea-description-1"
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

  fun stubGetCheckUserAccess(response: String? = null) {
    // language=json
    val accessJson = response ?: """
      {
        "access": [
          {
            "crn": "X12348",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "X12349",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "X12350",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "X12351",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "X12352",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "X12353",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "X12354",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "X12355",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "CRN1",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "CRN2",
            "userExcluded": false,
            "userRestricted": false
          }
        ]
      }
    """.trimIndent()
    stubFor(
      post(urlPathMatching("/users/.+/access"))
        .willReturn(
          aResponse().withHeader("Content-Type", "application/json")
            .withBody(accessJson)
            .withStatus(200),
        ),
    )
  }
}
