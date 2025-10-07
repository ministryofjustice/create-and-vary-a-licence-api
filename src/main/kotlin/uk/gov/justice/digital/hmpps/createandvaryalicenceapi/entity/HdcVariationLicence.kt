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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper.AppointmentMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@DiscriminatorValue(value = "HDC_VARIATION")
class HdcVariationLicence(
  id: Long? = null,
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
  appointment: Appointment? = null,
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
  override var variationOfId: Long? = null,
  licenceVersion: String? = "1.0",
  updatedBy: Staff? = null,

  @OneToMany(mappedBy = "licence", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderBy("curfewTimesSequence")
  override var curfewTimes: MutableList<HdcCurfewTimes> = mutableListOf(),

  @OneToOne(mappedBy = "licence", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  override val curfewAddress: HdcCurfewAddress? = null,

  @ManyToOne(cascade = [CascadeType.PERSIST,CascadeType.REFRESH,CascadeType.MERGE],fetch = FetchType.LAZY)
  @JoinColumn(name = "submitted_by_com_id", nullable = true)
  var submittedBy: CommunityOffenderManager? = null,

  @ManyToOne(cascade = [CascadeType.PERSIST,CascadeType.REFRESH,CascadeType.MERGE],fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_com_id", nullable = false)
  override var createdBy: CommunityOffenderManager? = null,

  responsibleCom: CommunityOffenderManager,
) : Licence(
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
  appointment = appointment,
  approvedDate = approvedDate,
  approvedByUsername = approvedByUsername,
  approvedByName = approvedByName,
  supersededDate = supersededDate,
  submittedDate = submittedDate,
  dateCreated = dateCreated,
  dateLastUpdated = dateLastUpdated,
  updatedByUsername = updatedByUsername,
  licenceVersion = licenceVersion,
  standardConditions = standardConditions.toMutableList(),
  additionalConditions = additionalConditions.toMutableList(),
  bespokeConditions = bespokeConditions.toMutableList(),
  updatedBy = updatedBy,
  responsibleCom = responsibleCom,
),
  Variation,
  HdcCase,
  AlwaysHasCom {

  fun copy(
    id: Long? = this.id,
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
    appointment: Appointment? = AppointmentMapper.copy(this.appointment),
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
    responsibleCom: CommunityOffenderManager = this.getCom(),
    curfewTimes: MutableList<HdcCurfewTimes> = this.curfewTimes.toMutableList(),
    submittedBy: CommunityOffenderManager? = this.submittedBy,
    createdBy: CommunityOffenderManager? = this.createdBy,
    updatedBy: Staff? = this.updatedBy,
    curfewAddress: HdcCurfewAddress? = this.curfewAddress,
  ): HdcVariationLicence {
    val hdcVariationLicence = HdcVariationLicence(
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
      appointment = appointment,
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

  fun submit(communityOffenderManager: CommunityOffenderManager) {
    statusCode = LicenceStatus.VARIATION_SUBMITTED
    submittedBy = communityOffenderManager
    updatedByUsername = communityOffenderManager.username
    submittedDate = LocalDateTime.now()
    dateLastUpdated = LocalDateTime.now()
    updatedBy = submittedBy
  }

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
    "appointmentPersonType=${appointment?.personType}, " +
    "appointmentPerson=${appointment?.person}, " +
    "appointmentTime=${appointment?.time}, " +
    "appointmentTimeType=${appointment?.timeType}, " +
    "appointmentAddress=${appointment?.addressText}, " +
    "licenceAppointmentAddress=${appointment?.address}, " +
    "appointmentContact=${appointment?.telephoneContactNumber}, " +
    "alternativeTelephoneContactNumber=${appointment?.alternativeTelephoneContactNumber}, " +
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

  override fun getCom(): CommunityOffenderManager = this.responsibleCom!!

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is VariationLicence) return false
    if (!super.equals(other)) return false
    return true
  }

  override fun hashCode(): Int = super.hashCode()
}
