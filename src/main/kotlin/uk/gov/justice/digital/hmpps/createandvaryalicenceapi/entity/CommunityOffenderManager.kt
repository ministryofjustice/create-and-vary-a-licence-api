package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.StaffKind
import java.time.LocalDateTime

@Entity
@DiscriminatorValue(value = "COMMUNITY_OFFENDER_MANAGER")
class CommunityOffenderManager(
  id: Long? = null,
  var staffIdentifier: Long,
  username: String,
  email: String?,
  firstName: String?,
  lastName: String?,
  lastUpdatedTimestamp: LocalDateTime? = null,
) : Staff(
  id = id,
  kind = StaffKind.COMMUNITY_OFFENDER_MANAGER,
  username = username,
  email = email,
  firstName = firstName,
  lastName = lastName,
  lastUpdatedTimestamp = lastUpdatedTimestamp,
),
  Creator {
  fun copy(
    id: Long? = this.id,
    staffIdentifier: Long = this.staffIdentifier,
    username: String = this.username,
    email: String? = this.email,
    firstName: String? = this.firstName,
    lastName: String? = this.lastName,
    lastUpdatedTimestamp: LocalDateTime? = this.lastUpdatedTimestamp,
  ) = CommunityOffenderManager(
    id = id,
    staffIdentifier = staffIdentifier,
    username = username,
    email = email,
    firstName = firstName,
    lastName = lastName,
    lastUpdatedTimestamp = lastUpdatedTimestamp,
  )

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CommunityOffenderManager) return false
    if (!super.equals(other)) return false
    return true
  }

  override fun hashCode(): Int = super.hashCode()

  override fun toString(): String = "CommunityOffenderManager(" +
    "staffIdentifier=$staffIdentifier" +
    ")" +
    " ${super.toString()}"
}
