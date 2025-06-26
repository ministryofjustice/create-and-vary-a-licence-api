package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Repository
interface HardStopLicenceRepository :
  JpaRepository<HardStopLicence, Long>,
  JpaSpecificationExecutor<HardStopLicence> {

  fun findByKindAndReviewDateIsNullAndLicenceActivatedDateBetween(
    kind: LicenceKind = LicenceKind.HARD_STOP,
    start: LocalDateTime = LocalDate.now().minusDays(5).atStartOfDay(),
    end: LocalDateTime = start.toLocalDate().atTime(LocalTime.MAX),
  ): List<HardStopLicence>
}
