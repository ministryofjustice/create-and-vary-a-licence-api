package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase

object SearchFilter {
  fun apply(searchString: String?, cases: List<CaCase>): List<CaCase> {
    if (searchString == null) {
      return cases
    }
    val term = searchString.lowercase()
    return cases.filter {
      it.name.lowercase().contains(term) ||
        it.prisonerNumber.lowercase().contains(term) ||
        it.probationPractitioner?.name?.lowercase()?.contains(term) ?: false
    }
  }
}
