package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class LocalPropertyIdentifierAddress(
  @field:JsonProperty("UPRN")
  override val uprn: String,

  @field:JsonProperty("ADDRESS")
  override val address: String,

  @field:JsonProperty("SAO_TEXT")
  override val subBuildingName: String? = null,

  @field:JsonProperty("ORGANISATION")
  override val organisationName: String? = null,

  @field:JsonProperty("PAO_TEXT")
  override val buildingName: String? = null,

  @field:JsonProperty("PAO_START_NUMBER")
  override val buildingNumber: String? = null,

  @field:JsonProperty("STREET_DESCRIPTION")
  override val thoroughfareName: String? = null,

  @field:JsonProperty("LOCALITY_NAME")
  override val locality: String? = null,

  @field:JsonProperty("TOWN_NAME")
  override val postTown: String? = null,

  @field:JsonProperty("ADMINISTRATIVE_AREA")
  override val county: String? = null,

  @field:JsonProperty("POSTCODE_LOCATOR")
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

  // LPI only fields

  @field:JsonProperty("RPC")
  val rpc: String? = null,

  @field:JsonProperty("STATUS")
  val status: String? = null,

  @field:JsonProperty("LOGICAL_STATUS_CODE")
  val logicalStatusCode: String? = null,

  @field:JsonProperty("CLASSIFICATION_CODE")
  val classificationCode: String? = null,

  @field:JsonProperty("CLASSIFICATION_CODE_DESCRIPTION")
  val classificationCodeDescription: String? = null,

  @field:JsonProperty("LOCAL_CUSTODIAN_CODE")
  val localCustodianCode: Int? = null,

  @field:JsonProperty("COUNTRY_CODE")
  val countryCode: String? = null,

  @field:JsonProperty("POSTAL_ADDRESS_CODE")
  val postalAddressCode: String? = null,

  @field:JsonProperty("POSTAL_ADDRESS_CODE_DESCRIPTION")
  val postalAddressCodeDescription: String? = null,

  @field:JsonProperty("BLPU_STATE_CODE")
  val blpuStateCode: String? = null,

  @field:JsonProperty("BLPU_STATE_CODE_DESCRIPTION")
  val blpuStateCodeDescription: String? = null,

  @field:JsonProperty("TOPOGRAPHY_LAYER_TOID")
  val topographyLayerToid: String? = null,

  @field:JsonProperty("WARD_CODE")
  val wardCode: String? = null,

  @field:JsonProperty("PARISH_CODE")
  val parishCode: String? = null,

  @field:JsonProperty("PARENT_UPRN")
  val parentUprn: String? = null,

  @field:JsonFormat(pattern = "dd/MM/yyyy")
  @field:JsonProperty("ENTRY_DATE")
  val entryDate: LocalDate? = null,

  @field:JsonFormat(pattern = "dd/MM/yyyy")
  @field:JsonProperty("BLPU_STATE_DATE")
  val blpuStateDate: LocalDate? = null,

  @field:JsonProperty("STREET_STATE_CODE")
  val streetStateCode: String? = null,

  @field:JsonProperty("STREET_STATE_CODE_DESCRIPTION")
  val streetStateCodeDescription: String? = null,

  @field:JsonProperty("STREET_CLASSIFICATION_CODE")
  val streetClassificationCode: String? = null,

  @field:JsonProperty("STREET_CLASSIFICATION_CODE_DESCRIPTION")
  val streetClassificationCodeDescription: String? = null,

  @field:JsonProperty("LPI_LOGICAL_STATUS_CODE")
  val lpiLogicalStatusCode: String? = null,

  @field:JsonProperty("LPI_LOGICAL_STATUS_CODE_DESCRIPTION")
  val lpiLogicalStatusCodeDescription: String? = null,

  @field:JsonProperty("LANGUAGE")
  val language: String? = null,

  @field:JsonProperty("MATCH")
  val match: Double? = null,

  @field:JsonProperty("MATCH_DESCRIPTION")
  val matchDescription: String? = null,
) : OsCommonAddress {
  override fun isInvalid(): Boolean = lpiLogicalStatusCodeDescription.equals("HISTORICAL", ignoreCase = true)
}
