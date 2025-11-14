package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.timeserved

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.timeserved.TimeServedProbationConfirmContact

@Repository
interface TimeServedProbationConfirmContactRepository : JpaRepository<TimeServedProbationConfirmContact, Long> {
  fun findByLicenceId(licenceId: Long): TimeServedProbationConfirmContact?
}
