package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest

@Service
class UpdateComService(
  private val staffService: StaffService,
) {

  fun updateComDetails(comDetails: UpdateComRequest): CommunityOffenderManager = try {
    staffService.updateComDetails(comDetails)
  } catch (e: DataIntegrityViolationException) {
    log.warn(
      "Duplicate key on COM insert for {} — retrying as update",
      "staffIdentifier='${comDetails.staffIdentifier}' staffCode='${comDetails.staffCode}' username='${comDetails.staffUsername}'",
      e,
    )
    staffService.updateComDetails(comDetails)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
