package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "record_nomis_time_served_licence")
class RecordNomisTimeServedLicence(
  @Column(name = "noms_id", length = 7, nullable = false)
  val nomsId: String,

  @Column(name = "booking_id", nullable = false)
  val bookingId: Long,

  @Column(name = "reason", nullable = false)
  var reason: String,

  @Column(name = "prison_code", length = 3)
  var prisonCode: String,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "updated_by_ca_id", nullable = false)
  var updatedByCa: Staff,

  @Column(name = "date_created", nullable = false, updatable = false)
  val dateCreated: LocalDateTime = LocalDateTime.now(),

  @Column(name = "date_last_updated", nullable = false)
  var dateLastUpdated: LocalDateTime = LocalDateTime.now(),

) : AbstractIdEntity()
