package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName

object ManagedOffenderCrnTransformer {
  fun ManagedOffenderCrn.toProbationPractitioner(): ProbationPractitioner {
    if (this.staff == null) return ProbationPractitioner.UNALLOCATED
    return this.staff.let {
      if (it.unallocated == true) return@let ProbationPractitioner.unallocated(it.code)
      ProbationPractitioner(
        staffCode = it.code,
        name = it.name?.fullName(),
        allocated = true,
      )
    }
  }
}
