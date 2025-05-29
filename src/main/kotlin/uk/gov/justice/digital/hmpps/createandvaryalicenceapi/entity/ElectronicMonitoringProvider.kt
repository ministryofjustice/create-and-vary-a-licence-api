package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.OneToOne
import jakarta.persistence.JoinColumn
import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "electronic_monitoring_provider")
data class ElectronicMonitoringProvider(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0,

  @OneToOne
  @JoinColumn(name = "licence_id", nullable = false, unique = true)
  val licence: Licence,

  @Column(name = "is_to_be_tagged_for_programme")
  val isToBeTaggedForProgramme: Boolean? = null,

  @Column(name = "programme_name", length = 100)
  val programmeName: String? = null,

  @Column(name = "create_date", nullable = false, updatable = false)
  val dateCreated: LocalDateTime = LocalDateTime.now(),

  @Column(name = "last_update_date", nullable = false)
  val dateLastUpdated: LocalDateTime = LocalDateTime.now()
)
