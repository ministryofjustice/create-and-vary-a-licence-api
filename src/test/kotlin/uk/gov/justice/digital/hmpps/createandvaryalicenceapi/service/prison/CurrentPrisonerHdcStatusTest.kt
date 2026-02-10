package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.currentPrisonerHdcStatus

class CurrentPrisonerHdcStatusTest {
  @Test
  fun `isHdcRelease returns true for any status that is not NOT_A_HDC_RELEASE`() {
    assertThat(currentPrisonerHdcStatus(bookingId = 1, currentHdcStatus = HdcStatus.NOT_STARTED).isHdcRelease()).isTrue()
    assertThat(currentPrisonerHdcStatus(bookingId = 2, currentHdcStatus = HdcStatus.ELIGIBILITY_CHECKS_COMPLETE).isHdcRelease()).isTrue()
    assertThat(currentPrisonerHdcStatus(bookingId = 3, currentHdcStatus = HdcStatus.RISK_CHECKS_COMPLETE).isHdcRelease()).isTrue()
    assertThat(currentPrisonerHdcStatus(bookingId = 4, currentHdcStatus = HdcStatus.APPROVED).isHdcRelease()).isTrue()
  }

  @Test
  fun `isHdcRelease returns false for NOT_A_HDC_RELEASE`() {
    assertThat(currentPrisonerHdcStatus(bookingId = 1, currentHdcStatus = HdcStatus.NOT_A_HDC_RELEASE).isHdcRelease()).isFalse()
  }
}
