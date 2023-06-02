package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

data class PrisonerSearchPrisoner(
  val prisonerNumber: String,
  val bookingId: String,
  val status: String? = null,
)
