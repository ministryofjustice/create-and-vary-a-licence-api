package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Staff

@Repository
interface StaffRepository : JpaRepository<Staff, Long> {
  fun findByStaffIdentifier(staffIdentifier: Long): CommunityOffenderManager?

  @Query(
    """
        SELECT s FROM CommunityOffenderManager s
            WHERE s.staffIdentifier = :staffIdentifier OR UPPER(s.username) = UPPER(:username)
            ORDER BY s.lastUpdatedTimestamp DESC
    """,
  )
  fun findCommunityOffenderManager(
    staffIdentifier: Long,
    username: String,
  ): List<CommunityOffenderManager>

  fun findByUsernameIgnoreCase(username: String): Staff?

  @Query(
    """
      SELECT s FROM PrisonUser s 
        WHERE UPPER(s.username) = UPPER(:username) 
        ORDER BY s.lastUpdatedTimestamp DESC LIMIT 1
    """,
  )
  fun findPrisonUserByUsernameIgnoreCase(username: String): PrisonUser?

  @Query(
    """
      SELECT s FROM Staff s
      LEFT JOIN s.savedAppointmentAddresses
      WHERE LOWER(s.username) = LOWER(:username)
  """,
  )
  fun findByUsernameIgnoreCaseWithAddresses(username: String): Staff?
}
