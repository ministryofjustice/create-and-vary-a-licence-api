package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.timeserved

import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.timeserved.TimeServedProbationConfirmContact
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.timeserved.TimeServedProbationConfirmContactRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.timeserved.TimeServedProbationConfirmContactRepository
import java.time.LocalDateTime

@Service
class TimeServedProbationConfirmContactService(
  private val repo: TimeServedProbationConfirmContactRepository,
) {

  companion object {
    private val log = LoggerFactory.getLogger(TimeServedProbationConfirmContactService::class.java)
  }

  @Transactional
  fun addConfirmContact(licenceId: Long, request: TimeServedProbationConfirmContactRequest) {
    val username = SecurityContextHolder.getContext().authentication.name

    val existing = repo.findByLicenceId(licenceId)
    if (existing != null) {
      log.info("Updating existing TimeServedProbationConfirmContact for licenceId $licenceId")
      existing.contactStatus = request.contactStatus
      existing.communicationMethods = request.communicationMethods
      existing.otherDetail = request.otherCommunicationDetail
      existing.confirmedByUsername = username
      existing.dateLastUpdated = LocalDateTime.now()
    } else {
      log.info("Creating new TimeServedProbationConfirmContact for licenceId $licenceId")
      val newEntity = TimeServedProbationConfirmContact(
        licenceId = licenceId,
        contactStatus = request.contactStatus,
        communicationMethods = request.communicationMethods,
        otherDetail = request.otherCommunicationDetail,
        confirmedByUsername = username,
      )
      repo.saveAndFlush(newEntity)
    }
  }
}
