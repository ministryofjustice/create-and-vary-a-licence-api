package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrrdLicenceResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.LicenceDomainEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.workingDays.WorkingDaysService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate
import kotlin.jvm.optionals.getOrNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateSentenceDatesIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Autowired
  lateinit var auditEventRepository: AuditEventRepository

  @Autowired
  lateinit var licenceEventRepository: LicenceEventRepository

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
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update sentence dates`() {
    prisonApiMockServer.stubGetHdcLatest()
    prisonApiMockServer.stubGetCourtOutcomes()
    mockPrisonerSearchResponse(
      SentenceDetail(
        conditionalReleaseDate = LocalDate.parse("2024-09-08"),
        confirmedReleaseDate = LocalDate.parse("2024-09-08"),
        sentenceStartDate = LocalDate.parse("2021-09-11"),
        sentenceExpiryDate = LocalDate.parse("2024-09-11"),
        licenceExpiryDate = LocalDate.parse("2024-09-11"),
        topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
        topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
      ),
    )

    webTestClient.put()
      .uri("/licence/id/1/sentence-dates")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.conditionalReleaseDate).isEqualTo(LocalDate.parse("2024-09-08"))
    assertThat(result?.actualReleaseDate).isEqualTo(LocalDate.parse("2024-09-08"))
    assertThat(result?.sentenceStartDate).isEqualTo(LocalDate.parse("2021-09-11"))
    assertThat(result?.sentenceEndDate).isEqualTo(LocalDate.parse("2024-09-11"))
    assertThat(result?.licenceStartDate).isEqualTo(LocalDate.parse("2024-09-08"))
    assertThat(result?.licenceExpiryDate).isEqualTo(LocalDate.parse("2024-09-11"))
    assertThat(result?.topupSupervisionStartDate).isEqualTo(LocalDate.parse("2024-09-11"))
    assertThat(result?.topupSupervisionExpiryDate).isEqualTo(LocalDate.parse("2025-09-11"))
  }

  @Test
  @Sql(
    "classpath:test_data/seed-hdc-licence-id-1.sql",
  )
  fun `Update sentence dates for HDC licence`() {
    prisonApiMockServer.stubGetHdcLatest()
    prisonApiMockServer.stubGetCourtOutcomes()
    mockPrisonerSearchResponse(
      SentenceDetail(
        conditionalReleaseDate = LocalDate.parse("2023-09-11"),
        confirmedReleaseDate = LocalDate.parse("2023-09-11"),
        sentenceStartDate = LocalDate.parse("2021-09-11"),
        sentenceExpiryDate = LocalDate.parse("2024-09-11"),
        licenceExpiryDate = LocalDate.parse("2024-09-11"),
        topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
        topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
        homeDetentionCurfewActualDate = LocalDate.parse("2024-08-01"),
        homeDetentionCurfewEndDate = LocalDate.parse("2023-08-10"),
      ),
    )

    webTestClient.put()
      .uri("/licence/id/1/sentence-dates")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(HdcLicence::class.java)
      .returnResult().responseBody

    assertThat(result?.conditionalReleaseDate).isEqualTo(LocalDate.parse("2023-09-11"))
    assertThat(result?.actualReleaseDate).isEqualTo(LocalDate.parse("2023-09-11"))
    assertThat(result?.sentenceStartDate).isEqualTo(LocalDate.parse("2021-09-11"))
    assertThat(result?.sentenceEndDate).isEqualTo(LocalDate.parse("2024-09-11"))
    assertThat(result?.licenceStartDate).isEqualTo(LocalDate.parse("2024-08-01"))
    assertThat(result?.licenceExpiryDate).isEqualTo(LocalDate.parse("2024-09-11"))
    assertThat(result?.topupSupervisionStartDate).isEqualTo(LocalDate.parse("2024-09-11"))
    assertThat(result?.topupSupervisionExpiryDate).isEqualTo(LocalDate.parse("2025-09-11"))
    assertThat(result?.homeDetentionCurfewActualDate).isEqualTo(LocalDate.parse("2024-08-01"))
    assertThat(result?.homeDetentionCurfewEndDate).isEqualTo(LocalDate.parse("2023-08-10"))
  }

  @Test
  @Sql(
    "classpath:test_data/seed-prrd-licence-id-1.sql",
  )
  fun `Update sentence dates for PRRD licence`() {
    prisonApiMockServer.stubGetHdcLatest()
    prisonApiMockServer.stubGetCourtOutcomes()

    val postRecallReleaseDate = LocalDate.parse("2025-02-24")

    mockPrisonerSearchResponse(
      SentenceDetail(
        conditionalReleaseDate = LocalDate.parse("2023-09-11"),
        confirmedReleaseDate = LocalDate.parse("2023-09-11"),
        sentenceStartDate = LocalDate.parse("2021-09-11"),
        sentenceExpiryDate = LocalDate.parse("2024-09-11"),
        licenceExpiryDate = LocalDate.parse("2024-09-11"),
        topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
        topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
        homeDetentionCurfewActualDate = LocalDate.parse("2024-08-01"),
        homeDetentionCurfewEndDate = LocalDate.parse("2023-08-10"),
        postRecallReleaseDate = postRecallReleaseDate,
      ),
    )

    webTestClient.put()
      .uri("/licence/id/1/sentence-dates")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrrdLicenceResponse::class.java)
      .returnResult().responseBody

    assertThat(result?.conditionalReleaseDate).isEqualTo(LocalDate.parse("2023-09-11"))
    assertThat(result?.actualReleaseDate).isEqualTo(LocalDate.parse("2023-09-11"))
    assertThat(result?.sentenceStartDate).isEqualTo(LocalDate.parse("2021-09-11"))
    assertThat(result?.sentenceEndDate).isEqualTo(LocalDate.parse("2024-09-11"))
    assertThat(result?.licenceStartDate).isEqualTo(postRecallReleaseDate)
    assertThat(result?.licenceExpiryDate).isEqualTo(LocalDate.parse("2024-09-11"))
    assertThat(result?.topupSupervisionStartDate).isEqualTo(LocalDate.parse("2024-09-11"))
    assertThat(result?.topupSupervisionExpiryDate).isEqualTo(LocalDate.parse("2025-09-11"))
    assertThat(result?.postRecallReleaseDate).isEqualTo(postRecallReleaseDate)
  }

  fun updateHardStopDateScenarios(): List<Arguments> {
    val workingDays = workingDaysService.workingDaysAfter(LocalDate.now())

    val tests = mutableListOf<Arguments>()

    val prrdDate = workingDays.elementAt(0)
    val lastWorkingDay = workingDaysService.getLastWorkingDay(prrdDate)

    tests.add(
      // PRRD calculation for LSD, confirmedReleaseDate is null, last working day from Prrd date expected outcome
      Arguments.of(null, null, prrdDate, lastWorkingDay),
    )

    val confirmedAfterPrrdDate = workingDays.elementAt(1)
    tests.add(
      // PRRD calculation for LSD, confirmedReleaseDate after prrdDate, last working day from Prrd date expected outcome
      Arguments.of(null, confirmedAfterPrrdDate, prrdDate, lastWorkingDay),
    )

    val confirmedBeforeCrdDate = workingDays.elementAt(2)
    val crdDate = workingDays.elementAt(3)
    tests.add(
      // Crd PRRD calculation for LSD, confirmedReleaseDate expected outcome
      Arguments.of(crdDate, confirmedBeforeCrdDate, prrdDate, confirmedBeforeCrdDate),
    )

    tests.add(
      // Crd PRRD calculation for LSD, crd date expected outcome
      Arguments.of(crdDate, null, null, crdDate),
    )

    return tests
  }

  @ParameterizedTest
  @MethodSource("updateHardStopDateScenarios")
  @Sql(
    "classpath:test_data/seed-prison-case-administrator.sql",
    "classpath:test_data/seed-hard-stop-licence-1.sql",
  )
  fun `Update sentence dates for Hard stop licence - licence start date calculation`(
    conditionalReleaseDate: LocalDate?,
    confirmedReleaseDate: LocalDate?,
    postRecallReleaseDate: LocalDate?,
    expectedLicenceStartDate: LocalDate,
  ) {
    // Given
    prisonApiMockServer.stubGetHdcLatest()
    prisonApiMockServer.stubGetCourtOutcomes()

    mockPrisonerSearchResponse(
      SentenceDetail(
        conditionalReleaseDate = conditionalReleaseDate,
        confirmedReleaseDate = confirmedReleaseDate,
        postRecallReleaseDate = postRecallReleaseDate,
      ),
    )

    // When
    val result = webTestClient.put()
      .uri("/licence/id/1/sentence-dates")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()

    // Then
    result.expectStatus().isOk

    val licenceOptional = licenceRepository.findById(1)
    assertThat(licenceOptional.isPresent).isTrue()
    val licence = licenceOptional.get()
    assertThat(licence.licenceStartDate).isEqualTo(expectedLicenceStartDate)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-3.sql",
  )
  fun `Update sentence dates should set license status to inactive when the offender has a new future release date`() {
    prisonApiMockServer.stubGetHdcLatest()
    prisonApiMockServer.stubGetCourtOutcomes()
    mockPrisonerSearchResponse(
      SentenceDetail(
        conditionalReleaseDate = LocalDate.now().plusDays(5),
        confirmedReleaseDate = LocalDate.now().plusDays(2),
        sentenceStartDate = LocalDate.parse("2021-09-11"),
        sentenceExpiryDate = LocalDate.parse("2024-09-11"),
        licenceExpiryDate = LocalDate.parse("2024-09-11"),
        topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
        topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
      ),
    )

    webTestClient.put()
      .uri("/licence/id/3/sentence-dates")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/3")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.statusCode).isEqualTo(LicenceStatus.INACTIVE)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-2.sql",
  )
  fun `Update sentence dates should set licence status to timed out when the licence is in hard stop period`() {
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

    webTestClient.put()
      .uri("/licence/id/2/sentence-dates")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val licence = licenceRepository.findById(2L).getOrNull()
    assertThat(licence).isNotNull
    assertThat(licence!!.statusCode).isEqualTo(LicenceStatus.TIMED_OUT)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-2.sql",
  )
  fun `Update sentence dates should inactivate licence where licence was in hard stop period but is no longer in hard stop period`() {
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

    webTestClient.put()
      .uri("/licence/id/2/sentence-dates")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val currentLicence = licenceRepository.findById(2L).getOrNull()
    assertThat(currentLicence).isNotNull

    argumentCaptor<HMPPSDomainEvent>().apply {
      verify(eventsPublisher, times(1)).publishDomainEvent(capture())
      assertThat(allValues).hasSize(1)
      assertThat(firstValue.eventType).isEqualTo(LicenceDomainEventType.LICENCE_INACTIVATED.value)
    }

    assertThat(auditEventRepository.count()).isEqualTo(3)
    assertThat(licenceEventRepository.count()).isEqualTo(2)
    assertThat(currentLicence?.statusCode).isEqualTo(LicenceStatus.INACTIVE)
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
