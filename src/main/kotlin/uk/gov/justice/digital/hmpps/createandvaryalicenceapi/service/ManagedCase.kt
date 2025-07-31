package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseLoadLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationCase

@Schema(description = "Describes an CA(OMU) caseload")
data class ManagedCase(
  @field:Schema(description = "Details about a offender")
  val deliusRecord: DeliusRecord? = null,

  @field:Schema(description = "Details about a prisoner")
  val nomisRecord: Prisoner? = null,

  @field:Schema(description = "Describes a licence within this service, A discriminator exists to distinguish between different types of licence")
  val licences: List<CaseLoadLicenceSummary> = emptyList(),

  @field:Schema(description = "Describes a probation practitioner on an approval case")
  val probationPractitioner: ProbationPractitioner? = null,

  @field:Schema(description = "Additional information pertinent to CVL")
  val cvlFields: CvlFields,
)

data class DeliusRecord(val probationCase: ProbationCase, val managedOffenderCrn: ManagedOffenderCrn)
