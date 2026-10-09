package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceLink
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceLinkType

@Repository
interface LicenceLinkRepository : JpaRepository<LicenceLink, Long> {

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
        SELECT DISTINCT l.toLicenceId  FROM LicenceLink l
            WHERE l.toLicenceId IN :licenceIds AND l.linkType = :linkType
    """,
  )
  fun findToLicenceIdsByLicenceIdsAndLinkType(
    licenceIds: List<Long>,
    linkType: LicenceLinkType,
  ): Set<Long>
}
