package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.PduHeadProperties
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService
import uk.gov.service.notify.NotificationClient

class NotifyIntegrationTest : IntegrationTestBase() {
  @Autowired
  private lateinit var pduHeadProperties: PduHeadProperties

  @MockBean
  private lateinit var notifyClient: NotificationClient

  @Test
  fun `check that PDU head contact info is injected from the spring context`() {
    val notifyService = NotifyService(
      enabled = true,
      licenceApprovedTemplateId = "licence-approved",
      variationForApprovalTemplateId = "variation-for-approval",
      initialLicencePromptTemplateId = "initial-prompt",
      urgentLicencePromptTemplateId = "urgent-prompt",
      client = notifyClient,
      pduHeadProperties = pduHeadProperties,
    )

    notifyService.sendVariationForApprovalEmail("N03SNT", "1", "Ryan", "Smith")

    verify(notifyClient).sendEmail(
      "variation-for-approval",
      "swansea@test.co.uk",
      mapOf(
        Pair("pduHeadFirstName", "Test"),
        Pair("licenceFirstName", "Ryan"),
        Pair("licenceLastName", "Smith"),
      ),
      null,
    )
  }
}
