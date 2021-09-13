package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "licence_history")
data class LicenceHistory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  val id: Long = -1,

  @NotNull
  val licenceId: Long = -1,

  @NotNull
  val statusCode: String = "",

  @NotNull
  val actionTime: LocalDateTime = LocalDateTime.now(),

  val actionDescription: String? = null,
  val actionUsername: String? = null,
)
