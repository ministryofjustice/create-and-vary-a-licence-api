package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.TimeServedExternalRecords
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.TimeServedExternalRecordsFlag

@Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
@Repository
interface TimeServedExternalRecordsRepository : JpaRepository<TimeServedExternalRecords, Long> {

  fun findByNomsIdAndBookingId(nomsId: String, bookingId: Long): TimeServedExternalRecords?

  @Query(
    """
    SELECT new uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.TimeServedExternalRecordsFlag(t.bookingId, true)
    FROM TimeServedExternalRecords t
    WHERE t.bookingId IN :bookingIds
    GROUP BY t.bookingId
    """,
  )
  fun getNomisLicenceFlagsByBookingIds(@Param("bookingIds") bookingIds: List<String>): List<TimeServedExternalRecordsFlag>
}
