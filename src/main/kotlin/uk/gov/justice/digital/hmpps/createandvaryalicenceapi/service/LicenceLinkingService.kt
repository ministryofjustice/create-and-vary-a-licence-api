package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceLink
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceLinkType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceLinkType.CRD_REPLACEMENT_FOR_OPTED_OUT_LICENCE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceLinkType.CRD_REPLACEMENT_FOR_REFUSED_LICENCE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceLinkType.HDC_REPLACEMENT_FOR_OPTED_IN_LICENCE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceLinkRepository

@Service
class LicenceLinkingService(
  private val licenceLinkRepository: LicenceLinkRepository,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Transactional
  fun setCrdReplacementForOptedOutLicence(hdcLicenceId: Long, crdLicenceId: Long) {
    link(
      hdcLicenceId,
      CRD_REPLACEMENT_FOR_OPTED_OUT_LICENCE,
      crdLicenceId,
    )
  }

  @Transactional
  fun setHdcReplacementForOptedInLicence(crdLicenceId: Long, hdcLicenceId: Long) {
    link(
      crdLicenceId,
      HDC_REPLACEMENT_FOR_OPTED_IN_LICENCE,
      hdcLicenceId,
    )
  }

  @Transactional
  fun setCrdReplacementForRefusedLicence(hdcLicenceId: Long, crdLicenceId: Long) {
    link(
      hdcLicenceId,
      CRD_REPLACEMENT_FOR_REFUSED_LICENCE,
      crdLicenceId,
    )
  }

  fun getLinkTypesToLicence(licenceId: Long): Set<LicenceLinkType> = licenceLinkRepository.findLinkTypesByToLicenceId(licenceId)

  private fun hasLinkTo(
    toLicenceId: Long,
    linkType: LicenceLinkType,
  ): Boolean {
    log.debug("Checking if licence {} has to link for type {}", toLicenceId, linkType)

    return licenceLinkRepository.existsByToLicenceIdAndLinkType(
      toLicenceId,
      linkType,
    )
  }

  private fun link(
    fromLicenceId: Long,
    linkType: LicenceLinkType,
    toLicenceId: Long,
  ) {
    log.debug("Linking licence {} to licence {} for type {}", fromLicenceId, toLicenceId, linkType)
    licenceLinkRepository.save(
      LicenceLink(
        fromLicenceId = fromLicenceId,
        toLicenceId = toLicenceId,
        linkType = linkType,
      ),
    )
  }

  fun getCrdReplacementLinkTypes(licenceIds: List<Long>): Map<Long, Set<LicenceLinkType>> {
    if (licenceIds.isEmpty()) return emptyMap()

    return licenceLinkRepository.findLinksByToLicenceIdsAndLinkTypes(
      licenceIds,
      setOf(CRD_REPLACEMENT_FOR_OPTED_OUT_LICENCE, CRD_REPLACEMENT_FOR_REFUSED_LICENCE),
    )
      .groupBy { it.toLicenceId }
      .mapValues { (_, links) -> links.mapTo(mutableSetOf()) { it.linkType } }
  }
}
