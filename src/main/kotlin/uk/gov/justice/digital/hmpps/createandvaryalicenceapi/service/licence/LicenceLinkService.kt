package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.licence

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.licence.entity.LicenceLink
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.licence.repository.LicenceLinkRepository

enum class LicenceLinkType {
  CRD_REPLACEMENT_FOR_OPTED_OUT_LICENCE,
}

@Service
class LicenceLinkingService(
  private val licenceLinkRepository: LicenceLinkRepository,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Transactional
  fun setCrdReplacementForOptedOutLicence(hdcLicenceId: Long, crdLicenceId: Long) {
    link(
      hdcLicenceId,
      LicenceLinkType.CRD_REPLACEMENT_FOR_OPTED_OUT_LICENCE,
      crdLicenceId,
    )
  }

  fun isCrdReplacementForOptedOutLicence(licenceId: Long): Boolean = hasLinkTo(
    licenceId,
    LicenceLinkType.CRD_REPLACEMENT_FOR_OPTED_OUT_LICENCE,
  )

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

  private fun hasLinkFrom(
    fromLicenceId: Long,
    linkType: LicenceLinkType,
  ): Boolean {
    log.debug("Checking if licence {} has from link for type {}", fromLicenceId, linkType)

    return licenceLinkRepository.existsByFromLicenceIdAndLinkType(
      fromLicenceId,
      linkType,
    )
  }
}
