package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.timeserved

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AbstractIdEntity
import java.time.LocalDateTime

@Entity
@Table(name = "time_served_probation_confirm_contact")
class TimeServedProbationConfirmContact(
  @Column(name = "licence_id", nullable = false, unique = true)
  var licenceId: Long,

  @Enumerated(EnumType.STRING)
  @Column(name = "contact_status", nullable = false)
  var contactStatus: ContactStatus,

  @Column(name = "communication_methods", nullable = false)
  @Convert(converter = CommunicationMethodListConverter::class)
  var communicationMethods: List<CommunicationMethod> = emptyList(),

  @Column(name = "other_detail")
  var otherDetail: String? = null,

  @Column(name = "confirmed_by_username", nullable = false)
  var confirmedByUsername: String? = null,

  @Column(name = "date_created", nullable = false)
  var dateCreated: LocalDateTime = LocalDateTime.now(),

  @Column(name = "date_last_updated", nullable = false)
  var dateLastUpdated: LocalDateTime = LocalDateTime.now(),
) : AbstractIdEntity()
