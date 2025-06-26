package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

@Repository
interface HdcLicenceRepository :
  JpaRepository<HdcLicence, Long>,
  JpaSpecificationExecutor<HdcLicence> {

  fun findByKindAndStatusCodeInAndConditionalReleaseDateLessThanEqual(
    kind: LicenceKind? = LicenceKind.HDC,
    statusCodes: List<LicenceStatus>? = listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED, LicenceStatus.APPROVED),
    date: LocalDate? = LocalDate.now().plusDays(9),
  ): List<HdcLicence>
}
