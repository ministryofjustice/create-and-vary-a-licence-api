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
  val typeCode: LicenceType = LicenceType.AP,

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
  val prisonCode: String? = null,
  val prisonDescription: String? = null,
  val prisonTelephone: String? = null,
  val forename: String? = null,
  val middleNames: String? = null,
  val surname: String? = null,
  val dateOfBirth: LocalDate? = null,
  val conditionalReleaseDate: LocalDate? = null,
  val actualReleaseDate: LocalDate? = null,
  val sentenceStartDate: LocalDate? = null,
  val sentenceEndDate: LocalDate? = null,
  val licenceStartDate: LocalDate? = null,
  val licenceExpiryDate: LocalDate? = null,
  val licenceActivatedDate: LocalDateTime? = null,
  val topupSupervisionStartDate: LocalDate? = null,
  val topupSupervisionExpiryDate: LocalDate? = null,
  val probationAreaCode: String? = null,
  val probationAreaDescription: String? = null,
  val probationPduCode: String? = null,
  val probationPduDescription: String? = null,
  val probationLauCode: String? = null,
  val probationLauDescription: String? = null,
  val probationTeamCode: String? = null,
  val probationTeamDescription: String? = null,

  var appointmentPerson: String? = null,
  @Enumerated(EnumType.STRING)
  var appointmentTimeType: AppointmentTimeType? = AppointmentTimeType.SPECIFIC_DATE_TIME,
  var appointmentTime: LocalDateTime? = null,
  var appointmentAddress: String? = null,
  var appointmentContact: String? = null,
  val approvedDate: LocalDateTime? = null,
  val approvedByUsername: String? = null,
  val approvedByName: String? = null,
  val supersededDate: LocalDateTime? = null,
  val submittedDate: LocalDateTime? = null,
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
  var standardConditions: List<StandardCondition> = emptyList(),

  @OneToMany(mappedBy = "licence", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(value = FetchMode.SUBSELECT)
  @OrderBy("conditionSequence")
  val additionalConditions: List<AdditionalCondition> = emptyList(),

  @OneToMany(mappedBy = "licence", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(value = FetchMode.SUBSELECT)
  @OrderBy("conditionSequence")
  val bespokeConditions: List<BespokeCondition> = emptyList(),

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "responsible_com_id", nullable = false)
  var responsibleCom: CommunityOffenderManager? = null,
) {

  fun isInPssPeriod(): Boolean {
    val led = licenceExpiryDate
    val tused = topupSupervisionExpiryDate

    if (led != null && tused != null) {
      return led.isBefore(LocalDate.now()) && !(tused.isBefore(LocalDate.now()))
    }
    return false
  }

  abstract fun activate(): Licence
  abstract fun deactivate(): Licence
  abstract fun deactivate(updatedByUsername: String): Licence

  abstract fun updatePrisonInfo(
    prisonCode: String,
    prisonDescription: String,
    prisonTelephone: String?,
    updatedByUsername: String?,
  ): Licence

  fun updateAppointmentAddress(appointmentAddress: String?, updatedByUsername: String?) {
    this.appointmentAddress = appointmentAddress
    this.dateLastUpdated = LocalDateTime.now()
    this.updatedByUsername = updatedByUsername
  }

  fun updateAppointmentContactNumber(appointmentContact: String?, updatedByUsername: String?) {
    this.appointmentContact = appointmentContact
    this.dateLastUpdated = LocalDateTime.now()
    this.updatedByUsername = updatedByUsername
  }

  fun updateAppointmentTime(
    appointmentTime: LocalDateTime?,
    appointmentTimeType: AppointmentTimeType,
    updatedByUsername: String?,
  ) {
    this.appointmentTime = appointmentTime
    this.appointmentTimeType = appointmentTimeType
    this.dateLastUpdated = LocalDateTime.now()
    this.updatedByUsername = updatedByUsername
  }

  fun updateAppointmentPerson(appointmentPerson: String?, updatedByUsername: String?) {
    this.appointmentPerson = appointmentPerson
    this.dateLastUpdated = LocalDateTime.now()
    this.updatedByUsername = updatedByUsername
  }

  abstract fun updateStatus(
    statusCode: LicenceStatus,
    updatedByUsername: String,
    approvedByUsername: String?,
    approvedByName: String?,
    approvedDate: LocalDateTime?,
    supersededDate: LocalDateTime?,
    submittedDate: LocalDateTime?,
    licenceActivatedDate: LocalDateTime?,
  ): Licence

  abstract fun overrideStatus(
    statusCode: LicenceStatus,
    updatedByUsername: String?,
    licenceActivatedDate: LocalDateTime?,
  ): Licence

  abstract fun updateConditions(
    updatedAdditionalConditions: List<AdditionalCondition>? = null,
    updatedStandardConditions: List<StandardCondition>? = null,
    updatedBespokeConditions: List<BespokeCondition>? = null,
    updatedByUsername: String?,
  ): Licence

  abstract fun updateLicenceDates(
    status: LicenceStatus? = null,
    conditionalReleaseDate: LocalDate?,
    actualReleaseDate: LocalDate?,
    sentenceStartDate: LocalDate?,
    sentenceEndDate: LocalDate?,
    licenceStartDate: LocalDate?,
    licenceExpiryDate: LocalDate?,
    topupSupervisionStartDate: LocalDate?,
    topupSupervisionExpiryDate: LocalDate?,
    updatedByUsername: String?,
  ): Licence

  abstract fun updateOffenderDetails(
    forename: String?,
    middleNames: String?,
    surname: String?,
    dateOfBirth: LocalDate?,
  ): Licence

  abstract fun updateProbationTeam(
    probationAreaCode: String?,
    probationAreaDescription: String?,
    probationPduCode: String?,
    probationPduDescription: String?,
    probationLauCode: String?,
    probationLauDescription: String?,
    probationTeamCode: String?,
    probationTeamDescription: String?,
  ): Licence

  abstract fun updateResponsibleCom(responsibleCom: CommunityOffenderManager): Licence

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

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Licence) return false
    if (id != other.id) return false
    return true
  }

  override fun hashCode(): Int {
    return Objects.hash(id)
  }
}
