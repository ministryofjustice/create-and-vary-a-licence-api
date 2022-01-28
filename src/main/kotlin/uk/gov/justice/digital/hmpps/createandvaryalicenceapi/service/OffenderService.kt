package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateResponsibleComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Service
class OffenderService(
  private val licenceRepository: LicenceRepository,
) {

  @Transactional
  fun updateOffenderWithResponsibleCom(crn: String, newComDetails: UpdateResponsibleComRequest) {
    val inFlightLicenceStatuses = listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED, LicenceStatus.APPROVED, LicenceStatus.ACTIVE)

    var offenderLicences = this.licenceRepository.findAllByCrnAndStatusCodeIn(crn, inFlightLicenceStatuses)

    offenderLicences = offenderLicences.map {
      it.copy(comStaffId = newComDetails.staffIdentifier, comUsername = newComDetails.staffUsername, comEmail = newComDetails.staffEmail)
    }

    // TODO: Create an audit log that offender managers were updated by the system
    this.licenceRepository.saveAllAndFlush(offenderLicences)
  }
}
