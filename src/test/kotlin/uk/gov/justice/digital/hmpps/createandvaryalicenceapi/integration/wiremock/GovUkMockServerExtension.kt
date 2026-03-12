package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class GovUkMockServerExtension :
  BeforeAllCallback,
  AfterAllCallback {
  private val govUkApiMockServer = GovUkMockServer()

  override fun beforeAll(context: ExtensionContext?) {
    govUkApiMockServer.start()
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
  }

  override fun afterAll(context: ExtensionContext?) {
    govUkApiMockServer.stop()
  }
}
