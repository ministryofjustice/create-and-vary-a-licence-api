package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.RecordNomisTimeServedLicenceReason
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.NomisLicenceReasonFlag

@Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
@Repository
interface RecordNomisTimeServedLicenceReasonRepository : JpaRepository<RecordNomisTimeServedLicenceReason, Long> {

  fun findByNomsIdAndBookingId(nomsId: String, bookingId: Long): RecordNomisTimeServedLicenceReason?

  @Query(
    """
    SELECT new uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.NomisLicenceReasonFlag(r.bookingId, true)
    FROM RecordNomisTimeServedLicenceReason r
    WHERE r.bookingId IN :bookingIds
    GROUP BY r.bookingId
    """,
  )
  fun getNomisLicenceFlagsByBookingIds(@Param("bookingIds") bookingIds: List<String>): List<NomisLicenceReasonFlag>
}
