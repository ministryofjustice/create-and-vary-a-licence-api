package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseLoadLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationCase

data class ManagedCaseDto(
  val deliusRecord: DeliusRecord? = null,

  val nomisRecord: Prisoner? = null,

  val licences: List<CaseLoadLicenceSummary> = emptyList(),

  val probationPractitioner: ProbationPractitioner? = null,

  val cvlFields: CvlFields,
)

data class DeliusRecord(val probationCase: ProbationCase, val managedOffenderCrn: ManagedOffenderCrn)
