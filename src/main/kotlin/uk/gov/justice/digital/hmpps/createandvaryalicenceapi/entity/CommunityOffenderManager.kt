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
@Table(name = "community_offender_manager")
data class CommunityOffenderManager(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  val id: Long = -1,

  @Column(unique = true)
  val staffIdentifier: Long,

  @Column(unique = true)
  val username: String,

  val email: String?,

  val firstName: String?,

  val lastName: String?,

  val lastUpdatedTimestamp: LocalDateTime? = null,
)
