package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseLoadLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import java.time.LocalDate

data class ManagedCaseDto(
  val deliusRecord: DeliusRecord,

  val nomisRecord: PrisonerSearchPrisoner,

  val licences: List<CaseLoadLicenceSummary> = emptyList(),

  val probationPractitioner: ProbationPractitioner? = null,

  val cvlRecord: ManagedCaseCvlRecord,
)

data class DeliusRecord(val probationCase: ProbationCase, val managedOffenderCrn: ManagedOffenderCrn)

data class ManagedCaseCvlRecord(
  val isEligible: Boolean,
  val eligibleKind: LicenceKind?,
  val licenceStartDate: LocalDate?,
)
