package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffender
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName

object ManagedOffenderTransformer {
  fun ManagedOffender.toProbationPractitioner() = if (this.staff.unallocated == true) {
    ProbationPractitioner.unallocated(this.staff.code)
  } else {
    ProbationPractitioner(
      staffCode = this.staff.code,
      name = this.staff.name?.fullName(),
      allocated = true,
    )
  }
}
