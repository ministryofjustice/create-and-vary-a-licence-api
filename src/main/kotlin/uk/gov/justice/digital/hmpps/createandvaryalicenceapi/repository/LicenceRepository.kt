package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Repository
interface LicenceRepository : JpaRepository<Licence, Long>, JpaSpecificationExecutor<Licence> {
  fun findAllByNomsIdAndStatusCodeIn(nomsId: String, status: List<LicenceStatus>): List<Licence>
  fun findAllByCrnAndStatusCodeIn(crn: String, status: List<LicenceStatus>): List<Licence>
}
