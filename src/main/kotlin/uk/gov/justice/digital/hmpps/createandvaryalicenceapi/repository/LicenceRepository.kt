package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Repository
interface LicenceRepository : JpaRepository<Licence, Long>, JpaSpecificationExecutor<Licence> {
  fun findAllByNomsIdAndStatusCodeIn(nomsId: String, status: List<LicenceStatus>): List<Licence>
  fun findAllByComStaffIdAndStatusCodeIn(staffId: Long, status: List<LicenceStatus>): List<Licence>
  fun findAllByComStaffId(staffId: Long): List<Licence>
  fun findAllByStatusCodeAndPrisonCodeIn(statusCode: LicenceStatus, prisonCaseload: List<String>): List<Licence>
  fun findAllByStatusCode(statusCode: LicenceStatus): List<Licence>
}
