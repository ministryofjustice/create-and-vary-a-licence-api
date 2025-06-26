package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

@Repository
interface CrdLicenceRepository :
  JpaRepository<CrdLicence, Long>,
  JpaSpecificationExecutor<CrdLicence> {

  fun findByKindAndStatusCodeAndConditionalReleaseDateLessThanEqual(
    kind: LicenceKind = LicenceKind.CRD,
    statusCode: LicenceStatus = LicenceStatus.IN_PROGRESS,
    date: LocalDate = LocalDate.now().plusDays(14),
  ): List<CrdLicence>

  fun findAllByBookingIdInAndStatusCodeOrderByDateCreatedDesc(
    bookingId: List<Long>,
    status: LicenceStatus,
  ): List<CrdLicence>
}
