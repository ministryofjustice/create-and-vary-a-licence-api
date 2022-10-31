package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonerForRelease
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.NotifyRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.PromptLicenceCreationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.UnapprovedLicence
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class NotifyService(
  @Value("\${notify.enabled}") private val enabled: Boolean,
  @Value("\${self.link}") private val selfLink: String,
  @Value("\${notify.templates.licenceApproved}") private val licenceApprovedTemplateId: String,
  @Value("\${notify.templates.variationForApproval}") private val variationForApprovalTemplateId: String,
  @Value("\${notify.templates.variationReApproval}") private val variationForReApprovalTemplateId: String,
  @Value("\${notify.templates.initialLicencePrompt}") private val initialLicencePromptTemplateId: String,
  @Value("\${notify.templates.urgentLicencePrompt}") private val urgentLicencePromptTemplateId: String,
  @Value("\${notify.templates.datesChanged}") private val datesChangedTemplateId: String,
  @Value("\${notify.templates.variationApproved}") private val variationApprovedTemplateId: String,
  @Value("\${notify.templates.variationReferred}") private val variationReferredTemplateId: String,
  @Value("\${notify.templates.unapprovedLicence}") private val unapprovedLicenceByCrdTemplateId: String,
  @Value("\${notify.templates.datesChanged14B}") private val datesChanged14bTemplateId: String,
  private val client: NotificationClient,
) {
  fun sendLicenceApprovedEmail(emailAddress: String, values: Map<String, String>, reference: String) {
    // Hobbled this email - no current requirement to send this out - blanked out the email address to avoid sending
    // sendEmail(licenceApprovedTemplateId, emailAddress, values, reference)
    sendEmail(licenceApprovedTemplateId, "", values, reference)
  }

  fun sendVariationForApprovalEmail(notifyRequest: NotifyRequest, licenceId: String, firstName: String, lastName: String) {
    if (notifyRequest.email != null && notifyRequest.name != null) {
      val values: Map<String, String> = mapOf(
        Pair("pduHeadName", notifyRequest.name),
        Pair("licenceFirstName", firstName),
        Pair("licenceLastName", lastName),
        Pair("approvalCasesLink", selfLink.plus("/licence/vary-approve/list"))
      )
      sendEmail(variationForApprovalTemplateId, notifyRequest.email, values, null)
      log.info("Notification sent to ${notifyRequest.email} VARIATION FOR APPROVAL for $licenceId $firstName $lastName")
    } else {
      log.error("Notification failed (variationSubmitted) - email address not present for the PDU head for licence ID: $licenceId")
    }
  }

  fun sendVariationForReApprovalEmail(emailAddress: String?, firstName: String, lastName: String, prisonerNumber: String?, crd: LocalDate?) {
    if (emailAddress != null && crd != null) {
      val values: Map<String, String> = mapOf(
        Pair("prisonerFirstName", firstName),
        Pair("prisonerLastName", lastName),
        Pair("prisonerNumber", prisonerNumber ?: "unknown"),
        Pair("crd", crd.format(DateTimeFormatter.ofPattern("dd LLLL yyyy")))
      )
      sendEmail(variationForReApprovalTemplateId, emailAddress, values, null)
      log.info("Notification sent to OMU $emailAddress VARIATION FOR RE_APPROVAL for OMU PrisonerNumber $prisonerNumber")
    } else {
      log.error("Notification failed (variationReApproval) for PrisonerNumber $prisonerNumber - OMU email and CRD must be present")
    }
  }

  fun sendVariationApprovedEmail(
    creatorEmail: String,
    creatorName: String,
    comEmail: String,
    comName: String,
    popName: String,
    licenceId: String
  ) {
    val contacts = getContacts(creatorEmail, creatorName, comEmail, comName)
    contacts.forEach {
      val values: Map<String, String> = mapOf(
        Pair("comName", it.name),
        Pair("fullName", popName),
        Pair("caseListLink", selfLink.plus("/licence/vary/caseload"))
      )
      sendEmail(variationApprovedTemplateId, it.email, values, null)
      log.info("Notification sent to ${it.email} VARIATION APPROVED for $licenceId $popName")
    }
    if (contacts.isEmpty()) {
      log.error("Notification failed (variationApproved) - email addresses not present for licence ID: $licenceId")
    }
  }

  fun sendUnapprovedLicenceEmail(unapprovedLicenceEmailData: List<UnapprovedLicence>) {
    if (unapprovedLicenceEmailData.isEmpty()) {
      log.info("There were no emails to send to the probation practitioner to inform them of any edited emails that weren't re-approved by CRD date")
    }
    unapprovedLicenceEmailData.forEach {
      sendEmail(
        unapprovedLicenceByCrdTemplateId,
        "${it.comEmail}",
        mapOf(
          Pair("crn", "${it.crn}"),
          Pair("prisonerFirstName", "${it.forename}"),
          Pair("prisonerLastName", "${it.surname}"),
          Pair("comName", "${it.comFirstName} ${it.comLastName}"),
        ),
        null
      )
      log.info("Notification sent to ${it.comEmail} informing edited licence not reapproved by CRD for prisoner ${it.forename} ${it.surname}")
    }
  }

  fun sendVariationReferredEmail(
    creatorEmail: String,
    creatorName: String,
    comEmail: String,
    comName: String,
    popName: String,
    licenceId: String
  ) {
    val contacts = getContacts(creatorEmail, creatorName, comEmail, comName)
    contacts.forEach {
      val values: Map<String, String> = mapOf(
        Pair("comName", it.name),
        Pair("fullName", popName),
        Pair("caseListLink", selfLink.plus("/licence/vary/caseload"))
      )
      sendEmail(variationReferredTemplateId, it.email, values, null)
      log.info("Notification sent to ${it.email} VARIATION REFERRED for $licenceId $popName")
    }
    if (contacts.isEmpty()) {
      log.error("Notification failed (variationReferred) - email addresses not present for licence ID: $licenceId")
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
      log.error("Notification failed (datesChangedEmail) - email address not present for licence Id $licenceId")
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
  fun send14BDatesChangedEmail(
    emailAddress: String,
    prisonerFirstName: String,
    prisonerLastName: String,
    prisonNumber: String,
    dateChanges: String
  ) {
    sendEmail(
      datesChanged14bTemplateId,
      emailAddress,
      mapOf(
        Pair("prisonerFirstName", prisonerFirstName),
        Pair("prisonerLastName", prisonerLastName),
        Pair("prisonNumber", prisonNumber),
        Pair("dateChanges", dateChanges)
      ),
      null
    )
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
      log.info("Notification - blank email address: Did not send for template ID $templateId ref $reference")
      return
    }

    try {
      client.sendEmail(templateId, emailAddress, values, reference)
    } catch (e: NotificationClientException) {
      log.error("Notification failed - templateId $templateId to $emailAddress", e)
    }
  }

  private fun getContacts(creatorEmail: String, creatorName: String, comEmail: String, comName: String): List<Contact> {
    val contacts = mutableListOf<Contact>()
    val creatorIsBlank = creatorEmail.isEmpty()
    val comIsBlank = comEmail.isEmpty()
    if (creatorIsBlank && !comIsBlank) {
      contacts.add(Contact(comEmail, comName))
    } else if (comIsBlank && !creatorIsBlank) {
      contacts.add(Contact(creatorEmail, creatorName))
    } else if (creatorEmail == comEmail) {
      contacts.add(Contact(creatorEmail, creatorName))
    } else {
      contacts.add(Contact(creatorEmail, creatorName))
      contacts.add(Contact(comEmail, comName))
    }
    return contacts.toList()
  }

  companion object {
    private val log = LoggerFactory.getLogger(NotifyService::class.java)
  }
}

data class Contact(
  var email: String,
  var name: String
)
