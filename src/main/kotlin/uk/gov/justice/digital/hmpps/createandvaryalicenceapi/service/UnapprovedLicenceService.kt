package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.UnapprovedLicence

@Service
class UnapprovedLicenceService(
  private val licenceRepository: LicenceRepository,
) {
  fun getEditedLicencesNotReApprovedByCrd(): List<UnapprovedLicence> {
    return licenceRepository.getEditedLicencesNotReApprovedByCrd()
  }
}
