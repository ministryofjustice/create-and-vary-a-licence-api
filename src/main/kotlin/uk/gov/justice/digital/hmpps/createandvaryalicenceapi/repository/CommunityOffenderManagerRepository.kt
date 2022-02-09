package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager

@Repository
interface CommunityOffenderManagerRepository : JpaRepository<CommunityOffenderManager, Long> {
  fun findByStaffIdentifier(staffIdentifier: Long): CommunityOffenderManager?
  fun findByUsernameIgnoreCase(staffIdentifier: String): CommunityOffenderManager?
}
