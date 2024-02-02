package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonView
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Describes a licence within this service")
object LicenceKinds {
  const val CRD = "CRD"
  const val VARIATION = "VARIATION"
  const val HARD_STOP = "HARD_STOP"
}

@Schema(
  description = "Describes a licence within this service, A discriminator exists to distinguish between different types of licence",
  oneOf = [CrdLicence::class, VariationLicence::class, HardStopLicence::class],
  discriminatorProperty = "kind",
  discriminatorMapping = [
    DiscriminatorMapping(value = LicenceKinds.CRD, schema = CrdLicence::class),
    DiscriminatorMapping(value = LicenceKinds.VARIATION, schema = VariationLicence::class),
    DiscriminatorMapping(value = LicenceKinds.HARD_STOP, schema = HardStopLicence::class),
  ],
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
sealed interface Licence {
  val kind: String

  @get:Deprecated("Soon to be removed, supports backward compatibility, should use kind to determine")
  val isVariation: Boolean

  @get:Schema(description = "Unique identifier for this licence within the service", example = "99999")
  val id: Long

  @get:Schema(description = "The licence type code", example = "AP")
  @get:JsonView(Views.PublicSar::class)
  val typeCode: LicenceType

  @get:Schema(description = "The version number used for standard and additional conditions", example = "1.4")
  @get:JsonView(Views.PublicSar::class)
  val version: String?

  @get:Schema(
    description = "The current status code for this licence",
    example = "IN_PROGRESS",
  )
  @get:JsonView(Views.PublicSar::class)
  val statusCode: LicenceStatus?

  @get:Schema(description = "The prison identifier for the person on this licence", example = "A9999AA")
  @get:JsonView(Views.PublicSar::class)
  val nomsId: String?

  @get:Schema(description = "The prison booking number for the person on this licence", example = "F12333")
  val bookingNo: String?

  @get:Schema(description = "The prison internal booking ID for the person on this licence", example = "989898")
  @get:JsonView(Views.PublicSar::class)
  val bookingId: Long?

  @get:Schema(description = "The case reference number (CRN) for the person on this licence", example = "X12444")
  val crn: String?

  @get:Schema(
    description = "The police national computer number (PNC) for the person on this licence",
    example = "2015/12444",
  )
  val pnc: String?

  @get:Schema(
    description = "The criminal records office number (CRO) for the person on this licence",
    example = "A/12444",
  )
  val cro: String?

  @get:Schema(description = "The agency code of the detaining prison", example = "LEI")
  val prisonCode: String?

  @get:Schema(description = "The agency description of the detaining prison", example = "Leeds (HMP)")
  val prisonDescription: String?

  @get:Schema(description = "The telephone number to contact the prison", example = "0161 234 4747")
  val prisonTelephone: String?

  @get:Schema(description = "The first name of the person on licence", example = "Michael")
  val forename: String?

  @get:Schema(description = "The middle names of the person on licence", example = "John Peter")
  val middleNames: String?

  @get:Schema(description = "The family name of the person on licence", example = "Smith")
  val surname: String?

  @get:Schema(description = "The date of birth of the person on licence", example = "12/05/1987")
  val dateOfBirth: LocalDate?

  @get:Schema(description = "The earliest conditional release date of the person on licence", example = "13/08/2022")
  val conditionalReleaseDate: LocalDate?

  @get:Schema(description = "The actual release date (if set)", example = "13/09/2022")
  val actualReleaseDate: LocalDate?

  @get:Schema(description = "The sentence start date", example = "13/09/2019")
  val sentenceStartDate: LocalDate?

  @get:Schema(description = "The sentence end date", example = "13/09/2022")
  val sentenceEndDate: LocalDate?

  @get:Schema(description = "The date that the licence will start", example = "13/09/2022")
  val licenceStartDate: LocalDate?

  @get:Schema(description = "The date that the licence will expire", example = "13/09/2024")
  val licenceExpiryDate: LocalDate?

  @get:Schema(
    description = "The date when the post sentence supervision period starts, from prison services",
    example = "06/05/2023",
  )
  val topupSupervisionStartDate: LocalDate?

  @get:Schema(
    description = "The date when the post sentence supervision period ends, from prison services",
    example = "06/06/2023",
  )
  val topupSupervisionExpiryDate: LocalDate?

  @get:Schema(description = "The nDELIUS user name for the supervising probation officer", example = "X32122")
  val comUsername: String?

  @get:Schema(description = "The nDELIUS staff identifier for the supervising probation officer", example = "12345")
  val comStaffId: Long?

  @get:Schema(
    description = "The email address for the supervising probation officer",
    example = "jane.jones@nps.gov.uk",
  )
  val comEmail: String?

  @get:Schema(description = "The probation area code where this licence is supervised from", example = "N01")
  val probationAreaCode: String?

  @get:Schema(description = "The probation area description", example = "Wales")
  val probationAreaDescription: String?

  @get:Schema(description = "The Probation Delivery Unit (PDU or borough) supervising this licence", example = "PDU01")
  val probationPduCode: String?

  @get:Schema(description = "The description for the PDU", example = "North Wales")
  val probationPduDescription: String?

  @get:Schema(
    description = "The Local Administrative Unit (LAU or district) supervising this licence",
    example = "LAU01",
  )
  val probationLauCode: String?

  @get:Schema(description = "The LAU description", example = "North Wales")
  val probationLauDescription: String?

  @get:Schema(description = "The team code that is supervising this licence", example = "Cardiff-A")
  val probationTeamCode: String?

  @get:Schema(description = "The team description", example = "Cardiff South")
  val probationTeamDescription: String?

  @get:Schema(description = "Who the person will meet at their initial appointment", example = "Duty officer")
  @get:JsonView(Views.PublicSar::class)
  val appointmentPerson: String?

  @get:Schema(description = "The date and time of the initial appointment", example = "23/08/2022 12:12")
  @get:JsonView(Views.PublicSar::class)
  val appointmentTime: LocalDateTime?

  @get:Schema(description = "The type of appointment time of the initial appointment", example = "SPECIFIC_DATE_TIME")
  @get:JsonView(Views.PublicSar::class)
  val appointmentTimeType: AppointmentTimeType?

  @get:Schema(
    description = "The address of initial appointment",
    example = "Manchester Probation Service, Unit 4, Smith Street, Stockport, SP1 3DN",
  )
  @get:JsonView(Views.PublicSar::class)
  val appointmentAddress: String?

  @get:Schema(
    description = "The UK telephone number to contact the person the offender should meet for their initial meeting",
    example = "0114 2557665",
  )
  @get:JsonView(Views.PublicSar::class)
  val appointmentContact: String?

  @get:Schema(description = "The date and time that this prison approved this licence", example = "24/08/2022 11:30:33")
  @get:JsonView(Views.PublicSar::class)
  val approvedDate: LocalDateTime?

  @get:Schema(
    description = "The username who approved the licence on behalf of the prison governor",
    example = "X33221",
  )
  @get:JsonView(Views.PublicSar::class)
  val approvedByUsername: String?

  @get:Schema(
    description = "The date and time that this licence was submitted for approval",
    example = "24/08/2022 11:30:33",
  )
  @get:JsonView(Views.PublicSar::class)
  val submittedDate: LocalDateTime?

  @get:Schema(
    description = "The full name of the person who approved the licence on behalf of the prison governor",
    example = "John Smith",
  )
  @get:JsonView(Views.PublicSar::class)
  val approvedByName: String?

  @get:Schema(
    description = "The date and time that this licence was superseded by a new variant",
    example = "24/08/2022 11:30:33",
  )
  @get:JsonView(Views.PublicSar::class)
  val supersededDate: LocalDateTime?

  @get:Schema(description = "The date and time that this licence was first created", example = "24/08/2022 09:30:33")
  @get:JsonView(Views.PublicSar::class)
  val dateCreated: LocalDateTime?

  @get:Schema(description = "The username which created this licence", example = "X12333")
  @get:JsonView(Views.PublicSar::class)
  val createdByUsername: String?

  @get:Schema(description = "The date and time that this licence was last updated", example = "24/08/2022 09:30:33")
  @get:JsonView(Views.PublicSar::class)
  val dateLastUpdated: LocalDateTime?

  @get:Schema(description = "The username of the person who last updated this licence", example = "X34433")
  @get:JsonView(Views.PublicSar::class)
  val updatedByUsername: String?

  @get:Schema(description = "The list of standard licence conditions on this licence")
  @get:JsonView(Views.PublicSar::class)
  val standardLicenceConditions: List<StandardCondition>?

  @get:Schema(description = "The list of standard post sentence supervision conditions on this licence")
  @get:JsonView(Views.PublicSar::class)
  val standardPssConditions: List<StandardCondition>?

  @get:Schema(description = "The list of additional licence conditions on this licence")
  @get:JsonView(Views.PublicSar::class)
  val additionalLicenceConditions: List<AdditionalCondition>

  @get:Schema(description = "The list of additional post sentence supervision conditions on this licence")
  @get:JsonView(Views.PublicSar::class)
  val additionalPssConditions: List<AdditionalCondition>

  @get:Schema(description = "The list of bespoke conditions on this licence")
  @get:JsonView(Views.PublicSar::class)
  val bespokeConditions: List<BespokeCondition>

  @get:Schema(description = "The full name of the person who created licence or variation", example = "Gordon Sumner")
  @get:JsonView(Views.PublicSar::class)
  val createdByFullName: String?

  @get:Schema(description = "Is this licence in PSS period?(LED < TODAY <= TUSED)")
  val isInPssPeriod: Boolean?

  @get:Schema(description = "Is this licence activated in PSS period?(LED < LAD <= TUSED)")
  val isActivatedInPssPeriod: Boolean?

  @get:Schema(description = "The version number of this licence", example = "1.3")
  @get:JsonView(Views.PublicSar::class)
  val licenceVersion: String?

  @get:Schema(description = "If ARD||CRD falls on Friday/Bank holiday/Weekend then it contains Earliest possible release date or ARD||CRD")
  val earliestReleaseDate: LocalDate?

  @get:Schema(description = "If ARD||CRD falls on Friday/Bank holiday/Weekend then it is eligible for early release)")
  val isEligibleForEarlyRelease: Boolean
}
