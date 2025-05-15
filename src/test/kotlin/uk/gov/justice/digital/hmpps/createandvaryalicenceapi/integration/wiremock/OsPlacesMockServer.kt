package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock

private const val OS_PLACES_WIREMOCK_PORT = 8099

class OsPlacesMockServer(private val apiKey: String) : WireMockServer(OS_PLACES_WIREMOCK_PORT) {

  fun stubGetAddressesForPostcode(postcode: String) {
    val json = """{
      "header": {
        "uri": "https://api.os.uk/search/places/v1/postcode?postcode=$postcode",
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
            "UPRN": "100120991537",
            "UDPRN": "22659267",
            "ADDRESS": "1, THE STREET, MORTIMER, READING, AG12 1RW",
            "BUILDING_NUMBER": "1",
            "THOROUGHFARE_NAME": "THE STREET",
            "DEPENDENT_LOCALITY": "MORTIMER",
            "POST_TOWN": "READING",
            "POSTCODE": "SA420UQ",
            "RPC": "1",
            "X_COORDINATE": 401024.0,
            "Y_COORDINATE": 154112.0,
            "STATUS": "APPROVED",
            "LOGICAL_STATUS_CODE": "1",
            "CLASSIFICATION_CODE": "RD03",
            "CLASSIFICATION_CODE_DESCRIPTION": "Semi-Detached",
            "LOCAL_CUSTODIAN_CODE": 3940,
            "LOCAL_CUSTODIAN_CODE_DESCRIPTION": "WILTSHIRE",
            "COUNTRY_CODE": "E",
            "COUNTRY_CODE_DESCRIPTION": "This record is within England",
            "POSTAL_ADDRESS_CODE": "D",
            "POSTAL_ADDRESS_CODE_DESCRIPTION": "A record which is linked to PAF",
            "BLPU_STATE_CODE": "2",
            "BLPU_STATE_CODE_DESCRIPTION": "In use",
            "TOPOGRAPHY_LAYER_TOID": "osgb1000015844938",
            "WARD_CODE": "E05013472",
            "PARISH_CODE": "E04011768",
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
            "UPRN": "100120991538",
            "UDPRN": "22659277",
            "ADDRESS": "2, A ROAD, EASTERTON, READING, AG12 1RW",
            "BUILDING_NUMBER": "2",
            "THOROUGHFARE_NAME": "A ROAD",
            "DEPENDENT_LOCALITY": "EASTERTON",
            "POST_TOWN": "READING",
            "POSTCODE": "SA420UQ",
            "RPC": "1",
            "X_COORDINATE": 401017.0,
            "Y_COORDINATE": 154112.0,
            "STATUS": "APPROVED",
            "LOGICAL_STATUS_CODE": "1",
            "CLASSIFICATION_CODE": "RD03",
            "CLASSIFICATION_CODE_DESCRIPTION": "Semi-Detached",
            "LOCAL_CUSTODIAN_CODE": 3940,
            "LOCAL_CUSTODIAN_CODE_DESCRIPTION": "WILTSHIRE",
            "COUNTRY_CODE": "E",
            "COUNTRY_CODE_DESCRIPTION": "This record is within England",
            "POSTAL_ADDRESS_CODE": "D",
            "POSTAL_ADDRESS_CODE_DESCRIPTION": "A record which is linked to PAF",
            "BLPU_STATE_CODE": "2",
            "BLPU_STATE_CODE_DESCRIPTION": "In use",
            "TOPOGRAPHY_LAYER_TOID": "osgb1000015844937",
            "WARD_CODE": "E05013472",
            "PARISH_CODE": "E04011768",
            "LAST_UPDATE_DATE": "01/05/2021",
            "ENTRY_DATE": "15/01/2002",
            "BLPU_STATE_DATE": "12/12/2007",
            "LANGUAGE": "EN",
            "MATCH": 1.0,
            "MATCH_DESCRIPTION": "EXACT",
            "DELIVERY_POINT_SUFFIX": "1P"
          }
        },
        {
          "DPA": {
            "UPRN": "100120991539",
            "UDPRN": "22659286",
            "ADDRESS": "3, PARK ROAD, WHITLEY, READING, AG12 1RW",
            "BUILDING_NUMBER": "3",
            "THOROUGHFARE_NAME": "PARK ROAD",
            "DEPENDENT_LOCALITY": "WHITLEY",
            "POST_TOWN": "READING",
            "POSTCODE": "SA420UQ",
            "RPC": "1",
            "X_COORDINATE": 401003.0,
            "Y_COORDINATE": 154111.0,
            "STATUS": "APPROVED",
            "LOGICAL_STATUS_CODE": "1",
            "CLASSIFICATION_CODE": "RD03",
            "CLASSIFICATION_CODE_DESCRIPTION": "Semi-Detached",
            "LOCAL_CUSTODIAN_CODE": 3940,
            "LOCAL_CUSTODIAN_CODE_DESCRIPTION": "WILTSHIRE",
            "COUNTRY_CODE": "E",
            "COUNTRY_CODE_DESCRIPTION": "This record is within England",
            "POSTAL_ADDRESS_CODE": "D",
            "POSTAL_ADDRESS_CODE_DESCRIPTION": "A record which is linked to PAF",
            "BLPU_STATE_CODE": "2",
            "BLPU_STATE_CODE_DESCRIPTION": "In use",
            "TOPOGRAPHY_LAYER_TOID": "osgb1000015844933",
            "WARD_CODE": "E05013472",
            "PARISH_CODE": "E04011768",
            "LAST_UPDATE_DATE": "01/05/2021",
            "ENTRY_DATE": "15/01/2002",
            "BLPU_STATE_DATE": "12/12/2007",
            "LANGUAGE": "EN",
            "MATCH": 1.0,
            "MATCH_DESCRIPTION": "EXACT",
            "DELIVERY_POINT_SUFFIX": "1Y"
          }
        }
      ]}
    """.trimIndent()

    stubFor(
      WireMock.get(WireMock.urlEqualTo("/postcode?postcode=$postcode&key=$apiKey"))
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

  fun stubSearchForAddresses(searchQuery: String) {
    val json = """{
      "header": {
        "uri": "https://api.os.uk/search/places/v1/find?query=$searchQuery",
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
            "UPRN": "100120991537",
            "UDPRN": "22659267",
            "ADDRESS": "10, THE STREET, MORTIMER, READING, AG12 1RW",
            "BUILDING_NUMBER": "10",
            "THOROUGHFARE_NAME": "THE STREET",
            "DEPENDENT_LOCALITY": "MORTIMER",
            "POST_TOWN": "READING",
            "POSTCODE": "SA420UQ",
            "RPC": "1",
            "X_COORDINATE": 401024.0,
            "Y_COORDINATE": 154112.0,
            "STATUS": "APPROVED",
            "LOGICAL_STATUS_CODE": "1",
            "CLASSIFICATION_CODE": "RD03",
            "CLASSIFICATION_CODE_DESCRIPTION": "Semi-Detached",
            "LOCAL_CUSTODIAN_CODE": 3940,
            "LOCAL_CUSTODIAN_CODE_DESCRIPTION": "WILTSHIRE",
            "COUNTRY_CODE": "E",
            "COUNTRY_CODE_DESCRIPTION": "This record is within England",
            "POSTAL_ADDRESS_CODE": "D",
            "POSTAL_ADDRESS_CODE_DESCRIPTION": "A record which is linked to PAF",
            "BLPU_STATE_CODE": "2",
            "BLPU_STATE_CODE_DESCRIPTION": "In use",
            "TOPOGRAPHY_LAYER_TOID": "osgb1000015844938",
            "WARD_CODE": "E05013472",
            "PARISH_CODE": "E04011768",
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
            "UPRN": "100120991538",
            "UDPRN": "22659277",
            "ADDRESS": "11, A ROAD, EASTERTON, READING, AG12 1RW",
            "BUILDING_NUMBER": "12",
            "THOROUGHFARE_NAME": "A ROAD",
            "DEPENDENT_LOCALITY": "EASTERTON",
            "POST_TOWN": "READING",
            "POSTCODE": "SA420UQ",
            "RPC": "1",
            "X_COORDINATE": 401017.0,
            "Y_COORDINATE": 154112.0,
            "STATUS": "APPROVED",
            "LOGICAL_STATUS_CODE": "1",
            "CLASSIFICATION_CODE": "RD03",
            "CLASSIFICATION_CODE_DESCRIPTION": "Semi-Detached",
            "LOCAL_CUSTODIAN_CODE": 3940,
            "LOCAL_CUSTODIAN_CODE_DESCRIPTION": "WILTSHIRE",
            "COUNTRY_CODE": "E",
            "COUNTRY_CODE_DESCRIPTION": "This record is within England",
            "POSTAL_ADDRESS_CODE": "D",
            "POSTAL_ADDRESS_CODE_DESCRIPTION": "A record which is linked to PAF",
            "BLPU_STATE_CODE": "2",
            "BLPU_STATE_CODE_DESCRIPTION": "In use",
            "TOPOGRAPHY_LAYER_TOID": "osgb1000015844937",
            "WARD_CODE": "E05013472",
            "PARISH_CODE": "E04011768",
            "LAST_UPDATE_DATE": "01/05/2021",
            "ENTRY_DATE": "15/01/2002",
            "BLPU_STATE_DATE": "12/12/2007",
            "LANGUAGE": "EN",
            "MATCH": 1.0,
            "MATCH_DESCRIPTION": "EXACT",
            "DELIVERY_POINT_SUFFIX": "1P"
          }
        }
      ]}
    """.trimIndent()

    stubFor(
      WireMock.get(WireMock.urlEqualTo("/find?query=$searchQuery&key=$apiKey"))
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
            "UDPRN": "52126562",
            "ADDRESS": "ORDNANCE SURVEY, 4, ADANAC DRIVE, NURSLING, SOUTHAMPTON, SO16 0AS",
            "ORGANISATION_NAME": "ORDNANCE SURVEY",
            "BUILDING_NUMBER": "4",
            "THOROUGHFARE_NAME": "ADANAC DRIVE",
            "DEPENDENT_LOCALITY": "NURSLING",
            "POST_TOWN": "SOUTHAMPTON",
            "POSTCODE": "SO16 0AS",
            "RPC": "2",
            "X_COORDINATE": 437292.43,
            "Y_COORDINATE": 115541.95,
            "STATUS": "APPROVED",
            "LOGICAL_STATUS_CODE": "1",
            "CLASSIFICATION_CODE": "CO01GV",
            "CLASSIFICATION_CODE_DESCRIPTION": "Central Government Service",
            "LOCAL_CUSTODIAN_CODE": 1760,
            "LOCAL_CUSTODIAN_CODE_DESCRIPTION": "TEST VALLEY",
            "COUNTRY_CODE": "E",
            "COUNTRY_CODE_DESCRIPTION": "This record is within England",
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
