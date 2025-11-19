package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName

object ManagedOffenderCrnTransformer {
  fun ManagedOffenderCrn?.toProbationPractitioner() = this?.staff
    ?.takeUnless { it.unallocated == true }
    ?.let {
      ProbationPractitioner(
        staffCode = it.code,
        name = it.name?.fullName(),
      )
    }
}
