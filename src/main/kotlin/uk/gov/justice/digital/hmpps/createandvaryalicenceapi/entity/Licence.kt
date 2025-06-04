package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentPersonType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentPersonType.SPECIFIC_PERSON
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Objects

@Entity
@Table(name = "licence")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "kind", discriminatorType = DiscriminatorType.STRING)
abstract class Licence(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  var id: Long = -1,

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "kind", insertable = false, updatable = false)
  var kind: LicenceKind,

  @NotNull
  @Enumerated(EnumType.STRING)
  var typeCode: LicenceType = LicenceType.AP,

  var version: String? = null,

  @NotNull
  @Enumerated(EnumType.STRING)
  var statusCode: LicenceStatus = LicenceStatus.IN_PROGRESS,

  val nomsId: String? = null,
  val bookingNo: String? = null,
  val bookingId: Long? = null,
  val crn: String? = null,
  val pnc: String? = null,
  val cro: String? = null,
  var prisonCode: String? = null,
  var prisonDescription: String? = null,
  var prisonTelephone: String? = null,
  var forename: String? = null,
  var middleNames: String? = null,
  var surname: String? = null,
  var dateOfBirth: LocalDate? = null,
  override var conditionalReleaseDate: LocalDate? = null,
  override var actualReleaseDate: LocalDate? = null,
  var sentenceStartDate: LocalDate? = null,
  var sentenceEndDate: LocalDate? = null,
  override var licenceStartDate: LocalDate? = null,
  var licenceExpiryDate: LocalDate? = null,
  var licenceActivatedDate: LocalDateTime? = null,
  var topupSupervisionStartDate: LocalDate? = null,
  var topupSupervisionExpiryDate: LocalDate? = null,
  var postRecallReleaseDate: LocalDate? = null,
  var probationAreaCode: String? = null,
  var probationAreaDescription: String? = null,
  var probationPduCode: String? = null,
  var probationPduDescription: String? = null,
  var probationLauCode: String? = null,
  var probationLauDescription: String? = null,
  var probationTeamCode: String? = null,
  var probationTeamDescription: String? = null,

  @Enumerated(EnumType.STRING)
  var appointmentPersonType: AppointmentPersonType? = null,
  var appointmentPerson: String? = null,
  @Enumerated(EnumType.STRING)
  var appointmentTimeType: AppointmentTimeType? = null,
  var appointmentTime: LocalDateTime? = null,
  var appointmentAddress: String? = null,
  var appointmentContact: String? = null,
  var approvedDate: LocalDateTime? = null,
  var approvedByUsername: String? = null,
  var approvedByName: String? = null,
  var supersededDate: LocalDateTime? = null,
  var submittedDate: LocalDateTime? = null,
  var dateCreated: LocalDateTime? = null,
  var dateLastUpdated: LocalDateTime? = null,
  var updatedByUsername: String? = null,
  var licenceVersion: String? = "1.0",

  @OneToMany(
    mappedBy = "licence",
    fetch = FetchType.LAZY,
    cascade = [CascadeType.ALL],
    orphanRemoval = true,
    targetEntity = StandardCondition::class,
  )
  @Fetch(value = FetchMode.SUBSELECT)
  @OrderBy("conditionSequence")
  var standardConditions: MutableList<StandardCondition> = mutableListOf(),

  @OneToMany(mappedBy = "licence", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(value = FetchMode.SUBSELECT)
  @OrderBy("conditionSequence")
  var additionalConditions: MutableList<AdditionalCondition> = mutableListOf(),

  @OneToMany(mappedBy = "licence", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(value = FetchMode.SUBSELECT)
  @OrderBy("conditionSequence")
  var bespokeConditions: MutableList<BespokeCondition> = mutableListOf(),

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "responsible_com_id", nullable = false)
  var responsibleCom: CommunityOffenderManager,

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "updated_by_id", nullable = true)
  var updatedBy: Staff? = null,
) : SentenceDateHolder {

  companion object {
    const val SYSTEM_USER = "SYSTEM_USER"
  }

  fun isInPssPeriod(): Boolean {
    val led = licenceExpiryDate
    val tused = topupSupervisionExpiryDate

    if (led != null && tused != null) {
      return led.isBefore(LocalDate.now()) && !(tused.isBefore(LocalDate.now()))
    }
    return false
  }

  fun activate() {
    statusCode = LicenceStatus.ACTIVE
    licenceActivatedDate = LocalDateTime.now()
  }

  fun deactivate(staffMember: Staff? = null) {
    statusCode = LicenceStatus.INACTIVE
    updatedByUsername = staffMember?.username ?: SYSTEM_USER
    updatedBy = staffMember ?: this.updatedBy
  }

  fun updatePrisonInfo(
    prisonCode: String,
    prisonDescription: String,
    prisonTelephone: String?,
    staffMember: Staff?,
  ) {
    this.prisonCode = prisonCode
    this.prisonDescription = prisonDescription
    this.prisonTelephone = prisonTelephone
    this.dateLastUpdated = LocalDateTime.now()
    this.updatedByUsername = staffMember?.username ?: SYSTEM_USER
    this.updatedBy = staffMember ?: this.updatedBy
  }

  fun updateAppointmentAddress(appointmentAddress: String?, staffMember: Staff?) {
    this.appointmentAddress = appointmentAddress
    this.dateLastUpdated = LocalDateTime.now()
    this.updatedByUsername = staffMember?.username ?: SYSTEM_USER
    this.updatedBy = staffMember ?: this.updatedBy
  }

  fun updateAppointmentContactNumber(appointmentContact: String?, staffMember: Staff?) {
    this.appointmentContact = appointmentContact
    this.dateLastUpdated = LocalDateTime.now()
    this.updatedByUsername = staffMember?.username ?: SYSTEM_USER
    this.updatedBy = staffMember ?: this.updatedBy
  }

  fun updateAppointmentTime(
    appointmentTime: LocalDateTime?,
    appointmentTimeType: AppointmentTimeType,
    staffMember: Staff?,
  ) {
    this.appointmentTime = appointmentTime
    this.appointmentTimeType = appointmentTimeType
    this.dateLastUpdated = LocalDateTime.now()
    this.updatedByUsername = staffMember?.username ?: SYSTEM_USER
    this.updatedBy = staffMember ?: this.updatedBy
  }

  fun updateAppointmentPerson(
    appointmentPersonType: AppointmentPersonType?,
    appointmentPerson: String?,
    staffMember: Staff?,
  ) {
    this.appointmentPersonType = appointmentPersonType
    this.appointmentPerson = if (appointmentPersonType == SPECIFIC_PERSON) appointmentPerson else null
    this.dateLastUpdated = LocalDateTime.now()
    this.updatedByUsername = staffMember?.username ?: SYSTEM_USER
    this.updatedBy = staffMember ?: this.updatedBy
  }

  fun updatePrisonerDetails(
    forename: String,
    middleNames: String?,
    surname: String,
    dateOfBirth: LocalDate,
    staffMember: Staff?,
  ) {
    this.forename = forename
    this.surname = surname
    this.dateOfBirth = dateOfBirth
    this.middleNames = middleNames
    this.dateLastUpdated = LocalDateTime.now()
    this.updatedByUsername = staffMember?.username ?: SYSTEM_USER
    this.updatedBy = staffMember ?: this.updatedBy
  }

  fun updateStatus(
    statusCode: LicenceStatus,
    staffMember: Staff?,
    approvedByUsername: String?,
    approvedByName: String?,
    approvedDate: LocalDateTime?,
    supersededDate: LocalDateTime?,
    submittedDate: LocalDateTime?,
    licenceActivatedDate: LocalDateTime?,
  ) {
    this.statusCode = statusCode
    this.dateLastUpdated = LocalDateTime.now()
    this.updatedByUsername = staffMember?.username ?: SYSTEM_USER
    this.approvedByUsername = approvedByUsername
    this.approvedByName = approvedByName
    this.approvedDate = approvedDate
    this.supersededDate = supersededDate
    this.submittedDate = submittedDate
    this.licenceActivatedDate = licenceActivatedDate
    this.updatedBy = staffMember ?: this.updatedBy
  }

  fun overrideStatus(
    statusCode: LicenceStatus,
    staffMember: Staff?,
    licenceActivatedDate: LocalDateTime?,
  ) {
    this.statusCode = statusCode
    this.updatedByUsername = staffMember?.username ?: SYSTEM_USER
    this.dateLastUpdated = LocalDateTime.now()
    this.licenceActivatedDate = licenceActivatedDate
    this.updatedBy = staffMember ?: this.updatedBy
  }

  fun updateConditions(
    updatedAdditionalConditions: List<AdditionalCondition>? = null,
    updatedStandardConditions: List<StandardCondition>? = null,
    updatedBespokeConditions: List<BespokeCondition>? = null,
    staffMember: Staff?,
  ) {
    if (updatedAdditionalConditions != null) {
      this.additionalConditions.clear()
      this.additionalConditions.addAll(updatedAdditionalConditions)
    }
    if (updatedStandardConditions != null) {
      this.standardConditions.clear()
      this.standardConditions.addAll(updatedStandardConditions)
    }
    if (updatedBespokeConditions != null) {
      this.bespokeConditions.clear()
      this.bespokeConditions.addAll(updatedBespokeConditions)
    }

    this.dateLastUpdated = LocalDateTime.now()
    this.updatedByUsername = staffMember?.username ?: SYSTEM_USER
    this.updatedBy = staffMember ?: this.updatedBy
  }

  fun updateLicenceDates(
    status: LicenceStatus? = null,
    conditionalReleaseDate: LocalDate?,
    actualReleaseDate: LocalDate?,
    sentenceStartDate: LocalDate?,
    sentenceEndDate: LocalDate?,
    licenceStartDate: LocalDate?,
    licenceExpiryDate: LocalDate?,
    topupSupervisionStartDate: LocalDate?,
    topupSupervisionExpiryDate: LocalDate?,
    postRecallReleaseDate: LocalDate?,
    homeDetentionCurfewActualDate: LocalDate?,
    homeDetentionCurfewEndDate: LocalDate?,
    staffMember: Staff?,
  ) {
    this.statusCode = status ?: this.statusCode
    this.conditionalReleaseDate = conditionalReleaseDate
    this.actualReleaseDate = actualReleaseDate
    this.sentenceStartDate = sentenceStartDate
    this.sentenceEndDate = sentenceEndDate
    this.licenceStartDate = licenceStartDate
    this.licenceExpiryDate = licenceExpiryDate
    this.topupSupervisionStartDate = topupSupervisionStartDate
    this.topupSupervisionExpiryDate = topupSupervisionExpiryDate
    this.postRecallReleaseDate = postRecallReleaseDate
    this.dateLastUpdated = LocalDateTime.now()
    this.updatedByUsername = staffMember?.username ?: SYSTEM_USER
    this.updatedBy = staffMember ?: this.updatedBy
    if (this is HdcLicence) {
      this.homeDetentionCurfewActualDate = homeDetentionCurfewActualDate
      this.homeDetentionCurfewEndDate = homeDetentionCurfewEndDate
    }
  }

  fun updateOffenderDetails(
    forename: String?,
    middleNames: String?,
    surname: String?,
    dateOfBirth: LocalDate?,
  ) {
    this.forename = forename
    this.middleNames = middleNames
    this.surname = surname
    this.dateOfBirth = dateOfBirth
  }

  fun updateProbationTeam(
    probationAreaCode: String?,
    probationAreaDescription: String?,
    probationPduCode: String?,
    probationPduDescription: String?,
    probationLauCode: String?,
    probationLauDescription: String?,
    probationTeamCode: String?,
    probationTeamDescription: String?,
  ) {
    this.probationAreaCode = probationAreaCode
    this.probationAreaDescription = probationAreaDescription
    this.probationPduCode = probationPduCode
    this.probationPduDescription = probationPduDescription
    this.probationLauCode = probationLauCode
    this.probationLauDescription = probationLauDescription
    this.probationTeamCode = probationTeamCode
    this.probationTeamDescription = probationTeamDescription
  }

  abstract fun getCreator(): Creator

  fun isActivatedInPssPeriod(): Boolean {
    val led = licenceExpiryDate
    val tused = topupSupervisionExpiryDate
    val lad = licenceActivatedDate

    if (lad != null && led != null && tused != null) {
      return led.isBefore(lad.toLocalDate()) && !(tused.isBefore(lad.toLocalDate()))
    }
    return false
  }

  fun isHdcLicence(): Boolean = kind == LicenceKind.HDC || kind == LicenceKind.HDC_VARIATION

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Licence) return false
    if (id != other.id) return false
    return true
  }

  override fun hashCode(): Int = Objects.hash(id)
}
