package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderBy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentPersonType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@DiscriminatorValue(value = "HDC_VARIATION")
class HdcVariationLicence(
  id: Long = -1L,
  typeCode: LicenceType,
  version: String? = null,
  statusCode: LicenceStatus = LicenceStatus.VARIATION_IN_PROGRESS,
  nomsId: String? = null,
  bookingNo: String? = null,
  bookingId: Long? = null,
  crn: String? = null,
  pnc: String? = null,
  cro: String? = null,
  prisonCode: String? = null,
  prisonDescription: String? = null,
  prisonTelephone: String? = null,
  forename: String? = null,
  middleNames: String? = null,
  surname: String? = null,
  dateOfBirth: LocalDate? = null,
  conditionalReleaseDate: LocalDate? = null,
  actualReleaseDate: LocalDate? = null,
  sentenceStartDate: LocalDate? = null,
  sentenceEndDate: LocalDate? = null,
  licenceStartDate: LocalDate? = null,
  licenceExpiryDate: LocalDate? = null,
  licenceActivatedDate: LocalDateTime? = null,
  override val homeDetentionCurfewActualDate: LocalDate? = null,
  val homeDetentionCurfewEndDate: LocalDate? = null,
  topupSupervisionStartDate: LocalDate? = null,
  topupSupervisionExpiryDate: LocalDate? = null,
  postRecallReleaseDate: LocalDate? = null,
  probationAreaCode: String? = null,
  probationAreaDescription: String? = null,
  probationPduCode: String? = null,
  probationPduDescription: String? = null,
  probationLauCode: String? = null,
  probationLauDescription: String? = null,
  probationTeamCode: String? = null,
  probationTeamDescription: String? = null,
  appointmentPersonType: AppointmentPersonType? = null,
  appointmentPerson: String? = null,
  appointmentTime: LocalDateTime? = null,
  appointmentTimeType: AppointmentTimeType? = null,
  appointmentAddress: String? = null,
  appointmentContact: String? = null,
  override var spoDiscussion: String? = null,
  override var vloDiscussion: String? = null,
  approvedDate: LocalDateTime? = null,
  approvedByUsername: String? = null,
  approvedByName: String? = null,
  supersededDate: LocalDateTime? = null,
  submittedDate: LocalDateTime? = null,
  dateCreated: LocalDateTime? = null,
  dateLastUpdated: LocalDateTime? = null,
  updatedByUsername: String? = null,
  standardConditions: List<StandardCondition> = emptyList(),
  additionalConditions: List<AdditionalCondition> = emptyList(),
  bespokeConditions: List<BespokeCondition> = emptyList(),
  responsibleCom: CommunityOffenderManager? = null,
  variationOfId: Long? = null,
  licenceVersion: String? = "1.0",
  updatedBy: Staff? = null,

  @OneToMany(mappedBy = "licence", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderBy("curfewTimesSequence")
  val curfewTimes: List<HdcCurfewTimes> = emptyList(),

  @OneToOne(mappedBy = "licence", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  val curfewAddress: HdcCurfewAddress? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "submitted_by_com_id", nullable = true)
  var submittedBy: CommunityOffenderManager? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_com_id", nullable = false)
  var createdBy: CommunityOffenderManager? = null,
) : Variation(
  id = id,
  kind = LicenceKind.HDC_VARIATION,
  typeCode = typeCode,
  version = version,
  statusCode = statusCode,
  nomsId = nomsId,
  bookingNo = bookingNo,
  bookingId = bookingId,
  crn = crn,
  pnc = pnc,
  cro = cro,
  prisonCode = prisonCode,
  prisonDescription = prisonDescription,
  prisonTelephone = prisonTelephone,
  forename = forename,
  middleNames = middleNames,
  surname = surname,
  dateOfBirth = dateOfBirth,
  conditionalReleaseDate = conditionalReleaseDate,
  actualReleaseDate = actualReleaseDate,
  sentenceStartDate = sentenceStartDate,
  sentenceEndDate = sentenceEndDate,
  licenceStartDate = licenceStartDate,
  licenceExpiryDate = licenceExpiryDate,
  licenceActivatedDate = licenceActivatedDate,
  topupSupervisionStartDate = topupSupervisionStartDate,
  topupSupervisionExpiryDate = topupSupervisionExpiryDate,
  postRecallReleaseDate = postRecallReleaseDate,
  probationAreaCode = probationAreaCode,
  probationAreaDescription = probationAreaDescription,
  probationPduCode = probationPduCode,
  probationPduDescription = probationPduDescription,
  probationLauCode = probationLauCode,
  probationLauDescription = probationLauDescription,
  probationTeamCode = probationTeamCode,
  probationTeamDescription = probationTeamDescription,
  appointmentPersonType = appointmentPersonType,
  appointmentPerson = appointmentPerson,
  appointmentTime = appointmentTime,
  appointmentTimeType = appointmentTimeType,
  appointmentAddress = appointmentAddress,
  appointmentContact = appointmentContact,
  approvedDate = approvedDate,
  approvedByUsername = approvedByUsername,
  approvedByName = approvedByName,
  supersededDate = supersededDate,
  submittedDate = submittedDate,
  dateCreated = dateCreated,
  dateLastUpdated = dateLastUpdated,
  updatedByUsername = updatedByUsername,
  licenceVersion = licenceVersion,
  standardConditions = standardConditions,
  additionalConditions = additionalConditions,
  bespokeConditions = bespokeConditions,
  responsibleCom = responsibleCom,
  updatedBy = updatedBy,
  spoDiscussion = spoDiscussion,
  vloDiscussion = vloDiscussion,
  variationOfId = variationOfId,
) {

  fun copy(
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
    homeDetentionCurfewActualDate: LocalDate? = this.homeDetentionCurfewActualDate,
    homeDetentionCurfewEndDate: LocalDate? = this.homeDetentionCurfewEndDate,
    topupSupervisionStartDate: LocalDate? = this.topupSupervisionStartDate,
    topupSupervisionExpiryDate: LocalDate? = this.topupSupervisionExpiryDate,
    postRecallReleaseDate: LocalDate? = this.postRecallReleaseDate,
    probationAreaCode: String? = this.probationAreaCode,
    probationAreaDescription: String? = this.probationAreaDescription,
    probationPduCode: String? = this.probationPduCode,
    probationPduDescription: String? = this.probationPduDescription,
    probationLauCode: String? = this.probationLauCode,
    probationLauDescription: String? = this.probationLauDescription,
    probationTeamCode: String? = this.probationTeamCode,
    probationTeamDescription: String? = this.probationTeamDescription,
    appointmentPersonType: AppointmentPersonType? = this.appointmentPersonType,
    appointmentPerson: String? = this.appointmentPerson,
    appointmentTime: LocalDateTime? = this.appointmentTime,
    appointmentTimeType: AppointmentTimeType? = this.appointmentTimeType,
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
    variationOfId: Long? = this.variationOfId,
    licenceVersion: String? = this.licenceVersion,
    standardConditions: List<StandardCondition> = this.standardConditions,
    additionalConditions: List<AdditionalCondition> = this.additionalConditions,
    bespokeConditions: List<BespokeCondition> = this.bespokeConditions,
    responsibleCom: CommunityOffenderManager? = this.responsibleCom,
    curfewTimes: List<HdcCurfewTimes> = this.curfewTimes,
    submittedBy: CommunityOffenderManager? = this.submittedBy,
    createdBy: CommunityOffenderManager? = this.createdBy,
    updatedBy: Staff? = this.updatedBy,
    curfewAddress: HdcCurfewAddress? = this.curfewAddress,
  ): HdcVariationLicence {
    val hdcVariationLicence: HdcVariationLicence = HdcVariationLicence(
      id = id,
      typeCode = typeCode,
      version = version,
      statusCode = statusCode,
      nomsId = nomsId,
      bookingNo = bookingNo,
      bookingId = bookingId,
      crn = crn,
      pnc = pnc,
      cro = cro,
      prisonCode = prisonCode,
      prisonDescription = prisonDescription,
      prisonTelephone = prisonTelephone,
      forename = forename,
      middleNames = middleNames,
      surname = surname,
      dateOfBirth = dateOfBirth,
      conditionalReleaseDate = conditionalReleaseDate,
      actualReleaseDate = actualReleaseDate,
      sentenceStartDate = sentenceStartDate,
      sentenceEndDate = sentenceEndDate,
      licenceStartDate = licenceStartDate,
      licenceExpiryDate = licenceExpiryDate,
      licenceActivatedDate = licenceActivatedDate,
      homeDetentionCurfewActualDate = homeDetentionCurfewActualDate,
      homeDetentionCurfewEndDate = homeDetentionCurfewEndDate,
      topupSupervisionStartDate = topupSupervisionStartDate,
      topupSupervisionExpiryDate = topupSupervisionExpiryDate,
      postRecallReleaseDate = postRecallReleaseDate,
      probationAreaCode = probationAreaCode,
      probationAreaDescription = probationAreaDescription,
      probationPduCode = probationPduCode,
      probationPduDescription = probationPduDescription,
      probationLauCode = probationLauCode,
      probationLauDescription = probationLauDescription,
      probationTeamCode = probationTeamCode,
      probationTeamDescription = probationTeamDescription,
      appointmentPersonType = appointmentPersonType,
      appointmentPerson = appointmentPerson,
      appointmentTime = appointmentTime,
      appointmentTimeType = appointmentTimeType,
      appointmentAddress = appointmentAddress,
      appointmentContact = appointmentContact,
      spoDiscussion = spoDiscussion,
      vloDiscussion = vloDiscussion,
      approvedDate = approvedDate,
      approvedByUsername = approvedByUsername,
      approvedByName = approvedByName,
      supersededDate = supersededDate,
      submittedDate = submittedDate,
      dateCreated = dateCreated,
      dateLastUpdated = dateLastUpdated,
      updatedByUsername = updatedByUsername,
      standardConditions = standardConditions,
      additionalConditions = additionalConditions,
      bespokeConditions = bespokeConditions,
      responsibleCom = responsibleCom,
      curfewTimes = curfewTimes,
      submittedBy = submittedBy,
      createdBy = createdBy,
      variationOfId = variationOfId,
      licenceVersion = licenceVersion,
      updatedBy = updatedBy,
      curfewAddress = curfewAddress,
    )
    return hdcVariationLicence
  }

  override fun activate(): Licence = copy(
    statusCode = LicenceStatus.ACTIVE,
    licenceActivatedDate = LocalDateTime.now(),
  )

  override fun deactivate() = copy(statusCode = LicenceStatus.INACTIVE)
  override fun deactivate(staffMember: Staff?) = copy(
    statusCode = LicenceStatus.INACTIVE,
    updatedByUsername = staffMember?.username ?: SYSTEM_USER,
    updatedBy = staffMember ?: this.updatedBy,
  )

  fun submit(submittedBy: CommunityOffenderManager) = copy(
    statusCode = LicenceStatus.VARIATION_SUBMITTED,
    submittedBy = submittedBy,
    updatedByUsername = submittedBy.username,
    submittedDate = LocalDateTime.now(),
    dateLastUpdated = LocalDateTime.now(),
    updatedBy = submittedBy,
  )

  override fun updatePrisonInfo(
    prisonCode: String,
    prisonDescription: String,
    prisonTelephone: String?,
    staffMember: Staff?,
  ) = copy(
    prisonCode = prisonCode,
    prisonDescription = prisonDescription,
    prisonTelephone = prisonTelephone,
    dateLastUpdated = LocalDateTime.now(),
    updatedByUsername = staffMember?.username ?: SYSTEM_USER,
    updatedBy = staffMember ?: this.updatedBy,
  )

  override fun updateStatus(
    statusCode: LicenceStatus,
    staffMember: Staff?,
    approvedByUsername: String?,
    approvedByName: String?,
    approvedDate: LocalDateTime?,
    supersededDate: LocalDateTime?,
    submittedDate: LocalDateTime?,
    licenceActivatedDate: LocalDateTime?,
  ) = copy(
    statusCode = statusCode,
    dateLastUpdated = LocalDateTime.now(),
    updatedByUsername = staffMember?.username ?: SYSTEM_USER,
    approvedByUsername = approvedByUsername,
    approvedByName = approvedByName,
    approvedDate = approvedDate,
    supersededDate = supersededDate,
    submittedDate = submittedDate,
    licenceActivatedDate = licenceActivatedDate,
    updatedBy = staffMember ?: this.updatedBy,
  )

  override fun overrideStatus(
    statusCode: LicenceStatus,
    staffMember: Staff?,
    licenceActivatedDate: LocalDateTime?,
  ) = copy(
    statusCode = statusCode,
    updatedByUsername = staffMember?.username ?: SYSTEM_USER,
    dateLastUpdated = LocalDateTime.now(),
    licenceActivatedDate = licenceActivatedDate,
    updatedBy = staffMember ?: this.updatedBy,
  )

  override fun updateConditions(
    updatedAdditionalConditions: List<AdditionalCondition>?,
    updatedStandardConditions: List<StandardCondition>?,
    updatedBespokeConditions: List<BespokeCondition>?,
    staffMember: Staff?,
  ) = copy(
    additionalConditions = updatedAdditionalConditions ?: additionalConditions,
    standardConditions = updatedStandardConditions ?: standardConditions,
    bespokeConditions = updatedBespokeConditions ?: bespokeConditions,
    dateLastUpdated = LocalDateTime.now(),
    updatedByUsername = staffMember?.username ?: SYSTEM_USER,
    updatedBy = staffMember ?: this.updatedBy,
  )

  override fun updateLicenceDates(
    status: LicenceStatus?,
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
  ) = copy(
    statusCode = status ?: this.statusCode,
    conditionalReleaseDate = conditionalReleaseDate,
    actualReleaseDate = actualReleaseDate,
    sentenceStartDate = sentenceStartDate,
    sentenceEndDate = sentenceEndDate,
    licenceStartDate = licenceStartDate,
    licenceExpiryDate = licenceExpiryDate,
    topupSupervisionStartDate = topupSupervisionStartDate,
    topupSupervisionExpiryDate = topupSupervisionExpiryDate,
    postRecallReleaseDate = postRecallReleaseDate,
    dateLastUpdated = LocalDateTime.now(),
    updatedByUsername = staffMember?.username ?: SYSTEM_USER,
    updatedBy = staffMember ?: this.updatedBy,
  )

  override fun updateOffenderDetails(
    forename: String?,
    middleNames: String?,
    surname: String?,
    dateOfBirth: LocalDate?,
  ) = copy(
    forename = forename,
    middleNames = middleNames,
    surname = surname,
    dateOfBirth = dateOfBirth,
  )

  override fun updateProbationTeam(
    probationAreaCode: String?,
    probationAreaDescription: String?,
    probationPduCode: String?,
    probationPduDescription: String?,
    probationLauCode: String?,
    probationLauDescription: String?,
    probationTeamCode: String?,
    probationTeamDescription: String?,
  ) = copy(
    probationAreaCode = probationAreaCode,
    probationAreaDescription = probationAreaDescription,
    probationPduCode = probationPduCode,
    probationPduDescription = probationPduDescription,
    probationLauCode = probationLauCode,
    probationLauDescription = probationLauDescription,
    probationTeamCode = probationTeamCode,
    probationTeamDescription = probationTeamDescription,
  )

  override fun updateResponsibleCom(responsibleCom: CommunityOffenderManager) = copy(
    responsibleCom = responsibleCom,
  )

  override fun getCreator() = createdBy ?: error("licence: $id has no COM/creator")

  override fun toString(): String = "HdcVariationLicence(" +
    "id=$id, " +
    "kind=$kind, " +
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
    "homeDetentionCurfewActualDate=$homeDetentionCurfewActualDate, " +
    "homeDetentionCurfewEndDate=$homeDetentionCurfewEndDate, " +
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
    "appointmentPersonType=$appointmentPersonType, " +
    "appointmentPerson=$appointmentPerson, " +
    "appointmentTime=$appointmentTime, " +
    "appointmentTimeType=$appointmentTimeType, " +
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
    "curfewTimes=$curfewTimes, " +
    "submittedBy=$submittedBy, " +
    "createdBy=$createdBy, " +
    "createdBy=$createdBy, " +
    "variationOfId=$variationOfId, " +
    "licenceVersion=$licenceVersion, " +
    "updatedBy=$updatedBy" +
    "curfewAddress=$curfewAddress" +
    ")"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is VariationLicence) return false
    if (!super.equals(other)) return false
    return true
  }

  override fun hashCode(): Int = super.hashCode()
}
