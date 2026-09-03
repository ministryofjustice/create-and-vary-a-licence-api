package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch.dto

import java.time.LocalDate

interface OsCommonAddress {
  val uprn: String
  val address: String
  val subBuildingName: String?
  val organisationName: String?
  val buildingName: String?
  val buildingNumber: String?
  val thoroughfareName: String?
  val locality: String?
  val postTown: String?
  val county: String?
  val postcode: String
  val countryDescription: String
  val xCoordinate: Double
  val yCoordinate: Double
  val lastUpdateDate: LocalDate
  fun isDeliveryPointAddress(): Boolean = false
  fun isInvalid(): Boolean = false
}
