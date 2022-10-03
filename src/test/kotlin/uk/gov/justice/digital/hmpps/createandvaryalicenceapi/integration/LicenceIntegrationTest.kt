package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentPersonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentTimeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ContactNumberRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StatusUpdateRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateAdditionalConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.MatchLicencesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdatePrisonInformationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateReasonForVariationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSentenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSpoDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateVloDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
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
  lateinit var additionalConditionRepository: AdditionalConditionRepository

  @Autowired
  lateinit var auditEventRepository: AuditEventRepository

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql"
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
    "classpath:test_data/seed-licence-id-1.sql"
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

    assertThat(result?.userMessage).contains("Access is denied")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql"
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

    assertThat(result?.userMessage).contains("Access is denied")
    assertThat(licenceRepository.count()).isEqualTo(0)
    assertThat(standardConditionRepository.count()).isEqualTo(0)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql"
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
    "classpath:test_data/seed-licence-id-1.sql"
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
    "classpath:test_data/seed-licence-id-1.sql"
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
    "classpath:test_data/seed-licence-id-1.sql"
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
    "classpath:test_data/seed-licence-id-1.sql"
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
    "classpath:test_data/seed-licence-id-1.sql"
  )
  fun `Update the bespoke conditions`() {
    webTestClient.put()
      .uri("/licence/id/1/bespoke-conditions")
      .bodyValue(aBespokeConditionRequest)
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

    assertThat(result?.bespokeConditions)
      .extracting("text")
      .containsAll(listOf("Condition 1", "Condition 2", "Condition 3"))
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql"
  )
  fun `Update the list of additional conditions`() {
    webTestClient.put()
      .uri("/licence/id/1/additional-conditions")
      .bodyValue(anAdditionalConditionsRequest)
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

    assertThat(result?.additionalLicenceConditions)
      .extracting<Tuple> { tuple(it.code, it.category, it.text, it.sequence) }
      .containsAll(
        listOf(
          tuple("code1", "category", "text", 0),
          tuple("code2", "category", "text", 1),
          tuple("code3", "category", "text", 2),
          tuple("code4", "category", "text", 3)
        )
      )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql"
  )
  fun `Update the data associated with an additional condition`() {
    webTestClient.put()
      .uri("/licence/id/1/additional-conditions")
      .bodyValue(anAdditionalConditionsRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    // The condition ids will depend upon the order in which tests run so find these dynamically
    val conditions = additionalConditionRepository.findAll().toMutableList().filter { condition -> condition.licence.id == 1L }
    assertThat(conditions).isNotEmpty
    val conditionId = conditions.first().id

    webTestClient.put()
      .uri("/licence/id/1/additional-conditions/condition/$conditionId")
      .bodyValue(anAdditionalConditionDataRequest)
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

    assertThat(result?.additionalLicenceConditions?.get(0)?.expandedText).isEqualTo("expanded text")

    assertThat(result?.additionalLicenceConditions?.get(0)?.data)
      .extracting<Tuple> { tuple(it.field, it.value, it.sequence) }
      .containsAll(
        listOf(
          tuple("field1", "value1", 0),
          tuple("field2", "value2", 1),
          tuple("field3", "value3", 2),
        )
      )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-approved-licences.sql"
  )
  fun `Activate licences in bulk`() {
    webTestClient.post()
      .uri("/licence/activate-licences")
      .bodyValue(listOf(1, 2, 3))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.post()
      .uri("/licence/match")
      .bodyValue(MatchLicencesRequest(status = listOf(LicenceStatus.ACTIVE)))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(result?.size).isEqualTo(3)
    assertThat(result)
      .extracting<Tuple> {
        tuple(it.licenceId, it.licenceStatus)
      }
      .contains(
        tuple(1L, LicenceStatus.ACTIVE),
        tuple(2L, LicenceStatus.ACTIVE),
        tuple(3L, LicenceStatus.ACTIVE),
      )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql"
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
    "classpath:test_data/seed-licence-id-1.sql"
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
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql"
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
    "classpath:test_data/seed-licence-id-1.sql"
  )
  fun `Update spo discussion`() {
    webTestClient.put()
      .uri("/licence/id/1/spo-discussion")
      .bodyValue(UpdateSpoDiscussionRequest(spoDiscussion = "Yes"))
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

    assertThat(result?.spoDiscussion).isEqualTo("Yes")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql"
  )
  fun `Update vlo discussion`() {
    webTestClient.put()
      .uri("/licence/id/1/vlo-discussion")
      .bodyValue(UpdateVloDiscussionRequest(vloDiscussion = "Not applicable"))
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

    assertThat(result?.vloDiscussion).isEqualTo("Not applicable")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql"
  )
  fun `Update reason for variation`() {
    webTestClient.put()
      .uri("/licence/id/1/reason-for-variation")
      .bodyValue(UpdateReasonForVariationRequest(reasonForVariation = "reason"))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/events/match?licenceId=1&eventType=VARIATION_SUBMITTED_REASON")
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
    "classpath:test_data/seed-licence-id-1.sql"
  )
  fun `Update prison information`() {
    webTestClient.put()
      .uri("/licence/id/1/prison-information")
      .bodyValue(UpdatePrisonInformationRequest(prisonCode = "PVI", prisonDescription = "Pentonville (HMP)", prisonTelephone = "+44 276 54545"))
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
    "classpath:test_data/seed-licence-id-1.sql"
  )
  fun `Update sentence dates`() {
    prisonApiMockServer.stubGetHdcLatest()

    webTestClient.put()
      .uri("/licence/id/1/sentence-dates")
      .bodyValue(
        UpdateSentenceDatesRequest(
          conditionalReleaseDate = LocalDate.parse("2023-09-11"),
          actualReleaseDate = LocalDate.parse("2023-09-11"),
          sentenceStartDate = LocalDate.parse("2021-09-11"),
          sentenceEndDate = LocalDate.parse("2024-09-11"),
          licenceStartDate = LocalDate.parse("2023-09-11"),
          licenceExpiryDate = LocalDate.parse("2024-09-11"),
          topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
          topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
        )
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

    assertThat(result?.conditionalReleaseDate).isEqualTo(LocalDate.parse("2023-09-11"))
    assertThat(result?.actualReleaseDate).isEqualTo(LocalDate.parse("2023-09-11"))
    assertThat(result?.sentenceStartDate).isEqualTo(LocalDate.parse("2021-09-11"))
    assertThat(result?.sentenceEndDate).isEqualTo(LocalDate.parse("2024-09-11"))
    assertThat(result?.licenceStartDate).isEqualTo(LocalDate.parse("2023-09-11"))
    assertThat(result?.licenceExpiryDate).isEqualTo(LocalDate.parse("2024-09-11"))
    assertThat(result?.topupSupervisionStartDate).isEqualTo(LocalDate.parse("2024-09-11"))
    assertThat(result?.topupSupervisionExpiryDate).isEqualTo(LocalDate.parse("2025-09-11"))
  }

  private companion object {
    val prisonApiMockServer = PrisonApiMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      prisonApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      prisonApiMockServer.stop()
    }

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
      responsibleComStaffId = 2000
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

    val aBespokeConditionRequest = BespokeConditionRequest(
      conditions = listOf("Condition 1", "Condition 2", "Condition 3")
    )

    val anAdditionalConditionsRequest = AdditionalConditionsRequest(
      additionalConditions = listOf(
        AdditionalCondition(code = "code1", category = "category", sequence = 0, text = "text"),
        AdditionalCondition(code = "code2", category = "category", sequence = 1, text = "text"),
        AdditionalCondition(code = "code3", category = "category", sequence = 2, text = "text"),
        AdditionalCondition(code = "code4", category = "category", sequence = 3, text = "text")
      ),
      conditionType = "AP"
    )

    val anAdditionalConditionDataRequest = UpdateAdditionalConditionDataRequest(
      data = listOf(
        AdditionalConditionData(field = "field1", value = "value1", sequence = 0),
        AdditionalConditionData(field = "field2", value = "value2", sequence = 1),
        AdditionalConditionData(field = "field3", value = "value3", sequence = 2),
      ),
      expandedConditionText = "expanded text"
    )

    val aStatusUpdateRequest = StatusUpdateRequest(status = LicenceStatus.APPROVED, username = "X", fullName = "Y")
  }
}
