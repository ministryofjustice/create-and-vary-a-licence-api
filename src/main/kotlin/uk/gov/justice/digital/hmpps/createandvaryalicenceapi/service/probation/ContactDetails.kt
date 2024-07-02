package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

class ContactDetails(
  val addresses: List<Address>?,
  val allowSMS: Boolean?,
  val emailAddresses: List<String>?,
  val phoneNumbers: List<PhoneNumber>?,
)
