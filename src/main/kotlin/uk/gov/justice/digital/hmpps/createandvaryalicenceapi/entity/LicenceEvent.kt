package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import java.time.LocalDateTime

@Entity
@Table(name = "licence_event")
data class LicenceEvent(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  val id: Long = -1,

  @NotNull
  val licenceId: Long = -1,

  @Enumerated(EnumType.STRING)
  val eventType: LicenceEventType,

  val username: String? = null,
  val forenames: String? = null,
  val surname: String? = null,
  val eventDescription: String? = null,

  @NotNull
  val eventTime: LocalDateTime = LocalDateTime.now(),
)
