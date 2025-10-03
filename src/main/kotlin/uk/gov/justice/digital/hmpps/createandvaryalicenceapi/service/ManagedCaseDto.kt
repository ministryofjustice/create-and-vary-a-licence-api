package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseLoadLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationCase
import java.time.LocalDate

data class ManagedCaseDto(
  val deliusRecord: DeliusRecord? = null,

  val nomisRecord: PrisonerSearchPrisoner? = null,

  val licences: List<CaseLoadLicenceSummary> = emptyList(),

  val probationPractitioner: ProbationPractitioner? = null,

  val licenceStartDate: LocalDate? = null,
)

data class DeliusRecord(val probationCase: ProbationCase, val managedOffenderCrn: ManagedOffenderCrn)
