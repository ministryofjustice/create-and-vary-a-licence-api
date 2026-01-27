package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock

private const val OS_PLACES_WIREMOCK_PORT = 8099

class OsPlacesMockServer(private val apiKey: String) : WireMockServer(OS_PLACES_WIREMOCK_PORT) {

  fun stubGetAddressesForPostcode(postcode: String, offset: Int = 0, maxResults: Int = 100) {
    val json = """{
      "header": {
        "uri": "https://api.os.uk/search/places/v1/postcode?postcode=$postcode&offset=$offset&maxresults=$maxResults",
        "query": "query=$postcode",
        "offset": 0,
        "totalresults": 29,
        "format": "JSON",
        "dataset": "DPA",
        "lr": "EN,CY",
        "maxresults": 100,
        "epoch": "112",
        "lastupdate": "2024-09-20",
        "output_srs": "EPSG:27700"
      },
      "results": [
        {
          "DPA": {
            "UPRN": "1234567",
            "UDPRN": "22659267",
            "ADDRESS": "1, THE ROAD, Fakeline, Faketon, FA1 1KE",
            "BUILDING_NUMBER": "1",
            "THOROUGHFARE_NAME": "THE ROAD",
            "DEPENDENT_LOCALITY": "Fakeline",
            "POST_TOWN": "Faketon",
            "POSTCODE": "FA11KE",
            "RPC": "1",
            "X_COORDINATE": 201024.0,
            "Y_COORDINATE": 454112.0,
            "STATUS": "APPROVED",
            "LOGICAL_STATUS_CODE": "1",
            "CLASSIFICATION_CODE": "PB03",
            "CLASSIFICATION_CODE_DESCRIPTION": "Detached",
            "LOCAL_CUSTODIAN_CODE": 390,
            "LOCAL_CUSTODIAN_CODE_DESCRIPTION": "Fakeshire",
            "COUNTRY_CODE": "W",
            "COUNTRY_CODE_DESCRIPTION": "This record is within Wales",
            "POSTAL_ADDRESS_CODE": "D",
            "POSTAL_ADDRESS_CODE_DESCRIPTION": "A record which is linked to PAF",
            "BLPU_STATE_CODE": "2",
            "BLPU_STATE_CODE_DESCRIPTION": "In use",
            "TOPOGRAPHY_LAYER_TOID": "osgb1TEST938",
            "WARD_CODE": "E050TEST72",
            "PARISH_CODE": "E040118TEST",
            "LAST_UPDATE_DATE": "01/05/2021",
            "ENTRY_DATE": "15/01/2002",
            "BLPU_STATE_DATE": "12/12/2007",
            "LANGUAGE": "EN",
            "MATCH": 1.0,
            "MATCH_DESCRIPTION": "EXACT",
            "DELIVERY_POINT_SUFFIX": "2A"
          }
        },
        {
          "DPA": {
            "UPRN": "12345678",
            "UDPRN": "12345678",
            "ADDRESS": "2, A ROAD, Fakeline, Faketon, FA1 1KE",
            "BUILDING_NUMBER": "2",
            "THOROUGHFARE_NAME": "A ROAD",
            "DEPENDENT_LOCALITY": "Fakeline",
            "POST_TOWN": "Faketon",
            "POSTCODE": "FA11KE",
            "RPC": "1",
            "X_COORDINATE": 201024.0,
            "Y_COORDINATE": 454112.0,
            "STATUS": "APPROVED",
            "LOGICAL_STATUS_CODE": "1",
            "CLASSIFICATION_CODE": "PB03",
            "CLASSIFICATION_CODE_DESCRIPTION": "Detached",
            "LOCAL_CUSTODIAN_CODE": 390,
            "LOCAL_CUSTODIAN_CODE_DESCRIPTION": "Fakeshire",
            "COUNTRY_CODE": "W",
            "COUNTRY_CODE_DESCRIPTION": "This record is within Wales",
            "POSTAL_ADDRESS_CODE": "D",
            "POSTAL_ADDRESS_CODE_DESCRIPTION": "A record which is linked to PAF",
            "BLPU_STATE_CODE": "2",
            "BLPU_STATE_CODE_DESCRIPTION": "In use",
            "TOPOGRAPHY_LAYER_TOID": "osgb1TEST938",
            "WARD_CODE": "E050TEST72",
            "PARISH_CODE": "E040118TEST",
            "LAST_UPDATE_DATE": "01/05/2021",
            "ENTRY_DATE": "15/01/2002",
            "BLPU_STATE_DATE": "12/12/2007",
            "LANGUAGE": "EN",
            "MATCH": 1.0,
            "MATCH_DESCRIPTION": "EXACT",
            "DELIVERY_POINT_SUFFIX": "2A"
          }
        },
        {
          "DPA": {
            "UPRN": "123456789",
            "UDPRN": "123456789",
            "ADDRESS": "3, A ROAD, FAKELINE, FAKETON, FA1X 1KE",
            "BUILDING_NUMBER": "3",
            "THOROUGHFARE_NAME": "A ROAD",
            "DEPENDENT_LOCALITY": "Fakeline",
            "POST_TOWN": "Faketon",
            "POSTCODE": "FA11KE",
            "RPC": "1",
            "X_COORDINATE": 201024.0,
            "Y_COORDINATE": 454112.0,
            "STATUS": "APPROVED",
            "LOGICAL_STATUS_CODE": "1",
            "CLASSIFICATION_CODE": "PB03",
            "CLASSIFICATION_CODE_DESCRIPTION": "Detached",
            "LOCAL_CUSTODIAN_CODE": 390,
            "LOCAL_CUSTODIAN_CODE_DESCRIPTION": "Fakeshire",
            "COUNTRY_CODE": "W",
            "COUNTRY_CODE_DESCRIPTION": "This record is within Wales",
            "POSTAL_ADDRESS_CODE": "D",
            "POSTAL_ADDRESS_CODE_DESCRIPTION": "A record which is linked to PAF",
            "BLPU_STATE_CODE": "2",
            "BLPU_STATE_CODE_DESCRIPTION": "In use",
            "TOPOGRAPHY_LAYER_TOID": "osgb1TEST938",
            "WARD_CODE": "E050TEST72",
            "PARISH_CODE": "E040118TEST",
            "LAST_UPDATE_DATE": "01/05/2021",
            "ENTRY_DATE": "15/01/2002",
            "BLPU_STATE_DATE": "12/12/2007",
            "LANGUAGE": "EN",
            "MATCH": 1.0,
            "MATCH_DESCRIPTION": "EXACT",
            "DELIVERY_POINT_SUFFIX": "2A"
          }
        }
      ]}
    """.trimIndent()

    stubFor(
      WireMock.get(WireMock.urlEqualTo("/postcode?postcode=$postcode&key=$apiKey&offset=$offset&maxresults=$maxResults"))
        .willReturn(
          WireMock.aResponse().withHeader(
            "Content-Type",
            "application/json",
          ).withBody(
            json,
          ).withStatus(200),
        ),
    )
  }

