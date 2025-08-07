package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

@Entity
@Table(name = "electronic_monitoring_provider")
data class ElectronicMonitoringProvider(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @param:Positive
  val id: Long? = null,

  @OneToOne
  @JoinColumn(name = "licence_id", nullable = false, unique = true)
  val licence: Licence,

  @Column(name = "is_to_be_tagged_for_programme")
  var isToBeTaggedForProgramme: Boolean? = null,

  @Column(name = "programme_name")
  var programmeName: String? = null,

  @Column(name = "date_created", nullable = false, updatable = false)
  val dateCreated: LocalDateTime = LocalDateTime.now(),

  @Column(name = "date_last_updated", nullable = false)
  val dateLastUpdated: LocalDateTime = LocalDateTime.now(),
)
