package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.licence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.licence.LicenceLinkType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.licence.entity.LicenceLink

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
}
