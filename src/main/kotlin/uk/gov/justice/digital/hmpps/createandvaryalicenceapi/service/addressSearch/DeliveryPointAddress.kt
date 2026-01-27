package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class DeliveryPointAddress(
  @field:JsonProperty("UPRN")
  val uprn: String,

  @field:JsonProperty("ADDRESS")
  val address: String,

  @field:JsonProperty("SUB_BUILDING_NAME")
  val subBuildingName: String? = null,

  @field:JsonProperty("ORGANISATION_NAME")
  val organisationName: String? = null,

  @field:JsonProperty("BUILDING_NAME")
  val buildingName: String? = null,

  @field:JsonProperty("BUILDING_NUMBER")
  val buildingNumber: String? = null,

  @field:JsonProperty("THOROUGHFARE_NAME")
  val thoroughfareName: String?,

  @field:JsonProperty("DEPENDENT_LOCALITY")
  val locality: String?,

  @field:JsonProperty("POST_TOWN")
  val postTown: String,

  @field:JsonProperty("LOCAL_CUSTODIAN_CODE_DESCRIPTION")
  val county: String?,

  @field:JsonProperty("POSTCODE")
  val postcode: String,

  @field:JsonProperty("COUNTRY_CODE_DESCRIPTION")
  val countryDescription: String,

  @field:JsonProperty("X_COORDINATE")
  val xCoordinate: Double,

  @field:JsonProperty("Y_COORDINATE")
  val yCoordinate: Double,

  @field:JsonFormat(pattern = "dd/MM/yyyy")
  @field:JsonProperty("LAST_UPDATE_DATE")
  val lastUpdateDate: LocalDate,
)
