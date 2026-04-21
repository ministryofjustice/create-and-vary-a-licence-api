package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request to migrate CVL data")
data class MigrateFromHdcToCvlRequest(
  @field:Schema(description = "Booking number", example = "A1234BC")
  val bookingNo: String?,

  @field:Schema(description = "Booking ID", example = "987654")
  val bookingId: Long,

  @field:Schema(description = "PNC number", example = "YYYY/NNNNNNND")
  val pnc: String?,

  @field:Schema(description = "CRO number", example = "NNNNNN/YYD")
  val cro: String?,

  @field:Schema(description = "Prisoner personal details")
  val prisoner: MigratePrisonerDetails,

  @field:Schema(description = "Prison details")
  val prison: MigratePrisonDetails,

  @field:Schema(description = "Sentence dates")
  val sentence: MigrateSentenceDetails,

  @field:Schema(description = "Licence details")
  val licence: MigrateLicenceDetails,

  @field:Schema(description = "Lifecycle details")
  val lifecycle: MigrateLicenceLifecycleDetails,

  @field:Schema(description = "Licence conditions")
  val conditions: MigrateConditions,

  @field:Schema(description = "Approved curfew address")
  val curfewAddress: MigrateAddress?,

  @field:Schema(description = "Curfew information")
  val curfew: MigrateCurfewDetails?,

  @field:Schema(description = "Appointment details")
  val appointment: MigrateAppointmentDetails?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Prison details")
data class MigratePrisonDetails(
  @field:Schema(description = "Prison code", example = "MDI")
  val prisonCode: String?,

  @field:Schema(description = "Prison description", example = "HMP Example")
  val prisonDescription: String?,

  @field:Schema(description = "Prison telephone number", example = "02038219211")
  val prisonTelephone: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Prisoner personal details")
data class MigratePrisonerDetails(
  @field:Schema(description = "Prisoner Number", example = "A1234BC")
  val prisonerNumber: String?,

  @field:Schema(description = "Forename")
  val forename: String?,

  @field:Schema(description = "Middle Names")
  val middleNames: String?,

  @field:Schema(description = "Surname")
  val surname: String?,

  @field:Schema(description = "Date of birth", example = "1974-05-29")
  val dateOfBirth: LocalDate?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Sentence details")
data class MigrateSentenceDetails(
  @field:Schema(description = "Sentence start date", example = "2024-01-01")
  val sentenceStartDate: LocalDate?,

  @field:Schema(description = "Sentence end date", example = "2025-06-01")
  val sentenceEndDate: LocalDate?,

  @field:Schema(description = "Conditional release date", example = "2025-05-01")
  val conditionalReleaseDate: LocalDate?,

  @field:Schema(description = "Actual release date", example = "2025-05-04")
  val actualReleaseDate: LocalDate?,

  @field:Schema(description = "Top-up supervision start date", example = "2026-05-05")
  val topupSupervisionStartDate: LocalDate?,

  @field:Schema(description = "Top-up supervision expiry date", example = "2026-11-05")
  val topupSupervisionExpiryDate: LocalDate?,

  @field:Schema(description = "Post-recall release date", example = "2024-08-01")
  val postRecallReleaseDate: LocalDate?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Licence details")
data class MigrateLicenceDetails(

  @field:Schema(description = "HDC licence ID", example = "1")
  val licenceId: Long,

  @field:Schema(description = "Licence type", example = "AP")
  val typeCode: LicenceType,

  @field:Schema(description = "Licence activation date", example = "2025-05-04")
  val licenceActivationDate: LocalDate?,

  @field:Schema(description = "HDC actual date", example = "2025-05-04")
  val homeDetentionCurfewActualDate: LocalDate?,

  @field:Schema(description = "HDC end date", example = "2025-06-04")
  val homeDetentionCurfewEndDate: LocalDate?,

  @field:Schema(description = "HDC eligibility date", example = "2025-06-04")
  val homeDetentionCurfewEligibilityDate: LocalDate?,

  @field:Schema(description = "Licence expiry date", example = "2026-05-04")
  val licenceExpiryDate: LocalDate?,

  @field:Schema(description = "HDC licence version", example = "1")
  val licenceVersion: Int,

  @field:Schema(description = "HDC vary version", example = "2")
  val varyVersion: Int,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Lifecycle details")
data class MigrateLicenceLifecycleDetails(
  @field:Schema(description = "Approved date", example = "2025-11-20T10:00:00")
  val approvedDate: LocalDateTime?,

  @field:Schema(description = "Approved by username", example = "username")
  val approvedByUsername: String?,

  @field:Schema(description = "Submitted date", example = "2025-11-20T09:00:00")
  val submittedDate: LocalDateTime?,

  @field:Schema(description = "Submitted by", example = "username")
  val submittedByUserName: String?,

  @field:Schema(description = "Created by", example = "username")
  val createdByUserName: String?,

  @field:Schema(description = "Date created", example = "2025-11-20T08:30:00")
  val dateCreated: LocalDateTime?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Licence conditions")
data class MigrateConditions(
  @field:Schema(description = "Bespoke conditions", example = "[\"Licence conditions have been taken from EPF\"]")
  val bespoke: List<String> = emptyList(),

  @field:Schema(description = "Additional conditions")
  val additional: List<MigrateAdditionalCondition> = emptyList(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Additional licence condition")
data class MigrateAdditionalCondition(
  @field:Schema(description = "Condition text", example = "Do not contact Person")
  val text: String,

  @field:Schema(description = "Condition code", example = "NO_CONTACT_NAMED")
  val conditionCode: String,

  @field:Schema(description = "Condition version", example = "1")
  val conditionsVersion: Int,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Address details")
data class MigrateAddress(
  @field:Schema(description = "Address line 1", example = "1 Bridge Street")
  val addressLine1: String?,

  @field:Schema(description = "Address line 2", example = "Flat 1")
  val addressLine2: String?,

  @field:Schema(description = "Town or city", example = "Newport")
  val townOrCity: String?,

  @field:Schema(description = "Postcode", example = "SA42 1DQ")
  val postcode: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Curfew details")
data class MigrateCurfewDetails(
  @field:Schema(description = "Curfew times")
  val curfewTimes: List<MigrateCurfewTime>? = emptyList(),

  @field:Schema(description = "First night curfew details")
  val firstNight: MigrateFirstNight?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Individual curfew time block")
data class MigrateCurfewTime(
  @field:Schema(description = "Start day", example = "MONDAY")
  val fromDay: DayOfWeek? = null,

  @field:Schema(description = "Start time", example = "19:00:00")
  val fromTime: LocalTime,

  @field:Schema(description = "End day", example = "TUESDAY")
  val untilDay: DayOfWeek? = null,

  @field:Schema(description = "End time", example = "07:00:00")
  val untilTime: LocalTime,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "First night curfew details")
data class MigrateFirstNight(
  @field:Schema(description = "First night from time", example = "17:00:00")
  val firstNightFrom: LocalTime,

  @field:Schema(description = "First night until time", example = "07:00:00")
  val firstNightUntil: LocalTime,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Appointment details")
data class MigrateAppointmentDetails(
  @field:Schema(description = "Person name", example = "Test Person")
  val person: String?,

  @field:Schema(description = "Appointment time", example = "2025-05-04T14:00:00")
  val time: LocalDateTime?,

  @field:Schema(description = "Telephone", example = "02038219211")
  val telephone: String?,

  @field:Schema(description = "Appointment address")
  val address: MigrateAppointmentAddress?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Appointment address")
data class MigrateAppointmentAddress(
  @field:Schema(description = "First line of address", example = "Probation Office")
  val firstLine: String?,

  @field:Schema(description = "Second line of address", example = "Magistrates Court")
  val secondLine: String?,

  @field:Schema(description = "Town or city", example = "Cardiff Place")
  val townOrCity: String?,

  @field:Schema(description = "Postcode", example = "SA42 7ND")
  val postcode: String?,
)
