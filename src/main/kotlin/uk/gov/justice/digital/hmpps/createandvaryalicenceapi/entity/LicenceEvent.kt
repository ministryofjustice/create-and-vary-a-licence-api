package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import javax.validation.constraints.NotNull

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
  val eventType: LicenceEventType? = null,

  val username: String? = null,
  val forenames: String? = null,
  val surname: String? = null,
  val eventDescription: String? = null,

  @NotNull
  val eventTime: LocalDateTime = LocalDateTime.now(),
)
