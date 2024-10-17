package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcCurfewTimes

@Repository
interface HdcCurfewTimesRepository : JpaRepository<HdcCurfewTimes, Long> {
  fun findByLicenceId(licenceId: Long?): List<HdcCurfewTimes?>
}
