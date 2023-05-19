package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

class CommunityApiMockServer : WireMockServer(8093) {
  fun stubGetTeamCodesForUser(staffIdentifier: Long = 123456) {
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
               }"""
        ).withStatus(200)
      )
    )
  }
}
