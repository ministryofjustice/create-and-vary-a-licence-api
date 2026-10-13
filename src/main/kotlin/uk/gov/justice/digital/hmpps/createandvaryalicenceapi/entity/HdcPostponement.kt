package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "hdc_postponement")
class HdcPostponement(
  @OneToOne(
    cascade = [CascadeType.DETACH],
    fetch = FetchType.LAZY,
  )
  @JoinColumn(name = "licence_id", nullable = false, unique = true)
  val licence: HdcLicence,

  @Column(name = "is_postponed", nullable = false)
  var isPostponed: Boolean,

  @CreationTimestamp
  @Column(name = "date_created", nullable = false, updatable = false)
  val dateCreated: LocalDateTime? = null,

  @UpdateTimestamp
  @Column(name = "date_last_updated", nullable = false)
  var dateLastUpdated: LocalDateTime? = null,
) : AbstractIdEntity()
