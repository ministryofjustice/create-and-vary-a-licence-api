package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

class CommunityApiMockServer : WireMockServer(8093) {

  fun stubGetTeamCodesForUser(staffIdentifier: Long = 1L) {
    stubFor(
      get(urlEqualTo("/secure/staff/staffIdentifier/$staffIdentifier")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          """{
                  "username": "Test User",
                  "email": "testUser@test.justice.gov.uk",
                  "telephoneNumber": "0123456789",
                  "staffCode": "AB00001",
                  "staffIdentifier": 123456,
                  "staff": {
                    "forenames": "Test",
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
                  ],
                  "probationArea": {
                    "probationAreaId": 0,
                    "code": "N01",
                    "description": "description",
                    "nps": true,
                    "organisation": {
                      "Code": "ABC123",
                      "Description": "Some description"
                    },
                    "institution": {
                      "institutionId": 0,
                      "isEstablishment": true,
                      "code": "string",
                      "description": "string",
                      "institutionName": "string",
                      "establishmentType": {
                        "Code": "ABC123",
                        "Description": "Some description"
                      },
                      "isPrivate": true,
                      "nomsPrisonInstitutionCode": "string"
                    },
                    "teams": [
                      {
                        "providerTeamId": 0,
                        "teamId": 0,
                        "code": "string",
                        "description": "string",
                        "name": "string",
                        "isPrivate": true,
                        "externalProvider": {
                          "Code": "ABC123",
                          "Description": "Some description"
                        },
                        "scProvider": {
                          "Code": "ABC123",
                          "Description": "Some description"
                        },
                        "localDeliveryUnit": {
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
                        }
                      }
                    ]
                  },
                  "staffGrade": {
                    "Code": "ABC123",
                    "Description": "Some description"
                  }
               }""",
        ).withStatus(200),
      ),
    )
  }

  fun stubGetAllOffenderManagers(crn: String = "X12345") {
    stubFor(
      get(urlEqualTo("/secure/offenders/crn/$crn/allOffenderManagers")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          """[{
            "staffCode": "staff-code-1",
            "staffId": 125,
            "team": {
              "code": "team-code-1",
              "description": "staff-description-1",
              "borough": { "code": "borough-code-1", "description": "borough-description-1" },
              "district": { "code": "district-code-1", "description": "district-description-1" }
            },
            "probationArea": { 
              "code": "probationArea-code-1", 
              "description": "probationArea-description-1"
            }
          }]""",
        ).withStatus(200),
      ),
    )
  }

  fun stubGetStaffDetailsByUsername(usernames: List<String> = listOf("Test1NPS")) {
    stubFor(
      post(urlEqualTo("/secure/staff/list"))
        .willReturn(
          aResponse().withHeader(
            "Content-Type",
            "application/json",
          ).withBody(
            """
              [
                {
                  "username": "test-client",
                  "email": "Test1@test.justice.gov.uk",
                  "telephoneNumber": "020 1111 2222",
                  "staffCode": "AB00001",
                  "staffIdentifier": 123456,
                  "staff": {
                    "forenames": "Test1 Firstname",
                    "surname": "Test1 Surname"
                  },
                  "teams": [
                    {
                      "code": "A01B01",
                      "description": "OMU A",
                      "telephone": "OMU A",
                      "emailAddress": "omu@digital.justice.gov.uk",
                      "localDeliveryUnit": "PO,CRC - PO",
                      "teamType": "PO,CRC - PO",
                      "district": "PO,CRC - PO",
                      "borough": "PO,CRC - PO",
                      "startDate": "2024-07-03",
                      "endDate": "2034-07-03"
                    }
                  ],
                  "probationArea": {
                    "probationAreaId": 0,
                    "code": "N01",
                    "description": "NPS North West",
                    "nps": true,
                    "organisation": "PO,CRC - PO",
                    "institution": {
                      "institutionId": 0,
                      "isEstablishment": true,
                      "code": "string",
                      "description": "string",
                      "institutionName": "string",
                      "establishmentType": "PO,CRC - PO",
                      "isPrivate": true,
                      "nomsPrisonInstitutionCode": "string"
                    },
                    "teams": [
                      {
                        "providerTeamId": 0,
                        "teamId": 0,
                        "code": "string",
                        "description": "string",
                        "name": "string",
                        "isPrivate": true,
                        "externalProvider": "PO,CRC - PO",
                        "scProvider": "PO,CRC - PO",
                        "localDeliveryUnit": "PO,CRC - PO",
                        "district": "PO,CRC - PO",
                        "borough": "PO,CRC - PO"
                      }
                    ]
                  },
                  "staffGrade": "PO,CRC - PO"
                }
              ]
            """.trimIndent(),
          ).withStatus(200),
        ),
    )
  }
}
