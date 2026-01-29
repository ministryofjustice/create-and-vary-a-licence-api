package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.timeserved.TimeServedExternalRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.TimeServedExternalSummaryRecord

@Repository
interface TimeServedExternalRecordsRepository : JpaRepository<TimeServedExternalRecord, Long> {

  fun findByNomsIdAndBookingId(nomsId: String, bookingId: Long): TimeServedExternalRecord?

  fun findAllByNomsId(nomsId: String): List<TimeServedExternalRecord>

  @Query(
    """
    SELECT t.bookingId, t.updatedByCa.firstName, t.updatedByCa.lastName FROM TimeServedExternalRecord t
        WHERE t.bookingId IN :bookingIds
        ORDER BY t.id DESC
    """,
  )
  fun getTimeServedExternalSummaryRecords(@Param("bookingIds") bookingIds: List<String>): List<TimeServedExternalSummaryRecord>
}
