package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import javax.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType.USER_EVENT

@Entity
@Table(name = "audit_event")
data class AuditEvent(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  val id: Long? = -1,

  val licenceId: Long? = null,

  val eventTime: LocalDateTime = LocalDateTime.now(),

  val username: String? = null,

  val fullName: String? = null,

  @Enumerated(EnumType.STRING)
  val eventType: AuditEventType = USER_EVENT,

  val summary: String? = null,

  val detail: String? = null,
)

