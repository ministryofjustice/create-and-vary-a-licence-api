package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class CaCaseloadServiceTest {
  private val caseloadService = mock<CaseloadService>()
  private val probationSearchApiClient = mock<ProbationSearchApiClient>()
  private val licenceService = mock<LicenceService>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val communityApiClient = mock<CommunityApiClient>()

  private val service = CaCaseloadService(
    caseloadService,
    clock,
    probationSearchApiClient,
    licenceService,
    prisonApiClient,
    communityApiClient,
  )

  @BeforeEach
  fun reset() {
    reset(caseloadService, clock, probationSearchApiClient, licenceService, prisonApiClient, communityApiClient)
  }

  private companion object {
    val clock: Clock = Clock.fixed(Instant.parse("2023-11-03T00:00:00Z"), ZoneId.systemDefault())
  }
}
