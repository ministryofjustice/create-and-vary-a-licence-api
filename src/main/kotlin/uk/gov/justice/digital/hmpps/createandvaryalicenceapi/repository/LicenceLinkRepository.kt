package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceLink
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceLinkType

interface LicenceReplacementLink {
  val toLicenceId: Long
  val linkType: LicenceLinkType
}

@Repository
interface LicenceLinkRepository : JpaRepository<LicenceLink, Long> {

  @Query("SELECT DISTINCT l.linkType FROM LicenceLink l WHERE l.toLicenceId = :licenceId")
  fun findLinkTypesByToLicenceId(licenceId: Long): Set<LicenceLinkType>

  fun existsByToLicenceIdAndLinkType(
    toLicenceId: Long,
    linkType: LicenceLinkType,
  ): Boolean

  fun existsByFromLicenceIdAndLinkType(
    fromLicenceId: Long,
    linkType: LicenceLinkType,
  ): Boolean

  @Query(
    """
        SELECT l.toLicenceId AS toLicenceId, l.linkType AS linkType
            FROM LicenceLink l
            WHERE l.toLicenceId IN :licenceIds AND l.linkType IN :linkTypes
    """,
  )
  fun findLinksByToLicenceIdsAndLinkTypes(
    licenceIds: List<Long>,
    linkTypes: Set<LicenceLinkType>,
  ): List<LicenceReplacementLink>
}
