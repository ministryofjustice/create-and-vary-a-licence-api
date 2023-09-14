package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.model.request

data class PrisonerSearchByPrisonerNumbersRequest(
  val prisonerNumbers: List<String?>,
)
