package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentPersonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentTimeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ContactNumberRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StatusUpdateRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateAdditionalConditionDataRequest
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

  @Test
  @Sql(
    "classpath:test_data/clear-all-licences.sql",
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
    "classpath:test_data/clear-all-licences.sql",
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
    "classpath:test_data/clear-all-licences.sql",
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
  @Sql("classpath:test_data/clear-all-licences.sql")
  fun `Create a licence`() {
    assertThat(licenceRepository.count()).isEqualTo(0)
    assertThat(standardConditionRepository.count()).isEqualTo(0)

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
  }

  @Test
  @Sql("classpath:test_data/clear-all-licences.sql")
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
  @Sql("classpath:test_data/clear-all-licences.sql")
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
    "classpath:test_data/clear-all-licences.sql",
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
    "classpath:test_data/clear-all-licences.sql",
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
    "classpath:test_data/clear-all-licences.sql",
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

    assertThat(result?.comTelephone).isEqualTo(aContactNumberRequest.comTelephone)
  }

  @Test
  @Sql(
    "classpath:test_data/clear-all-licences.sql",
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
    "classpath:test_data/clear-all-licences.sql",
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
    "classpath:test_data/clear-all-licences.sql",
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

    var result = webTestClient.get()
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
    "classpath:test_data/clear-all-licences.sql",
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

    var result = webTestClient.get()
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
    "classpath:test_data/clear-all-licences.sql",
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

    webTestClient.put()
      .uri("/licence/id/1/additional-conditions/condition/1")
      .bodyValue(anAdditionalConditionDataRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    var result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

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
    "classpath:test_data/clear-all-licences.sql",
    "classpath:test_data/seed-licence-summaries.sql"
  )
  fun `Get licence summaries by staffId`() {
    val result = webTestClient.get()
      .uri("/licence/staffId/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    log.info("Expect OK: Licence is ${mapper.writeValueAsString(result)}")

    assertThat(result?.size).isEqualTo(3)
    assertThat(result)
      .extracting<Tuple> { tuple(it.licenceId, it.licenceStatus) }
      .contains(tuple(1L, LicenceStatus.IN_PROGRESS), tuple(2L, LicenceStatus.APPROVED), tuple(3L, LicenceStatus.REJECTED))
  }

  @Test
  @Sql(
    "classpath:test_data/clear-all-licences.sql",
    "classpath:test_data/seed-licence-summaries.sql"
  )
  fun `Get licence summaries by staffId and status`() {
    val result = webTestClient.get()
      .uri("/licence/staffId/1?status=IN_PROGRESS&status=APPROVED")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    log.info("Expect OK: Licence is ${mapper.writeValueAsString(result)}")

    assertThat(result?.size).isEqualTo(2)
    assertThat(result)
      .extracting<Tuple> { tuple(it.licenceId, it.licenceStatus) }
      .contains(tuple(1L, LicenceStatus.IN_PROGRESS), tuple(2L, LicenceStatus.APPROVED))
  }

  @Test
  @Sql(
    "classpath:test_data/clear-all-licences.sql",
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

    val result = webTestClient.get()
      .uri("/licence/staffId/1?status=ACTIVE")
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
      comFirstName = "Stephen",
      comLastName = "Mills",
      comUsername = "X12345",
      comStaffId = 12345,
      comEmail = "stephen.mills@nps.gov.uk",
      comTelephone = "0116 2788777",
      probationAreaCode = "N01",
      probationLduCode = "LDU1",
      standardLicenceConditions = someStandardConditions,
      standardPssConditions = someStandardConditions,
    )

    val anUpdateAppointmentPersonRequest = AppointmentPersonRequest(
      appointmentPerson = "John Smith",
    )

    val anAppointmentTimeRequest = AppointmentTimeRequest(
      appointmentTime = LocalDateTime.now().plusDays(10),
    )

    val aContactNumberRequest = ContactNumberRequest(
      comTelephone = "0114 2565555",
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
      )
    )

    val aStatusUpdateRequest = StatusUpdateRequest(status = LicenceStatus.APPROVED, username = "X", fullName = "Y")
  }
}
