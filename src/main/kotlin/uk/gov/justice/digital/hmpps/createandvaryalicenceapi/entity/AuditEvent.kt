package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.Positive
import org.hibernate.annotations.Type
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType.USER_EVENT
import java.time.LocalDateTime

@Entity
@Table(name = "audit_event")
data class AuditEvent(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @field:Positive
  open val id: Long? = null,

  val licenceId: Long? = null,

  val eventTime: LocalDateTime = LocalDateTime.now(),

  val username: String? = null,

  val fullName: String? = null,

  @Enumerated(EnumType.STRING)
  val eventType: AuditEventType = USER_EVENT,

  val summary: String? = null,

  val detail: String? = null,

  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb")
  val changes: Map<String, Any>? = null,
)
