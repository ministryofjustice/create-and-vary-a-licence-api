package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.DeliusMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateAppointmentAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateAppointmentDetails
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateCurfewDetails
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateCurfewTime
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateFirstNight
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateFromHdcToCvlRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateLicenceDetails
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateLicenceLifecycleDetails
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigratePrisonDetails
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigratePrisonerDetails
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateSentenceDetails
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private const val MIGRATE_URL = "/licences/migrate/active"

class MigrationControllerIntegrationTest : IntegrationTestBase() {

  @Test
  @Sql(
    "classpath:test_data/seed-staff-for-migration.sql",
  )
  fun `should migrate licence successfully`() {
    // Given
    deliusMockServer.stubGetProbationCase()
    deliusMockServer.stubGetOffenderManagerWithNomsId("A1234BC")
    deliusMockServer.stubGetUserByUserName(2L, userName = "submittedByUserName", firstName = "submittedByFirstName", lastName = "submittedByLastName")
    deliusMockServer.stubGetUserByUserName(3L, userName = "createdByUserName", firstName = "createdByFirstName", lastName = "createdByLastName")
    deliusMockServer.stubGetUserByUserName(4L, userName = "approvedByUsername", firstName = "approvedByFirstName", lastName = "approvedByLastName")

    val request = validRequest()

    // When
    val result = webTestClient.post()
      .uri(MIGRATE_URL)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .bodyValue(request)
      .exchange()

    // Then
    result.expectStatus().isOk

    val licence = testRepository.findLicence(1)
    assertHdcLicenceMatches(request, licence)
    assertThat(testRepository.hasMetaData()).isTrue
  }

  @Test
  fun `should not migrate or save meta data when error is thrown`() {
    // Given
    val request = validRequest()

    // When
    val result = webTestClient.post()
      .uri(MIGRATE_URL)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .bodyValue(request)
      .exchange()

    // Then
    result.expectStatus().isBadRequest

    assertThat(testRepository.doesLicenceExist(1)).isFalse
    assertThat(testRepository.hasMetaData()).isFalse
  }

  @Test
  fun `should return unauthorized if no token`() {
    // Given
    val request = validRequest()

    // When
    val result = webTestClient.post()
      .uri(MIGRATE_URL)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()

    // Then
    result.expectStatus().isUnauthorized
    assertThat(testRepository.hasMetaData()).isFalse
  }

  @Test
  fun `should return forbidden if no role`() {
    // Given
    val request = validRequest()

    // When
    val result = webTestClient.post()
      .uri(MIGRATE_URL)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation())
      .bodyValue(request)
      .exchange()

