package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

@Entity
@Table(name = "omu_contact")
data class OmuContact(
  @Id
  @param:NotNull
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  var id: Long = -1,

  val prisonCode: String,
  val email: String,
  var dateCreated: LocalDateTime,
  val dateLastUpdated: LocalDateTime? = null,
)
