package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.service.notify.NotificationClient

@Service
class NotifyService(
  @Value("\${notify.enabled}") private val enabled: Boolean,
  @Value("\${notify.templates.licence-approved-id}") private val licenceApprovedTemplateId: String,
  private val client: NotificationClient
) {
  fun sendLicenceApprovedEmail(emailAddress: String, values: Map<String, String>, reference: String) {
    sendEmail(licenceApprovedTemplateId, emailAddress, values, reference)
  }

  private fun sendEmail(templateId: String, emailAddress: String, values: Map<String, String>, reference: String) {
    if (!enabled) {
      log.info("Notification disabled: Did not send notification to ${emailAddress} for ${templateId} ref ${reference}")
      return
    }

    if (emailAddress.isNullOrEmpty()) {
      log.info("Blank email address: Did not send notification for ${templateId} ref ${reference}")
      return
    }

    try {
      client.sendEmail(templateId, emailAddress, values, reference)
    } catch (e: Exception) {
      log.error("Email notification failed", e)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(NotifyService::class.java)
  }
}