    // Then
    result.expectStatus().isForbidden
    assertThat(testRepository.hasMetaData()).isFalse
  }

  @Test
  fun `should return forbidden if wrong role`() {
    // Given
    val request = validRequest()

    // When
    val result = webTestClient.post()
      .uri(MIGRATE_URL)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
      .bodyValue(request)
      .exchange()

    // Then
    result.expectStatus().isForbidden
    assertThat(testRepository.hasMetaData()).isFalse
  }

  fun assertHdcLicenceMatches(
    request: MigrateFromHdcToCvlRequest,
    licence: Licence,
  ) {
    assertThat(licence.typeCode).isEqualTo(request.licence.typeCode)
    assertThat(licence.licenceExpiryDate).isEqualTo(request.licence.licenceExpiryDate)
    assertThat(licence.licenceActivatedDate?.toLocalDate()).isEqualTo(request.licence.licenceActivationDate)
    assertThat(licence.licenceExpiryDate).isEqualTo(request.licence.licenceExpiryDate)
    assertThat(licence.licenceVersion).isEqualTo("1.0")
    assertThat(licence.version).isEqualTo("3.0")

    assertThat(licence.statusCode).isEqualTo(LicenceStatus.ACTIVE)

    assertThat(licence.bookingNo).isEqualTo(request.bookingNo)
    assertThat(licence.bookingId).isEqualTo(request.bookingId)

    assertThat(licence.pnc).isEqualTo(request.pnc)
    assertThat(licence.cro).isEqualTo(request.cro)
    assertThat(licence.crn).isEqualTo("X12345")

    assertThat(licence.nomsId).isEqualTo(request.prisoner.prisonerNumber)
    assertThat(licence.forename).isEqualTo(request.prisoner.forename)
    assertThat(licence.middleNames).isEqualTo(request.prisoner.middleNames)
    assertThat(licence.surname).isEqualTo(request.prisoner.surname)
    assertThat(licence.dateOfBirth).isEqualTo(request.prisoner.dateOfBirth)

    assertThat(licence.prisonCode).isEqualTo(request.prison.prisonCode)
    assertThat(licence.prisonDescription).isEqualTo(request.prison.prisonDescription)
    assertThat(licence.prisonTelephone).isEqualTo(request.prison.prisonTelephone)

    assertThat(licence.sentenceStartDate).isEqualTo(request.sentence.sentenceStartDate)
    assertThat(licence.sentenceEndDate).isEqualTo(request.sentence.sentenceEndDate)
    assertThat(licence.conditionalReleaseDate).isEqualTo(request.sentence.conditionalReleaseDate)
    assertThat(licence.actualReleaseDate).isEqualTo(request.sentence.actualReleaseDate)
    assertThat(licence.topupSupervisionStartDate).isEqualTo(request.sentence.topupSupervisionStartDate)
    assertThat(licence.topupSupervisionExpiryDate).isEqualTo(request.sentence.topupSupervisionExpiryDate)
    assertThat(licence.postRecallReleaseDate).isEqualTo(request.sentence.postRecallReleaseDate)

    assertThat(licence.approvedDate).isEqualTo(request.lifecycle.approvedDate)
    assertThat(licence.approvedByUsername).isEqualTo(request.lifecycle.approvedByUsername)
    assertThat(licence.approvedByName).isEqualTo("Approvedbyfirstname Approvedbylastname")

    assertThat(licence.submittedDate).isEqualTo(request.lifecycle.submittedDate)
    assertThat(licence.dateCreated).isEqualTo(request.lifecycle.dateCreated)

    assertThat(licence.licenceStartDate).isEqualTo(request.licence.homeDetentionCurfewActualDate)

    assertThat(licence.bespokeConditions).extracting<Int> { it.conditionSequence }.containsExactly(0, 1)

    val actualConditions = licence.bespokeConditions.map { it.conditionText }
    val expectedConditions = request.conditions.additional.map { it.text } + request.conditions.bespoke.map { it }

    assertThat(actualConditions)
      .containsExactlyElementsOf(expectedConditions)

    assertThat(licence).isInstanceOf(HdcLicence::class.java)
    if (licence is HdcLicence) {
      assertThat(licence.submittedBy?.username).isEqualToIgnoringCase(request.lifecycle.submittedByUserName)
      assertThat(licence.createdBy?.username).isEqualToIgnoringCase(request.lifecycle.createdByUserName)
      assertThat(licence.curfewAddress?.addressLine1).isEqualTo(request.curfewAddress?.addressLine1)
      assertThat(licence.curfewAddress?.addressLine2).isEqualTo(request.curfewAddress?.addressLine2)
      assertThat(licence.curfewAddress?.townOrCity).isEqualTo(request.curfewAddress?.townOrCity)
      assertThat(licence.curfewAddress?.postcode).isEqualTo(request.curfewAddress?.postcode)
      assertThat(licence.homeDetentionCurfewEndDate).isEqualTo(request.licence.homeDetentionCurfewEndDate)
      assertThat(licence.homeDetentionCurfewEligibilityDate).isEqualTo(request.licence.homeDetentionCurfewEligibilityDate)
      assertThat(licence.weeklyCurfewTimes).hasSize(2)
      with(licence.weeklyCurfewTimes[0]) {
        assertThat(curfewTimesSequence).isEqualTo(0)
        assertThat(fromDay).isEqualTo(DayOfWeek.MONDAY)
        assertThat(fromTime).isEqualTo("19:00")
        assertThat(untilTime).isEqualTo("07:00")
        assertThat(untilDay).isEqualTo(DayOfWeek.TUESDAY)
        assertThat(createdTimestamp).isNotNull
      }
      with(licence.weeklyCurfewTimes[1]) {
        assertThat(curfewTimesSequence).isEqualTo(1)
        assertThat(fromDay).isEqualTo(DayOfWeek.FRIDAY)
        assertThat(fromTime).isEqualTo("19:00")
        assertThat(untilTime).isEqualTo("07:00")
        assertThat(untilDay).isEqualTo(DayOfWeek.SATURDAY)
        assertThat(createdTimestamp).isNotNull
      }

      assertThat(licence.firstNightCurfewTimes).isNotNull
      assertThat(licence.firstNightCurfewTimes!!.curfewTimesSequence).isNull()
      assertThat(licence.firstNightCurfewTimes!!.fromTime).isEqualTo("17:00")
      assertThat(licence.firstNightCurfewTimes!!.untilTime).isEqualTo("07:00")
      assertThat(licence.firstNightCurfewTimes!!.createdTimestamp).isNotNull
    }

    assertThat(licence.homeDetentionCurfewActualDate).isEqualTo(request.licence.homeDetentionCurfewActualDate)

    assertThat(licence.appointment?.person).isEqualTo(request.appointment?.person)
    assertThat(licence.appointment?.time).isEqualTo(request.appointment?.time)
    assertThat(licence.appointment?.telephoneContactNumber).isEqualTo(request.appointment?.telephone)
    assertThat(licence.appointment?.address?.firstLine).isEqualTo(request.appointment?.address?.firstLine)
    assertThat(licence.appointment?.address?.secondLine).isEqualTo(request.appointment?.address?.secondLine)
    assertThat(licence.appointment?.address?.townOrCity).isEqualTo(request.appointment?.address?.townOrCity)
    assertThat(licence.appointment?.address?.postcode).isEqualTo(request.appointment?.address?.postcode)

    assertThat(licence.probationAreaCode).isEqualTo("probationArea-code-1")
    assertThat(licence.probationAreaDescription).isEqualTo("probationArea-description-1")
    assertThat(licence.probationPduCode).isEqualTo("borough-code-1")
    assertThat(licence.probationPduDescription).isEqualTo("borough-description-1")
    assertThat(licence.probationLauCode).isEqualTo("district-code-1")
    assertThat(licence.probationLauDescription).isEqualTo("district-description-1")
    assertThat(licence.probationTeamCode).isEqualTo("team-code-1")
    assertThat(licence.probationTeamDescription).isEqualTo("staff-description-1")
  }

  private fun validRequest() = MigrateFromHdcToCvlRequest(
    bookingNo = "A1234BC",
    bookingId = 123456,
    pnc = "YYYY/NNNNNNND",
    cro = "NNNNNN/YYD",
    prisoner = MigratePrisonerDetails(
      prisonerNumber = "A1234BC",
      forename = "forename",
      middleNames = "middleNames",
      surname = "surname",
      dateOfBirth = LocalDate.parse("1974-05-29"),
    ),
    prison = MigratePrisonDetails(
      prisonCode = "MDI",
      prisonDescription = "HMP Example",
      prisonTelephone = "02038219211",
    ),
    sentence = MigrateSentenceDetails(
      sentenceStartDate = LocalDate.parse("2024-01-01"),
      sentenceEndDate = LocalDate.parse("2025-06-01"),
      conditionalReleaseDate = LocalDate.parse("2025-05-01"),
      actualReleaseDate = LocalDate.parse("2025-05-04"),
      topupSupervisionStartDate = LocalDate.parse("2026-05-05"),
      topupSupervisionExpiryDate = LocalDate.parse("2026-11-05"),
      postRecallReleaseDate = LocalDate.parse("2024-08-01"),
    ),
    licence = MigrateLicenceDetails(
      licenceId = 1,
      typeCode = LicenceType.AP,
      licenceActivationDate = LocalDate.parse("2025-05-03"),
      homeDetentionCurfewActualDate = LocalDate.parse("2025-05-04"),
      homeDetentionCurfewEndDate = LocalDate.parse("2025-06-05"),
      homeDetentionCurfewEligibilityDate = LocalDate.parse("2025-06-06"),
      licenceExpiryDate = LocalDate.parse("2026-05-06"),
      licenceVersion = 1,
      varyVersion = 2,
    ),
    lifecycle = MigrateLicenceLifecycleDetails(
      approvedDate = LocalDateTime.parse("2025-11-20T10:00:00"),
      approvedByUsername = "approvedByUsername",
      submittedDate = LocalDateTime.parse("2025-11-20T09:00:00"),
      submittedByUserName = "submittedByUserName",
      createdByUserName = "createdByUserName",
      dateCreated = LocalDateTime.parse("2025-11-20T08:30:00"),
    ),
    conditions = MigrateConditions(
      bespoke = listOf("Licence conditions have been taken from EPF"),
      additional = listOf(
        MigrateAdditionalCondition(
          text = "Do not contact Person",
          conditionCode = "NO_CONTACT_NAMED",
          conditionsVersion = 1,
        ),
      ),
    ),
    curfewAddress = MigrateAddress(
      addressLine1 = "1 Bridge Street",
      addressLine2 = "Flat 1",
      townOrCity = "Newport",
      postcode = "SA42 1DQ",
    ),
    curfew = MigrateCurfewDetails(
      curfewTimes = listOf(
        MigrateCurfewTime(
          fromDay = DayOfWeek.MONDAY,
          fromTime = LocalTime.parse("19:00:00"),
          untilDay = DayOfWeek.TUESDAY,
          untilTime = LocalTime.parse("07:00:00"),
        ),
        MigrateCurfewTime(
          fromDay = DayOfWeek.FRIDAY,
          fromTime = LocalTime.parse("19:00:00"),
          untilDay = DayOfWeek.SATURDAY,
          untilTime = LocalTime.parse("07:00:00"),
        ),
      ),
      firstNight = MigrateFirstNight(
        firstNightFrom = LocalTime.parse("17:00:00"),
        firstNightUntil = LocalTime.parse("07:00:00"),
      ),
    ),
    appointment = MigrateAppointmentDetails(
      person = "Test Person",
      time = LocalDateTime.parse("2025-05-04T14:00:00"),
      telephone = "02038219211",
      address = MigrateAppointmentAddress(
        firstLine = "Probation Office",
        secondLine = "Magistrates Court",
        townOrCity = "Cardiff Place",
        postcode = "SA42 7ND",
      ),
    ),
  )

  private companion object {
    val deliusMockServer = DeliusMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      deliusMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      deliusMockServer.stop()
    }
  }
}
