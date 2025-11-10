package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model

interface EditedLicenceNotReApproved {
  fun getCrn(): String?
  fun getForename(): String?
  fun getSurname(): String?
  fun getComFirstName(): String?
  fun getComLastName(): String?
  fun getComEmail(): String?
}
