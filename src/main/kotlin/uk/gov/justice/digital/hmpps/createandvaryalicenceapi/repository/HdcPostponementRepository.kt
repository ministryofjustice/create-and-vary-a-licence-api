package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcPostponement

interface HdcPostponementRepository : JpaRepository<HdcPostponement, Long> {
  fun findByLicenceId(licenceId: Long): HdcPostponement?
}
