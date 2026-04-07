package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.migration

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateLicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigratePrisonDetails
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigratePrisonerDetails
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateSentenceDetails
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private const val MIGRATE_URL = "/licences/migration"

class MigrationControllerIntegrationTest : IntegrationTestBase() {

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
  }

  @Test
  fun `should migrate licence successfully`() {
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
    result.expectStatus().isOk
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
      startDate = LocalDate.parse("2024-01-01"),
      endDate = LocalDate.parse("2025-06-01"),
      conditionalReleaseDate = LocalDate.parse("2025-05-01"),
      actualReleaseDate = LocalDate.parse("2025-05-04"),
      topupSupervisionStartDate = LocalDate.parse("2026-05-05"),
      topupSupervisionExpiryDate = LocalDate.parse("2026-11-05"),
      postRecallReleaseDate = LocalDate.parse("2024-08-01"),
    ),
    licence = MigrateLicenceDetails(
      typeCode = MigrateLicenceType.AP,
      statusCode = MigrateStatus.APPROVED,
      licenceActivationDate = LocalDate.parse("2025-05-04"),
      homeDetentionCurfewActualDate = LocalDate.parse("2025-05-04"),
      homeDetentionCurfewEndDate = LocalDate.parse("2025-06-04"),
      licenceExpiryDate = LocalDate.parse("2026-05-04"),
    ),
    lifecycle = MigrateLicenceLifecycleDetails(
      approvedDate = LocalDateTime.parse("2025-11-20T10:00:00"),
      approvedByUsername = "approvedByUsername",
      submittedDate = LocalDateTime.parse("2025-11-20T09:00:00"),
      submittedByUserName = "submittedByUserName",
      createdByUserName = "createdByUserName",
      dateCreated = LocalDateTime.parse("2025-11-20T08:30:00"),
      dateLastUpdated = LocalDateTime.parse("2025-11-20T10:30:00"),
      updatedByUsername = "userByUsername",
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
}
