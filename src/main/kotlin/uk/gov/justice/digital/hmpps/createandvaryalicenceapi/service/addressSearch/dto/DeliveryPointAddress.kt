package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class DeliveryPointAddress(
  @field:JsonProperty("UPRN")
  override val uprn: String,

  @field:JsonProperty("ADDRESS")
  override val address: String,

  @field:JsonProperty("SUB_BUILDING_NAME")
  override val subBuildingName: String? = null,

  @field:JsonProperty("ORGANISATION_NAME")
  override val organisationName: String? = null,

  @field:JsonProperty("BUILDING_NAME")
  override val buildingName: String? = null,

  @field:JsonProperty("BUILDING_NUMBER")
  override val buildingNumber: String? = null,

  @field:JsonProperty("THOROUGHFARE_NAME")
  override val thoroughfareName: String?,

  @field:JsonProperty("DEPENDENT_LOCALITY")
  override val locality: String?,

  @field:JsonProperty("POST_TOWN")
  override val postTown: String,

  @field:JsonProperty("LOCAL_CUSTODIAN_CODE_DESCRIPTION")
  override val county: String?,

  @field:JsonProperty("POSTCODE")
  override val postcode: String,

  @field:JsonProperty("COUNTRY_CODE_DESCRIPTION")
  override val countryDescription: String,

  @field:JsonProperty("X_COORDINATE")
  override val xCoordinate: Double,

  @field:JsonProperty("Y_COORDINATE")
  override val yCoordinate: Double,

  @field:JsonFormat(pattern = "dd/MM/yyyy")
  @field:JsonProperty("LAST_UPDATE_DATE")
  override val lastUpdateDate: LocalDate,
) : OsCommonAddress
