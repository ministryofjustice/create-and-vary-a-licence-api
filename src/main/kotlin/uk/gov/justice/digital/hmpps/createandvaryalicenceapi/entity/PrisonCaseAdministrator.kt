package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.StaffKind
import java.time.LocalDateTime

@Entity
@DiscriminatorValue(value = "PRISON_CASE_ADMIN")
class PrisonCaseAdministrator(
  id: Long = -1,
  username: String,
  email: String?,
  firstName: String?,
  lastName: String?,
  lastUpdatedTimestamp: LocalDateTime? = null,
) : Creator, Staff(
  id = id,
  kind = StaffKind.PRISON_CASE_ADMIN,
  username = username,
  email = email,
  firstName = firstName,
  lastName = lastName,
  lastUpdatedTimestamp = lastUpdatedTimestamp,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is PrisonCaseAdministrator) return false
    if (!super.equals(other)) return false
    return true
  }

  override fun hashCode(): Int {
    return super.hashCode()
  }

  override fun toString(): String {
    return "PrisonCaseAdministrator()" +
      " ${super.toString()}"
  }
}
