package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.RecordNomisTimeServedLicence

@Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
@Repository
interface NomisTimeServedLicenceRepository : JpaRepository<RecordNomisTimeServedLicence, Long> {

  fun findByNomsIdAndBookingId(nomsId: String, bookingId: Long): RecordNomisTimeServedLicence?
}