  fun stubSearchForAddresses(searchQuery: String, offset: Int = 0, maxResults: Int = 100) {
    val json = """{
      "header": {
        "uri": "https://api.os.uk/search/places/v1/find?query=$searchQuery&offset=$offset&maxresults=$maxResults&lr=EN",
        "query": "query=$searchQuery",
        "offset": 0,
        "totalresults": 29,
        "format": "JSON",
        "dataset": "DPA",
        "lr": "EN,CY",
        "maxresults": 100,
        "epoch": "112",
        "lastupdate": "2024-09-20",
        "output_srs": "EPSG:27700"
      },
      "results": [
        {
          "DPA": {
            "UPRN": "23456789",
            "UDPRN": "23456789",
            "ADDRESS": "10, THE FAKE AV, FAKETON, FAKESHIRE, FA11KE",
            "BUILDING_NUMBER": "10",
            "THOROUGHFARE_NAME": "THE FAKE AV",
            "DEPENDENT_LOCALITY": "FAKETON",
            "POST_TOWN": "FAKESVILLE",
            "POSTCODE": "FA11KE",
            "RPC": "1",
            "X_COORDINATE": 304024.0,
            "Y_COORDINATE": 354212.0,
            "STATUS": "APPROVED",
            "LOGICAL_STATUS_CODE": "2",
            "CLASSIFICATION_CODE": "AL03",
            "CLASSIFICATION_CODE_DESCRIPTION": "Detached",
            "LOCAL_CUSTODIAN_CODE": 3140,
            "COUNTRY_CODE": "W",
            "COUNTRY_CODE_DESCRIPTION": "This record is within Wales",
            "POSTAL_ADDRESS_CODE": "D",
            "POSTAL_ADDRESS_CODE_DESCRIPTION": "A record which is linked to PAF",
            "BLPU_STATE_CODE": "2",
            "BLPU_STATE_CODE_DESCRIPTION": "In use",
            "TOPOGRAPHY_LAYER_TOID": "osgbTEST",
            "WARD_CODE": "E0TEST",
            "PARISH_CODE": "E0TEST",
            "LAST_UPDATE_DATE": "01/05/2021",
            "ENTRY_DATE": "15/01/2002",
            "BLPU_STATE_DATE": "12/12/2007",
            "LANGUAGE": "EN",
            "MATCH": 1.0,
            "MATCH_DESCRIPTION": "EXACT",
            "DELIVERY_POINT_SUFFIX": "1A"
          }
        },
        {
          "DPA": {
            "UPRN": "234567891",
            "UDPRN": "234567891",
            "ADDRESS": "12, THE FAKE AV, FAKETON, FAKESHIRE, FA11KE",
            "BUILDING_NUMBER": "12",
            "THOROUGHFARE_NAME": "THE FAKE AV",
            "DEPENDENT_LOCALITY": "FAKETON",
            "POST_TOWN": "FAKESVILLE",
            "POSTCODE": "FA11KE",
            "RPC": "1",
            "X_COORDINATE": 304024.0,
            "Y_COORDINATE": 354212.0,
            "STATUS": "APPROVED",
            "LOGICAL_STATUS_CODE": "2",
            "CLASSIFICATION_CODE": "AL03",
            "CLASSIFICATION_CODE_DESCRIPTION": "Detached",
            "LOCAL_CUSTODIAN_CODE": 3140,
            "LOCAL_CUSTODIAN_CODE_DESCRIPTION": "FAKESHIRE",
            "COUNTRY_CODE": "W",
            "COUNTRY_CODE_DESCRIPTION": "This record is within Wales",
            "POSTAL_ADDRESS_CODE": "D",
            "POSTAL_ADDRESS_CODE_DESCRIPTION": "A record which is linked to PAF",
            "BLPU_STATE_CODE": "2",
            "BLPU_STATE_CODE_DESCRIPTION": "In use",
            "TOPOGRAPHY_LAYER_TOID": "osgbTEST",
            "WARD_CODE": "E0TEST",
            "PARISH_CODE": "E0TEST",
            "LAST_UPDATE_DATE": "01/05/2021",
            "ENTRY_DATE": "15/01/2002",
            "BLPU_STATE_DATE": "12/12/2007",
            "LANGUAGE": "EN",
            "MATCH": 1.0,
            "MATCH_DESCRIPTION": "EXACT",
            "DELIVERY_POINT_SUFFIX": "1A"
          }
        }
      ]}
    """.trimIndent()

    stubFor(
      WireMock.get(WireMock.urlEqualTo("/find?query=$searchQuery&key=$apiKey&offset=$offset&maxresults=$maxResults&lr=EN"))
        .willReturn(
          WireMock.aResponse().withHeader(
            "Content-Type",
            "application/json",
          ).withBody(
            json,
          ).withStatus(200),
        ),
    )
  }

