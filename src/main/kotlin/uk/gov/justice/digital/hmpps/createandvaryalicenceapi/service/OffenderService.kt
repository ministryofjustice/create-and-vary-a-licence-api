package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Service
class OffenderService(private val licenceRepository: LicenceRepository) {

  @Transactional
  fun updateOffenderWithResponsibleCom(crn: String, newCom: CommunityOffenderManager) {
    val inFlightLicenceStatuses = listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED, LicenceStatus.APPROVED, LicenceStatus.ACTIVE)

    var offenderLicences = this.licenceRepository.findAllByCrnAndStatusCodeIn(crn, inFlightLicenceStatuses)

    offenderLicences = offenderLicences.map { it.copy(responsibleCom = newCom) }

    // TODO: Create an audit log

    this.licenceRepository.saveAllAndFlush(offenderLicences)
  }
}
