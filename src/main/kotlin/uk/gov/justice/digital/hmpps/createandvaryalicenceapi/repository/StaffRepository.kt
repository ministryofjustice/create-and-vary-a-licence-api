package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Staff

@Repository
interface StaffRepository : JpaRepository<Staff, Long> {
  fun findByStaffIdentifier(staffIdentifier: Long): CommunityOffenderManager?
  fun findByStaffIdentifierOrUsernameIgnoreCase(staffIdentifier: Long, username: String): List<CommunityOffenderManager>?
  fun findByUsernameIgnoreCase(username: String): Staff?
  fun findPrisonUserByUsernameIgnoreCase(username: String): PrisonUser?
}