  fun stubGetAddressByUprn(uprn: String) {
    val json = """
      {
        "header": {
          "uri": "https://api.os.uk/search/places/v1/uprn?uprn=$uprn",
          "query": "uprn=$uprn",
          "offset": 0,
          "totalresults": 1,
          "format": "JSON",
          "dataset": "DPA",
          "lr": "EN,CY",
          "maxresults": 100,
          "epoch": "112",
          "lastupdate": "2024-09-23",
          "output_srs": "EPSG:27700"
        },
        "results": [{
          "DPA": {
            "UPRN": "$uprn",
            "UDPRN": "2345678",
            "ADDRESS": "ORDNANCE SURVEY, 4, FAKE DRIVE, FAKELINE, FAKESVILLE, FA2 2KE",
            "ORGANISATION_NAME": "ORDNANCE SURVEY",
            "BUILDING_NUMBER": "4",
            "THOROUGHFARE_NAME": "FAKE DRIVE",
            "DEPENDENT_LOCALITY": "FAKELINE",
            "POST_TOWN": "FAKESVILLE",
            "POSTCODE": "FA2 2KE",
            "RPC": "2",
            "X_COORDINATE": 437292.43,
            "Y_COORDINATE": 115541.95,
            "STATUS": "APPROVED",
            "LOGICAL_STATUS_CODE": "1",
            "CLASSIFICATION_CODE": "CO01GV",
            "CLASSIFICATION_CODE_DESCRIPTION": "Central Government Service",
            "LOCAL_CUSTODIAN_CODE": 1760,
            "LOCAL_CUSTODIAN_CODE_DESCRIPTION": "FAKEHAM",
            "COUNTRY_CODE": "E",
            "COUNTRY_CODE_DESCRIPTION": "This record is within Wales",
            "POSTAL_ADDRESS_CODE": "D",
            "POSTAL_ADDRESS_CODE_DESCRIPTION": "A record which is linked to PAF",
            "BLPU_STATE_CODE": "2",
            "BLPU_STATE_CODE_DESCRIPTION": "In use",
            "TOPOGRAPHY_LAYER_TOID": "osgb1000002682081995",
            "WARD_CODE": "E05012936",
            "PARISH_CODE": "E04004629",
            "LAST_UPDATE_DATE": "31/03/2020",
            "ENTRY_DATE": "01/09/2010",
            "BLPU_STATE_DATE": "01/09/2010",
            "LANGUAGE": "EN",
            "MATCH": 1.0,
            "MATCH_DESCRIPTION": "EXACT",
            "DELIVERY_POINT_SUFFIX": "1A"
          }
        }
      ]}
    """.trimIndent()

    stubFor(
      WireMock.get(WireMock.urlEqualTo("/uprn?uprn=$uprn&key=$apiKey"))
        .willReturn(
          WireMock.aResponse().withHeader(
            "Content-Type",
            "application/json",
          ).withBody(
            json,
          ).withStatus(200),
        ),
    )
  }
}
