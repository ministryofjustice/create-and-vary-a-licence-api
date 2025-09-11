package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

@Entity
@Table(name = "omu_contact")
data class OmuContact(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @field:Positive
  open val id: Long? = null,
  val prisonCode: String,
  var email: String,
  var dateCreated: LocalDateTime,
  var dateLastUpdated: LocalDateTime? = null,
)
