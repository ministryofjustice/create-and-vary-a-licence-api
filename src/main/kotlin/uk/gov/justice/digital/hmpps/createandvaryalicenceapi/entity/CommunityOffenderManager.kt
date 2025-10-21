package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.StaffKind
import java.time.LocalDateTime

@Entity
@DiscriminatorValue(value = "COMMUNITY_OFFENDER_MANAGER")
class CommunityOffenderManager(
  id: Long? = null,
  var staffIdentifier: Long,
  @Column(name = "delius_staff_code")
  var staffCode: String?,
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
    staffCode: String? = this.staffCode,
    username: String = this.username,
    email: String? = this.email,
    firstName: String? = this.firstName,
    lastName: String? = this.lastName,
    lastUpdatedTimestamp: LocalDateTime? = this.lastUpdatedTimestamp,
  ) = CommunityOffenderManager(
    id = id,
    staffIdentifier = staffIdentifier,
    staffCode = staffCode,
    username = username,
    email = email,
    firstName = firstName,
    lastName = lastName,
    lastUpdatedTimestamp = lastUpdatedTimestamp,
  )

  override fun toString(): String = "${javaClass.simpleName}(" +
    "id=$id, " +
    "staffIdentifier=$staffIdentifier, " +
    "staffCode=$staffCode," +
    "kind=$kind, " +
    "username='$username', " +
    "email=$email, " +
    "firstName=$firstName, " +
    "lastName=$lastName, " +
    "lastUpdatedTimestamp=$lastUpdatedTimestamp" +
    ")"
}
