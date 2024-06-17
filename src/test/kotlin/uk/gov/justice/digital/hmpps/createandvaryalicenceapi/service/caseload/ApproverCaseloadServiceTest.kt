package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonApproverService

class ApproverCaseloadServiceTest {
  private val prisonApproverService = mock<PrisonApproverService>()

  private val service = ApproverCaseloadService(prisonApproverService)

  @BeforeEach
  fun reset() {
    reset(prisonApproverService)
  }

  @Test
  fun `CADM prison caseload is filtered out`() {
    val aListOfPrisonCodes = listOf("ABC", "DEF", "CADM")
    service.getApprovalNeeded(aListOfPrisonCodes)
    verify(prisonApproverService, times(1)).getLicencesForApproval(listOf("ABC", "DEF"))
  }
}
