package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.EmailConfig
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.PduHeadProperties
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException

@Service
class NotifyService(
  @Value("\${notify.enabled}") private val enabled: Boolean,
  @Value("\${notify.templates.licenceApproved}") private val licenceApprovedTemplateId: String,
  @Value("\${notify.templates.variationForApproval}") private val variationForApprovalTemplateId: String,
  private val client: NotificationClient,
  private val pduHeadProperties: PduHeadProperties,
) {
  fun sendLicenceApprovedEmail(emailAddress: String, values: Map<String, String>, reference: String) {
    sendEmail(licenceApprovedTemplateId, emailAddress, values, reference)
  }

  fun sendVariationForApprovalEmail(pduCode: String, values: Map<String, String>, reference: String) {
    var pduHead: EmailConfig?
    try {
      pduHead = pduHeadProperties.contacts.getValue(pduCode)
      if (pduHead.email.isNotBlank()) {
        sendEmail(variationForApprovalTemplateId, pduHead.email, values, reference)
      } else {
        log.error("sendVariationForApproval: An email address was not configured for the head of PDU $pduCode")
      }
    } catch(e: NoSuchElementException) {
      log.error("sendVariationForApproval: No PDU head contact detail configured for $pduCode - No email was sent")
    }
  }

  private fun sendEmail(templateId: String, emailAddress: String, values: Map<String, String>, reference: String) {
    if (!enabled) {
      log.info("Notification disabled: Did not send notification to $emailAddress for $templateId ref $reference")
      return
    }

    if (emailAddress.isBlank()) {
      log.info("Blank email address: Did not send notification for $templateId ref $reference")
      return
    }

    try {
      client.sendEmail(templateId, emailAddress, values, reference)
    } catch (e: NotificationClientException) {
      log.error("Email notification failed - templateId $templateId to $emailAddress", e)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(NotifyService::class.java)
  }
}
