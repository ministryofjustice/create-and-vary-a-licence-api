package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.timeserved.TimeServedExternalRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.TimeServedExternalRecordFlags

@Repository
interface TimeServedExternalRecordsRepository : JpaRepository<TimeServedExternalRecord, Long> {

  fun findByNomsIdAndBookingId(nomsId: String, bookingId: Long): TimeServedExternalRecord?

  @Query(
    """
    SELECT t.bookingId, true AS hasNomisLicence FROM TimeServedExternalRecord t
        WHERE t.bookingId IN :bookingIds
    GROUP BY t.bookingId
    """,
  )
  fun getTimeServedExternalRecordFlags(@Param("bookingIds") bookingIds: List<String>): List<TimeServedExternalRecordFlags>
}
