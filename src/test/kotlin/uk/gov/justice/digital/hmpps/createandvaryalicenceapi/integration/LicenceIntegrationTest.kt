package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentPersonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentTimeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ContactNumberRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StatusUpdateRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.RecentlyApprovedLicencesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdatePrisonInformationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateReasonForVariationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSpoDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateVloDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StandardConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class LicenceIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Autowired
  lateinit var standardConditionRepository: StandardConditionRepository

  @Autowired
  lateinit var auditEventRepository: AuditEventRepository

  @BeforeEach
  fun reset() {
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Get a licence by ID`() {
    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    log.info("Expect OK: Licence is ${mapper.writeValueAsString(result)}")

    assertThat(result?.standardLicenceConditions?.size).isEqualTo(2)
    assertThat(result?.standardLicenceConditions)
      .extracting("code")
      .containsAll(listOf("goodBehaviour", "notBreakLaw"))
    assertThat(result?.standardPssConditions?.size).isEqualTo(1)
    assertThat(result?.standardPssConditions)
      .extracting("code")
      .containsAll(listOf("attendMeetings"))
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Get forbidden (403) when incorrect roles are supplied`() {
    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_VERY_WRONG")))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(result?.userMessage).contains("Access Denied")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Unauthorized (401) when no token is supplied`() {
    webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED.value())
  }

  @Test
  fun `Create a licence`() {
    assertThat(licenceRepository.count()).isEqualTo(0)
    assertThat(standardConditionRepository.count()).isEqualTo(0)
    assertThat(auditEventRepository.count()).isEqualTo(0)

    val result = webTestClient.post()
      .uri("/licence/create")
      .bodyValue(aCreateLicenceRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicenceSummary::class.java)
      .returnResult().responseBody

    log.info("Expect OK: Result returned ${mapper.writeValueAsString(result)}")

    assertThat(result?.licenceId).isGreaterThan(0L)
    assertThat(result?.licenceType).isEqualTo(LicenceType.AP)
    assertThat(result?.licenceStatus).isEqualTo(LicenceStatus.IN_PROGRESS)

    assertThat(licenceRepository.count()).isEqualTo(1)
    assertThat(standardConditionRepository.count()).isEqualTo(6)
    assertThat(auditEventRepository.count()).isEqualTo(1)
  }

  @Test
  fun `Unauthorized (401) for create when no token is supplied`() {
    webTestClient.post()
      .uri("/licence/create")
      .bodyValue(aCreateLicenceRequest)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED.value())

    assertThat(licenceRepository.count()).isEqualTo(0)
    assertThat(standardConditionRepository.count()).isEqualTo(0)
  }

  @Test
  fun `Get forbidden (403) for create when incorrect roles are supplied`() {
    val result = webTestClient.post()
      .uri("/licence/create")
      .bodyValue(aCreateLicenceRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_VERY_WRONG")))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(result?.userMessage).contains("Access Denied")
    assertThat(licenceRepository.count()).isEqualTo(0)
    assertThat(standardConditionRepository.count()).isEqualTo(0)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update person to meet at initial appointment`() {
    webTestClient.put()
      .uri("/licence/id/1/appointmentPerson")
      .bodyValue(anUpdateAppointmentPersonRequest)
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

    assertThat(result?.appointmentPerson).isEqualTo(anUpdateAppointmentPersonRequest.appointmentPerson)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update time of the initial appointment`() {
    webTestClient.put()
      .uri("/licence/id/1/appointmentTime")
      .bodyValue(anAppointmentTimeRequest)
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

    assertThat(result?.appointmentTime)
      .isEqualTo(anAppointmentTimeRequest.appointmentTime.truncatedTo(ChronoUnit.MINUTES))
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update the contact number for the officer on a licence`() {
    webTestClient.put()
      .uri("/licence/id/1/contact-number")
      .bodyValue(aContactNumberRequest)
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

    assertThat(result?.appointmentContact).isEqualTo(aContactNumberRequest.telephone)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update the status of a licence to approved`() {
    webTestClient.put()
      .uri("/licence/id/1/status")
      .bodyValue(aStatusUpdateRequest)
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

    assertThat(result?.statusCode).isEqualTo(aStatusUpdateRequest.status)
    assertThat(result?.updatedByUsername).isEqualTo(aStatusUpdateRequest.username)
    assertThat(result?.approvedByUsername).isEqualTo(aStatusUpdateRequest.username)
    assertThat(result?.approvedByName).isEqualTo(aStatusUpdateRequest.fullName)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-versioned-licence-id-1.sql",
  )
  fun `Approve a new version of a licence`() {
    webTestClient.put()
      .uri("/licence/id/2/status")
      .bodyValue(aStatusUpdateRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val licenceV1 = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(licenceV1?.statusCode).isEqualTo(LicenceStatus.INACTIVE)
    assertThat(licenceV1?.updatedByUsername).isEqualTo(aStatusUpdateRequest.username)

    val licenceV2 = webTestClient.get()
      .uri("/licence/id/2")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(licenceV2?.statusCode).isEqualTo(LicenceStatus.APPROVED)
    assertThat(licenceV2?.approvedByUsername).isEqualTo(aStatusUpdateRequest.username)
    assertThat(licenceV2?.approvedByName).isEqualTo(aStatusUpdateRequest.fullName)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update the address where the initial appointment will take place`() {
    webTestClient.put()
      .uri("/licence/id/1/appointment-address")
      .bodyValue(anAppointmentAddressRequest)
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

    assertThat(result?.appointmentAddress).isEqualTo(anAppointmentAddressRequest.appointmentAddress)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Submit licence`() {
    webTestClient.put()
      .uri("/licence/id/1/submit")
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

    assertThat(result?.statusCode).isEqualTo(LicenceStatus.SUBMITTED)
    assertThat(result?.comUsername).isEqualTo("test-client")
    assertThat(result?.comEmail).isEqualTo("testClient@probation.gov.uk")
    assertThat(result?.comStaffId).isEqualTo(2000)
    assertThat(result?.updatedByUsername).isEqualTo("test-client")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Create licence variation`() {
    val result = webTestClient.post()
      .uri("/licence/id/1/create-variation")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(result?.licenceId).isGreaterThan(1)
    assertThat(result?.licenceType).isEqualTo(LicenceType.AP)
    assertThat(result?.licenceStatus).isEqualTo(LicenceStatus.VARIATION_IN_PROGRESS)
    assertThat(licenceRepository.count()).isEqualTo(2)

    val newLicence = webTestClient.get()
      .uri("/licence/id/${result?.licenceId}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(newLicence?.variationOf).isEqualTo(1)
    assertThat(newLicence?.licenceVersion).isEqualTo("2.0")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-approved-licence-1.sql",
  )
  fun `Edit an approved licence`() {
    val result = webTestClient.post()
      .uri("/licence/id/1/edit")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(result?.licenceId).isGreaterThan(1)
    assertThat(result?.licenceType).isEqualTo(LicenceType.AP)
    assertThat(result?.licenceStatus).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(licenceRepository.count()).isEqualTo(2)

    val newLicence = webTestClient.get()
      .uri("/licence/id/${result?.licenceId}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(newLicence?.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(newLicence?.licenceVersion).isEqualTo("1.1")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Discard licence`() {
    webTestClient.delete()
      .uri("/licence/id/1/discard")
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    assertThat(licenceRepository.count()).isEqualTo(0)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-variation-licence.sql",
  )
  fun `Update spo discussion`() {
    webTestClient.put()
      .uri("/licence/id/2/spo-discussion")
      .bodyValue(UpdateSpoDiscussionRequest(spoDiscussion = "Yes"))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/2")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.spoDiscussion).isEqualTo("Yes")
    assertThat(result?.id).isEqualTo(2)
    assertThat(result?.variationOf).isEqualTo(1)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-variation-licence.sql",
  )
  fun `Update vlo discussion`() {
    webTestClient.put()
      .uri("/licence/id/2/vlo-discussion")
      .bodyValue(UpdateVloDiscussionRequest(vloDiscussion = "Not applicable"))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/2")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.vloDiscussion).isEqualTo("Not applicable")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-variation-licence.sql",
  )
  fun `Update reason for variation`() {
    webTestClient.put()
      .uri("/licence/id/2/reason-for-variation")
      .bodyValue(UpdateReasonForVariationRequest(reasonForVariation = "reason"))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/events/match?licenceId=2&eventType=VARIATION_SUBMITTED_REASON")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(LicenceEvent::class.java)
      .returnResult().responseBody

    assertThat(result).isNotNull
    assertThat(result).hasSize(1)
    assertThat(result!!.get(0)?.eventDescription).isEqualTo("reason")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update prison information`() {
    webTestClient.put()
      .uri("/licence/id/1/prison-information")
      .bodyValue(
        UpdatePrisonInformationRequest(
          prisonCode = "PVI",
          prisonDescription = "Pentonville (HMP)",
          prisonTelephone = "+44 276 54545",
        ),
      )
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

    assertThat(result?.prisonCode).isEqualTo("PVI")
    assertThat(result?.prisonDescription).isEqualTo("Pentonville (HMP)")
    assertThat(result?.prisonTelephone).isEqualTo("+44 276 54545")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-recently-approved-licences.sql",
  )
  fun `find recently approved licences`() {
    val result = webTestClient.post()
      .uri("/licence/recently-approved")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        RecentlyApprovedLicencesRequest(
          prisonCodes = listOf("MDI", "BMI"),
        ),
      )
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(result?.size).isEqualTo(4)
    assertThat(result)
      .extracting<Tuple> {
        tuple(it.licenceId, it.licenceStatus, it.nomisId, it.surname, it.forename, it.prisonCode, it.prisonDescription)
      }
      .contains(
        tuple(2L, LicenceStatus.APPROVED, "B1234BB", "Bobson", "Bob", "MDI", "Moorland HMP"),
        tuple(4L, LicenceStatus.ACTIVE, "C1234DD", "Harcourt", "Kate", "BMI", "Birmingham HMP"),
        tuple(6L, LicenceStatus.INACTIVE, "C1234FF", "Biggs", "Harold", "BMI", "Birmingham HMP"),
        tuple(10L, LicenceStatus.APPROVED, "F2504MG", "Smith", "Jim", "MDI", "Moorland HMP"),
      )
  }

  private companion object {
    val someStandardConditions = listOf(
      StandardCondition(code = "goodBehaviour", sequence = 1, text = "Be of good behaviour"),
      StandardCondition(code = "notBreakLaw", sequence = 2, text = "Do not break any law"),
      StandardCondition(code = "attendMeetings", sequence = 3, text = "Attend meetings"),
    )

    val aCreateLicenceRequest = CreateLicenceRequest(
      typeCode = LicenceType.AP,
      version = "1.4",
      nomsId = "NOMSID",
      bookingNo = "BOOKNO",
      bookingId = 1L,
      crn = "CRN1",
      pnc = "PNC1",
      cro = "CRO1",
      prisonCode = "MDI",
      prisonDescription = "Moorland (HMP)",
      forename = "Mike",
      surname = "Myers",
      dateOfBirth = LocalDate.of(2001, 10, 1),
      conditionalReleaseDate = LocalDate.of(2021, 10, 22),
      actualReleaseDate = LocalDate.of(2021, 10, 22),
      sentenceStartDate = LocalDate.of(2018, 10, 22),
      sentenceEndDate = LocalDate.of(2021, 10, 22),
      licenceStartDate = LocalDate.of(2021, 10, 22),
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
      topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
      probationAreaCode = "N01",
      probationAreaDescription = "Wales",
      probationPduCode = "N01A",
      probationPduDescription = "Cardiff",
      probationLauCode = "N01A2",
      probationLauDescription = "Cardiff South",
      probationTeamCode = "NA01A2-A",
      probationTeamDescription = "Cardiff South Team A",
      standardLicenceConditions = someStandardConditions,
      standardPssConditions = someStandardConditions,
      responsibleComStaffId = 2000,
    )

    val anUpdateAppointmentPersonRequest = AppointmentPersonRequest(
      appointmentPerson = "John Smith",
    )

    val anAppointmentTimeRequest = AppointmentTimeRequest(
      appointmentTime = LocalDateTime.now().plusDays(10),
    )

    val aContactNumberRequest = ContactNumberRequest(
      telephone = "0114 2565555",
    )

    val anAppointmentAddressRequest = AppointmentAddressRequest(
      appointmentAddress = "221B Baker Street, London, City of London, NW1 6XE",
    )

    val aStatusUpdateRequest = StatusUpdateRequest(status = LicenceStatus.APPROVED, username = "X", fullName = "Y")

    val govUkApiMockServer = GovUkMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      govUkApiMockServer.stop()
    }
  }
}
