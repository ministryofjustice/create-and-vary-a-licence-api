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
  val appointmentPerson: String? = null,
  val appointmentTime: LocalDateTime? = null,
  val appointmentAddress: String? = null,
  val appointmentContact: String? = null,
  val spoDiscussion: String? = null,
  val vloDiscussion: String? = null,
  val approvedDate: LocalDateTime? = null,
  val approvedByUsername: String? = null,
  val approvedByName: String? = null,
  val supersededDate: LocalDateTime? = null,
  val submittedDate: LocalDateTime? = null,
  var dateCreated: LocalDateTime? = null,
  val dateLastUpdated: LocalDateTime? = null,
  var updatedByUsername: String? = null,

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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "submitted_by_com_id", nullable = true)
  var submittedBy: CommunityOffenderManager? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_com_id", nullable = false)
  var createdBy: CommunityOffenderManager? = null,

  var variationOfId: Long? = null,
  var versionOfId: Long? = null,
  var licenceVersion: String? = "1.0",
) {

  fun isInPssPeriod(): Boolean {
    val led = licenceExpiryDate
    val tused = topupSupervisionExpiryDate

    if (led != null && tused != null) {
      return led.isBefore(LocalDate.now()) && !(tused.isBefore(LocalDate.now()))
    }
    return false
  }

  fun isActivatedInPssPeriod(): Boolean {
    val led = licenceExpiryDate
    val tused = topupSupervisionExpiryDate
    val lad = licenceActivatedDate

    if (lad != null && led != null && tused != null) {
      return led.isBefore(lad.toLocalDate()) && !(tused.isBefore(lad.toLocalDate()))
    }
    return false
  }

  abstract fun copy(
    id: Long = this.id,
    typeCode: LicenceType = this.typeCode,
    version: String? = this.version,
    statusCode: LicenceStatus = this.statusCode,
    nomsId: String? = this.nomsId,
    bookingNo: String? = this.bookingNo,
    bookingId: Long? = this.bookingId,
    crn: String? = this.crn,
    pnc: String? = this.pnc,
    cro: String? = this.cro,
    prisonCode: String? = this.prisonCode,
    prisonDescription: String? = this.prisonDescription,
    prisonTelephone: String? = this.prisonTelephone,
    forename: String? = this.forename,
    middleNames: String? = this.middleNames,
    surname: String? = this.surname,
    dateOfBirth: LocalDate? = this.dateOfBirth,
    conditionalReleaseDate: LocalDate? = this.conditionalReleaseDate,
    actualReleaseDate: LocalDate? = this.actualReleaseDate,
    sentenceStartDate: LocalDate? = this.sentenceStartDate,
    sentenceEndDate: LocalDate? = this.sentenceEndDate,
    licenceStartDate: LocalDate? = this.licenceStartDate,
    licenceExpiryDate: LocalDate? = this.licenceExpiryDate,
    licenceActivatedDate: LocalDateTime? = this.licenceActivatedDate,
    topupSupervisionStartDate: LocalDate? = this.topupSupervisionStartDate,
    topupSupervisionExpiryDate: LocalDate? = this.topupSupervisionExpiryDate,
    probationAreaCode: String? = this.probationAreaCode,
    probationAreaDescription: String? = this.probationAreaDescription,
    probationPduCode: String? = this.probationPduCode,
    probationPduDescription: String? = this.probationPduDescription,
    probationLauCode: String? = this.probationLauCode,
    probationLauDescription: String? = this.probationLauDescription,
    probationTeamCode: String? = this.probationTeamCode,
    probationTeamDescription: String? = this.probationTeamDescription,
    appointmentPerson: String? = this.appointmentPerson,
    appointmentTime: LocalDateTime? = this.appointmentTime,
    appointmentAddress: String? = this.appointmentAddress,
    appointmentContact: String? = this.appointmentContact,
    spoDiscussion: String? = this.spoDiscussion,
    vloDiscussion: String? = this.vloDiscussion,
    approvedDate: LocalDateTime? = this.approvedDate,
    approvedByUsername: String? = this.approvedByUsername,
    approvedByName: String? = this.approvedByName,
    supersededDate: LocalDateTime? = this.supersededDate,
    submittedDate: LocalDateTime? = this.submittedDate,
    dateCreated: LocalDateTime? = this.dateCreated,
    dateLastUpdated: LocalDateTime? = this.dateLastUpdated,
    updatedByUsername: String? = this.updatedByUsername,
    standardConditions: List<StandardCondition> = this.standardConditions,
    additionalConditions: List<AdditionalCondition> = this.additionalConditions,
    bespokeConditions: List<BespokeCondition> = this.bespokeConditions,
    responsibleCom: CommunityOffenderManager? = this.responsibleCom,
    submittedBy: CommunityOffenderManager? = this.submittedBy,
    createdBy: CommunityOffenderManager? = this.createdBy,
    variationOfId: Long? = this.variationOfId,
    versionOfId: Long? = this.versionOfId,
    licenceVersion: String? = this.licenceVersion,
  ): Licence

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
