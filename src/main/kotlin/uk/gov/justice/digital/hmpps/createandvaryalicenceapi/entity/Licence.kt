package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Objects

@Entity
@Table(name = "licence")
class Licence(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  val id: Long = -1,

  @NotNull
  @Enumerated(STRING)
  val typeCode: LicenceType = AP,

  var version: String? = null,

  @NotNull
  @Enumerated(STRING)
  var statusCode: LicenceStatus = IN_PROGRESS,

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
  var paroleEligibilityDate: LocalDate? = null,
  var nonParoleDate: LocalDate? = null,
  var postRecallReleaseDate: LocalDate? = null,

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

  @ManyToOne(fetch = FetchType.LAZY)
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
  fun copyLicence(newStatus: LicenceStatus, newVersion: String?): Licence {
    return Licence(
      typeCode = this.typeCode,
      version = this.version,
      statusCode = newStatus,
      nomsId = this.nomsId,
      bookingNo = this.bookingNo,
      bookingId = this.bookingId,
      crn = this.crn,
      pnc = this.pnc,
      cro = this.cro,
      prisonCode = this.prisonCode,
      prisonDescription = this.prisonDescription,
      prisonTelephone = this.prisonTelephone,
      forename = this.forename,
      middleNames = this.middleNames,
      surname = this.surname,
      dateOfBirth = this.dateOfBirth,
      conditionalReleaseDate = this.conditionalReleaseDate,
      actualReleaseDate = this.actualReleaseDate,
      sentenceStartDate = this.sentenceStartDate,
      sentenceEndDate = this.sentenceEndDate,
      licenceStartDate = this.licenceStartDate,
      licenceExpiryDate = this.licenceExpiryDate,
      topupSupervisionStartDate = this.topupSupervisionStartDate,
      topupSupervisionExpiryDate = this.topupSupervisionExpiryDate,
      probationAreaCode = this.probationAreaCode,
      probationAreaDescription = this.probationAreaDescription,
      probationPduCode = this.probationPduCode,
      probationPduDescription = this.probationPduDescription,
      probationLauCode = this.probationLauCode,
      probationLauDescription = this.probationLauDescription,
      probationTeamCode = this.probationTeamCode,
      probationTeamDescription = this.probationTeamDescription,
      appointmentPerson = this.appointmentPerson,
      appointmentTime = this.appointmentTime,
      appointmentAddress = this.appointmentAddress,
      appointmentContact = this.appointmentContact,
      responsibleCom = this.responsibleCom,
      dateCreated = LocalDateTime.now(),
      licenceVersion = newVersion,
    )
  }

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

  fun copy(
    id: @NotNull Long = this.id,
    typeCode: @NotNull LicenceType = this.typeCode,
    version: String? = this.version,
    statusCode: @NotNull LicenceStatus = this.statusCode,
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
  ): Licence {
    return Licence(
      id,
      typeCode,
      version,
      statusCode,
      nomsId,
      bookingNo,
      bookingId,
      crn,
      pnc,
      cro,
      prisonCode,
      prisonDescription,
      prisonTelephone,
      forename,
      middleNames,
      surname,
      dateOfBirth,
      conditionalReleaseDate,
      actualReleaseDate,
      sentenceStartDate,
      sentenceEndDate,
      licenceStartDate,
      licenceExpiryDate,
      licenceActivatedDate,
      topupSupervisionStartDate,
      topupSupervisionExpiryDate,
      probationAreaCode,
      probationAreaDescription,
      probationPduCode,
      probationPduDescription,
      probationLauCode,
      probationLauDescription,
      probationTeamCode,
      probationTeamDescription,
      appointmentPerson,
      appointmentTime,
      appointmentAddress,
      appointmentContact,
      spoDiscussion,
      vloDiscussion,
      approvedDate,
      approvedByUsername,
      approvedByName,
      supersededDate,
      submittedDate,
      dateCreated,
      dateLastUpdated,
      updatedByUsername,
      standardConditions,
      additionalConditions,
      bespokeConditions,
      responsibleCom,
      submittedBy,
      createdBy,
      variationOfId,
      versionOfId,
      licenceVersion,
    )
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Licence

    if (id != other.id) return false
    if (typeCode != other.typeCode) return false
    if (version != other.version) return false
    if (statusCode != other.statusCode) return false
    if (nomsId != other.nomsId) return false
    if (bookingNo != other.bookingNo) return false
    if (bookingId != other.bookingId) return false
    if (crn != other.crn) return false
    if (pnc != other.pnc) return false
    if (cro != other.cro) return false
    if (prisonCode != other.prisonCode) return false
    if (prisonDescription != other.prisonDescription) return false
    if (prisonTelephone != other.prisonTelephone) return false
    if (forename != other.forename) return false
    if (middleNames != other.middleNames) return false
    if (surname != other.surname) return false
    if (dateOfBirth != other.dateOfBirth) return false
    if (conditionalReleaseDate != other.conditionalReleaseDate) return false
    if (actualReleaseDate != other.actualReleaseDate) return false
    if (sentenceStartDate != other.sentenceStartDate) return false
    if (sentenceEndDate != other.sentenceEndDate) return false
    if (licenceStartDate != other.licenceStartDate) return false
    if (licenceExpiryDate != other.licenceExpiryDate) return false
    if (licenceActivatedDate != other.licenceActivatedDate) return false
    if (topupSupervisionStartDate != other.topupSupervisionStartDate) return false
    if (topupSupervisionExpiryDate != other.topupSupervisionExpiryDate) return false
    if (probationAreaCode != other.probationAreaCode) return false
    if (probationAreaDescription != other.probationAreaDescription) return false
    if (probationPduCode != other.probationPduCode) return false
    if (probationPduDescription != other.probationPduDescription) return false
    if (probationLauCode != other.probationLauCode) return false
    if (probationLauDescription != other.probationLauDescription) return false
    if (probationTeamCode != other.probationTeamCode) return false
    if (probationTeamDescription != other.probationTeamDescription) return false
    if (appointmentPerson != other.appointmentPerson) return false
    if (appointmentTime != other.appointmentTime) return false
    if (appointmentAddress != other.appointmentAddress) return false
    if (appointmentContact != other.appointmentContact) return false
    if (spoDiscussion != other.spoDiscussion) return false
    if (vloDiscussion != other.vloDiscussion) return false
    if (approvedDate != other.approvedDate) return false
    if (approvedByUsername != other.approvedByUsername) return false
    if (approvedByName != other.approvedByName) return false
    if (supersededDate != other.supersededDate) return false
    if (submittedDate != other.submittedDate) return false
    if (dateCreated != other.dateCreated) return false
    if (dateLastUpdated != other.dateLastUpdated) return false
    if (updatedByUsername != other.updatedByUsername) return false
    if (standardConditions != other.standardConditions) return false
    if (additionalConditions != other.additionalConditions) return false
    if (bespokeConditions != other.bespokeConditions) return false
    if (responsibleCom != other.responsibleCom) return false
    if (submittedBy != other.submittedBy) return false
    if (createdBy != other.createdBy) return false
    if (variationOfId != other.variationOfId) return false
    if (versionOfId != other.versionOfId) return false
    if (licenceVersion != other.licenceVersion) return false

    return true
  }

  override fun hashCode(): Int {
    return Objects.hash(
      id,
      typeCode,
      version,
      statusCode,
      nomsId,
      bookingNo,
      bookingId,
      crn,
      pnc,
      cro,
      prisonCode,
      prisonDescription,
      prisonTelephone,
      forename,
      middleNames,
      surname,
      dateOfBirth,
      conditionalReleaseDate,
      actualReleaseDate,
      sentenceStartDate,
      sentenceEndDate,
      licenceStartDate,
      licenceExpiryDate,
      licenceActivatedDate,
      topupSupervisionStartDate,
      topupSupervisionExpiryDate,
      probationAreaCode,
      probationAreaDescription,
      probationPduCode,
      probationPduDescription,
      probationLauCode,
      probationLauDescription,
      probationTeamCode,
      probationTeamDescription,
      appointmentPerson,
      appointmentTime,
      appointmentAddress,
      appointmentContact,
      spoDiscussion,
      vloDiscussion,
      approvedDate,
      approvedByUsername,
      approvedByName,
      supersededDate,
      submittedDate,
      dateCreated,
      dateLastUpdated,
      updatedByUsername,
      standardConditions,
      additionalConditions,
      bespokeConditions,
      responsibleCom,
      submittedBy,
      createdBy,
      variationOfId,
      versionOfId,
      licenceVersion,
    )
  }

  override fun toString(): String {
    return "Licence(" +
      "id=$id, " +
      "typeCode=$typeCode, " +
      "version=$version, " +
      "statusCode=$statusCode, " +
      "nomsId=$nomsId, " +
      "bookingNo=$bookingNo, " +
      "bookingId=$bookingId, " +
      "crn=$crn, " +
      "pnc=$pnc, " +
      "cro=$cro, " +
      "prisonCode=$prisonCode, " +
      "prisonDescription=$prisonDescription, " +
      "prisonTelephone=$prisonTelephone, " +
      "forename=$forename, " +
      "middleNames=$middleNames, " +
      "surname=$surname, " +
      "dateOfBirth=$dateOfBirth, " +
      "conditionalReleaseDate=$conditionalReleaseDate, " +
      "actualReleaseDate=$actualReleaseDate, " +
      "sentenceStartDate=$sentenceStartDate, " +
      "sentenceEndDate=$sentenceEndDate, " +
      "licenceStartDate=$licenceStartDate, " +
      "licenceExpiryDate=$licenceExpiryDate, " +
      "licenceActivatedDate=$licenceActivatedDate, " +
      "topupSupervisionStartDate=$topupSupervisionStartDate, " +
      "topupSupervisionExpiryDate=$topupSupervisionExpiryDate, " +
      "probationAreaCode=$probationAreaCode, " +
      "probationAreaDescription=$probationAreaDescription, " +
      "probationPduCode=$probationPduCode, " +
      "probationPduDescription=$probationPduDescription, " +
      "probationLauCode=$probationLauCode, " +
      "probationLauDescription=$probationLauDescription, " +
      "probationTeamCode=$probationTeamCode, " +
      "probationTeamDescription=$probationTeamDescription, " +
      "appointmentPerson=$appointmentPerson, " +
      "appointmentTime=$appointmentTime, " +
      "appointmentAddress=$appointmentAddress, " +
      "appointmentContact=$appointmentContact, " +
      "spoDiscussion=$spoDiscussion, " +
      "vloDiscussion=$vloDiscussion, " +
      "approvedDate=$approvedDate, " +
      "approvedByUsername=$approvedByUsername, " +
      "approvedByName=$approvedByName, " +
      "supersededDate=$supersededDate, " +
      "submittedDate=$submittedDate, " +
      "dateCreated=$dateCreated, " +
      "dateLastUpdated=$dateLastUpdated, " +
      "updatedByUsername=$updatedByUsername, " +
      "standardConditions=$standardConditions, " +
      "additionalConditions=$additionalConditions, " +
      "bespokeConditions=$bespokeConditions, " +
      "responsibleCom=$responsibleCom, " +
      "submittedBy=$submittedBy, " +
      "createdBy=$createdBy, " +
      "variationOfId=$variationOfId, " +
      "versionOfId=$versionOfId, " +
      "licenceVersion=$licenceVersion" +
      ")"
  }
}
