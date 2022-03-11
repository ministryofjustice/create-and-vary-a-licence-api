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
    val templateId = "xxx-xxx-xxx"
    val notifyService = NotifyService(enabled = true, licenceApprovedTemplateId = templateId, variationForApprovalTemplateId = templateId, client = notifyClient, pduHeadProperties)

    notifyService.sendVariationForApprovalEmail("N03SNT", mapOf(Pair("key", "value")), "Reference")

    verify(notifyClient).sendEmail(templateId, "swansea@test.co.uk", mapOf(Pair("key", "value")), "Reference")
  }
}
