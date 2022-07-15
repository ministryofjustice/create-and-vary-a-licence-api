package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "omu_contact")
data class OmuContact(
  @Id
  @NotNull
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  var id: Long? = null,

  val prisonCode: String,
  val email: String,
  var dateCreated: LocalDateTime,
  val dateLastUpdated: LocalDateTime? = null,
)
