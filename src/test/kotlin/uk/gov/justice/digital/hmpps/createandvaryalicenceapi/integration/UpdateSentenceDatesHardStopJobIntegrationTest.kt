package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PotentialHardstopCaseStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.PotentialHardstopCaseRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.workingDays.WorkingDaysService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate
import kotlin.jvm.optionals.getOrNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = ["hardstop.deactivation.job.enabled=true"])
class UpdateSentenceDatesHardStopJobIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var auditEventRepository: AuditEventRepository

  @Autowired
  lateinit var licenceEventRepository: LicenceEventRepository

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Autowired
  lateinit var potentialHardstopCaseRepository: PotentialHardstopCaseRepository

  @MockitoBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  @Autowired
  lateinit var workingDaysService: WorkingDaysService

  @BeforeEach
  fun setup() {
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-2.sql",
  )
  fun `Update sentence dates should add an entry to potential hard stop cases when licence was in hard stop period but is no longer`() {
    prisonApiMockServer.stubGetHdcLatest()
    prisonApiMockServer.stubGetCourtOutcomes()
    mockPrisonerSearchResponse(
      SentenceDetail(
        conditionalReleaseDate = LocalDate.now(),
        confirmedReleaseDate = LocalDate.now(),
        sentenceStartDate = LocalDate.now().minusYears(2),
        sentenceExpiryDate = LocalDate.now().plusYears(1),
        licenceExpiryDate = LocalDate.now().plusYears(1),
        topupSupervisionStartDate = LocalDate.now().plusYears(1),
        topupSupervisionExpiryDate = LocalDate.now().plusYears(2),
      ),
    )

    // Put the licence into hard stop
    val result = webTestClient.put()
      .uri("/licence/id/2/sentence-dates")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()

    result.expectStatus().isOk
    assertThat(auditEventRepository.count()).isEqualTo(1)
    assertThat(licenceEventRepository.count()).isEqualTo(1)

    val previousLicence = licenceRepository.findById(2L).getOrNull()
    assertThat(previousLicence?.statusCode).isEqualTo(LicenceStatus.TIMED_OUT)
    mockPrisonerSearchResponse(
      SentenceDetail(
        conditionalReleaseDate = LocalDate.now().plusWeeks(1),
        confirmedReleaseDate = LocalDate.now().plusWeeks(1),
        sentenceStartDate = LocalDate.now().minusYears(2),
        sentenceExpiryDate = LocalDate.now().plusYears(1),
        licenceExpiryDate = LocalDate.now().plusYears(1),
        topupSupervisionStartDate = LocalDate.now().plusYears(1),
        topupSupervisionExpiryDate = LocalDate.now().plusYears(2),
      ),
    )

    // move out of hard stop
    webTestClient.put()
      .uri("/licence/id/2/sentence-dates")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val currentLicence = licenceRepository.findById(2L).getOrNull()
    assertThat(currentLicence).isNotNull

    assertThat(auditEventRepository.count()).isEqualTo(2)
    assertThat(licenceEventRepository.count()).isEqualTo(1)
    assertThat(currentLicence?.statusCode).isEqualTo(LicenceStatus.TIMED_OUT)

    assertThat(potentialHardstopCaseRepository.count()).isEqualTo(1)
    val potentialHardstopCase = potentialHardstopCaseRepository.findById(1L).getOrNull()
    assertThat(potentialHardstopCase).isNotNull()
    assertThat(potentialHardstopCase?.status).isEqualTo(PotentialHardstopCaseStatus.PENDING)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-timedout-and-inprogress-hardstop-licences.sql",
  )
  fun `If we move a timed out licence and in in progress hard stop licence out of hard stop we should only create one hard stop case for each licence`() {
    prisonApiMockServer.stubGetHdcLatest()
    prisonApiMockServer.stubGetCourtOutcomes()

    mockPrisonerSearchResponse(
      SentenceDetail(
        conditionalReleaseDate = LocalDate.now().plusWeeks(1),
        confirmedReleaseDate = LocalDate.now().plusWeeks(1),
        sentenceStartDate = LocalDate.now().minusYears(2),
        sentenceExpiryDate = LocalDate.now().plusYears(1),
        licenceExpiryDate = LocalDate.now().plusYears(1),
        topupSupervisionStartDate = LocalDate.now().plusYears(1),
        topupSupervisionExpiryDate = LocalDate.now().plusYears(2),
      ),
    )

    // move the timed out licence out of the hard stop period
    webTestClient.put()
      .uri("/licence/id/2/sentence-dates")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    // move the in-progress licence out of the hard stop period
    webTestClient.put()
      .uri("/licence/id/3/sentence-dates")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    assertThat(auditEventRepository.count()).isEqualTo(2)
    assertThat(licenceEventRepository.count()).isEqualTo(0)
    val timedOutLicence = licenceRepository.findById(2L).getOrNull()
    assertThat(timedOutLicence?.statusCode).isEqualTo(LicenceStatus.TIMED_OUT)

    val inProgressLicence = licenceRepository.findById(3L).getOrNull()
    assertThat(inProgressLicence?.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)

    assertThat(potentialHardstopCaseRepository.count()).isEqualTo(2)
    assertThat(potentialHardstopCaseRepository.findAll())
      .extracting("status")
      .contains(PotentialHardstopCaseStatus.PENDING, PotentialHardstopCaseStatus.PENDING)
  }

  private fun mockPrisonerSearchResponse(sentenceDetail: SentenceDetail) {
    prisonApiMockServer.stubGetPrisonerDetail("A1234AA", sentenceDetail)
  }

  private companion object {
    val prisonApiMockServer = PrisonApiMockServer()
    val govUkApiMockServer = GovUkMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      prisonApiMockServer.start()
      prisonApiMockServer.stubGetSentenceAndRecallTypes(123456)
      govUkApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      prisonApiMockServer.stop()
      govUkApiMockServer.stop()
    }
  }
}
