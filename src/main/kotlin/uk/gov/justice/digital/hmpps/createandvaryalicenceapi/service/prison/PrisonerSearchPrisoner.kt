package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

data class PrisonerSearchPrisoner(
  val prisonerNumber: String,
  val bookingId: String,
  val status: String? = null,
  val licenceExpiryDate: String? = null,
  val topUpSupervisionExpiryDate: String? = null,
  val releaseDate: String? = null,
  val confirmedReleaseDate: String? = null,
)
