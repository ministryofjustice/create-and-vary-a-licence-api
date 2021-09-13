package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Repository
interface LicenceRepository : JpaRepository<Licence, Long> {

  fun findAllByNomsIdAndStatusCodeIn(nomsId: String, status: List<LicenceStatus>): List<Licence>
}
