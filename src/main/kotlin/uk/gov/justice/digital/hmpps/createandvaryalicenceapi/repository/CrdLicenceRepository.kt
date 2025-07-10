package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Repository
interface CrdLicenceRepository :
  JpaRepository<CrdLicence, Long>,
  JpaSpecificationExecutor<CrdLicence> {

  fun findAllByBookingIdInAndStatusCodeOrderByDateCreatedDesc(
    bookingId: List<Long>,
    status: LicenceStatus,
  ): List<CrdLicence>
}
