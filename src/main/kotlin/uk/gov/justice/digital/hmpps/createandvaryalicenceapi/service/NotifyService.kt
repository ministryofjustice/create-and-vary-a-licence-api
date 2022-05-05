package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.EmailConfig
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.PduHeadProperties
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonerForRelease
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.PromptLicenceCreationRequest
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException
import java.time.format.DateTimeFormatter

@Service
class NotifyService(
  @Value("\${notify.enabled}") private val enabled: Boolean,
  @Value("\${self.link}") private val selfLink: String,
  @Value("\${notify.templates.licenceApproved}") private val licenceApprovedTemplateId: String,
  @Value("\${notify.templates.variationForApproval}") private val variationForApprovalTemplateId: String,
  @Value("\${notify.templates.initialLicencePrompt}") private val initialLicencePromptTemplateId: String,
  @Value("\${notify.templates.urgentLicencePrompt}") private val urgentLicencePromptTemplateId: String,
  @Value("\${notify.templates.datesChanged}") private val datesChangedTemplateId: String,
  private val client: NotificationClient,
  private val pduHeadProperties: PduHeadProperties,
) {
  fun sendLicenceApprovedEmail(emailAddress: String, values: Map<String, String>, reference: String) {
    // Hobbled this email - no current requirement to send this out - blanked out the email address to avoid sending
    // sendEmail(licenceApprovedTemplateId, emailAddress, values, reference)
    sendEmail(licenceApprovedTemplateId, "", values, reference)
  }

  fun sendVariationForApprovalEmail(pduCode: String, licenceId: String, firstName: String, lastName: String) {
    val pduHead = pduHeadProperties.contacts
      .getOrDefault(pduCode, EmailConfig(forename = "", surname = "", email = "", description = ""))

    if (pduHead.email.isNotBlank()) {
      val values: Map<String, String> = mapOf(
        Pair("pduHeadName", pduHead.forename),
        Pair("licenceFirstName", firstName),
        Pair("licenceLastName", lastName),
        Pair("approvalCasesLink", selfLink.plus("/licence/vary-approve/list"))
      )
      sendEmail(variationForApprovalTemplateId, pduHead.email, values, null)
      log.info("Notification sent to ${pduHead.email} VARIATION FOR APPROVAL for $licenceId $firstName $lastName")
    } else {
      log.error("sendVariationForApproval: A contact was not configured for the head of PDU $pduCode")
    }
  }

  fun sendDatesChangedEmail(
    licenceId: String,
    emailAddress: String?,
    comFullName: String,
    offenderFullName: String,
    crn: String?,
    datesChanged: Map<String, Boolean>
  ) {
    if (emailAddress?.isNotBlank() == true) {
      // For all the dates with the change flag set to true get their descriptions as a list
      val listOfDateTypes = mutableListOf<String>()
      datesChanged.asSequence().filter { it.value }.forEach { listOfDateTypes.add(it.key) }

      val values: Map<String, Any> = mapOf(
        Pair("comFullName", comFullName),
        Pair("offenderFullName", offenderFullName),
        Pair("crn", crn!!),
        Pair("dateDescriptions", listOfDateTypes),
        Pair("caseloadLink", selfLink.plus("/licence/create/caseload"))
      )

      sendEmail(datesChangedTemplateId, emailAddress, values, null)
      log.info("Notification sent to $emailAddress DATES CHANGED for $licenceId $offenderFullName")
    } else {
      log.error("sendDatesChangedEmail: The COM email address was not present to inform of a dates change for licence Id $licenceId")
    }
  }

  fun sendInitialLicenceCreateEmails(comsToEmail: List<PromptLicenceCreationRequest>) {
    comsToEmail.forEach {
      if (it.initialPromptCases.isNotEmpty()) {
        sendLicenceCreateEmail(initialLicencePromptTemplateId, it.email, it.comName, it.initialPromptCases)
      }
      if (it.urgentPromptCases.isNotEmpty()) {
        sendLicenceCreateEmail(urgentLicencePromptTemplateId, it.email, it.comName, it.urgentPromptCases)
      }
    }
  }

  private fun sendLicenceCreateEmail(templateId: String, emailAddress: String, comName: String, cases: List<PrisonerForRelease>) {
    sendEmail(
      templateId,
      emailAddress,
      mapOf(
        Pair("comName", comName),
        Pair(
          "prisonersForRelease",
          cases.map { prisoner ->
            "${prisoner.name} who will leave custody on ${prisoner.releaseDate.format(DateTimeFormatter.ofPattern("dd LLLL yyyy"))}"
          }
        ),
        Pair("createLicenceLink", selfLink.plus("/licence/create/caseload"))
      ),
      null
    )
    cases.map { prisoner ->
      var promptType = "REMINDER"
      if (templateId == initialLicencePromptTemplateId) {
        promptType = "INITIAL PROMPT"
      }
      log.info("Notification sent to $emailAddress $promptType for ${prisoner.name} being release on ${prisoner.releaseDate.format(DateTimeFormatter.ofPattern("dd LLLL yyyy"))}")
    }
  }

  private fun sendEmail(templateId: String, emailAddress: String, values: Map<String, Any>, reference: String?) {
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
