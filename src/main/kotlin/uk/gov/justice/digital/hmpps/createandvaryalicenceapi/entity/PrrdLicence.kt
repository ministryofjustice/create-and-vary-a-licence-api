package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HasElectronicMonitoringResponseProvider
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentPersonType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@DiscriminatorValue(value = "PRRD")
class PrrdLicence(
  id: Long = -1L,
  typeCode: LicenceType,
  version: String? = null,
  statusCode: LicenceStatus = LicenceStatus.IN_PROGRESS,
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
  topupSupervisionStartDate: LocalDate? = null,
  topupSupervisionExpiryDate: LocalDate? = null,
  postRecallReleaseDate: LocalDate,
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
  approvedDate: LocalDateTime? = null,
  approvedByUsername: String? = null,
  approvedByName: String? = null,
  supersededDate: LocalDateTime? = null,
  submittedDate: LocalDateTime? = null,
  dateCreated: LocalDateTime? = null,
  dateLastUpdated: LocalDateTime? = null,
  updatedByUsername: String? = null,
  val versionOfId: Long? = null,
  licenceVersion: String? = "1.0",
  standardConditions: List<StandardCondition> = emptyList(),
  additionalConditions: List<AdditionalCondition> = emptyList(),
  bespokeConditions: List<BespokeCondition> = emptyList(),
  responsibleCom: CommunityOffenderManager,
  updatedBy: Staff? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "submitted_by_com_id", nullable = true)
  var submittedBy: CommunityOffenderManager? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_com_id", nullable = false)
  var createdBy: CommunityOffenderManager? = null,

  @OneToOne(mappedBy = "licence", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, optional = true, orphanRemoval = true)
  override var electronicMonitoringProvider: ElectronicMonitoringProvider? = null,
) : Licence(
  id = id,
  kind = LicenceKind.PRRD,
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
  standardConditions = standardConditions.toMutableList(),
  additionalConditions = additionalConditions.toMutableList(),
  bespokeConditions = bespokeConditions.toMutableList(),
  responsibleCom = responsibleCom,
  updatedBy = updatedBy,
),
  HasElectronicMonitoringResponseProvider {

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
    topupSupervisionStartDate: LocalDate? = this.topupSupervisionStartDate,
    topupSupervisionExpiryDate: LocalDate? = this.topupSupervisionExpiryDate,
    postRecallReleaseDate: LocalDate = this.postRecallReleaseDate!!,
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
    responsibleCom: CommunityOffenderManager = this.responsibleCom,
    submittedBy: CommunityOffenderManager? = this.submittedBy,
    createdBy: CommunityOffenderManager? = this.createdBy,
    versionOfId: Long? = this.versionOfId,
    licenceVersion: String? = this.licenceVersion,
    updatedBy: Staff? = this.updatedBy,
    electronicMonitoringProvider: ElectronicMonitoringProvider? = this.electronicMonitoringProvider,
  ): PrrdLicence = PrrdLicence(
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
    standardConditions = standardConditions,
    additionalConditions = additionalConditions,
    bespokeConditions = bespokeConditions,
    responsibleCom = responsibleCom,
    submittedBy = submittedBy,
    createdBy = createdBy,
    versionOfId = versionOfId,
    licenceVersion = licenceVersion,
    updatedBy = updatedBy,
    electronicMonitoringProvider = electronicMonitoringProvider,
  )

  fun timeOut() = copy(
    statusCode = LicenceStatus.TIMED_OUT,
    dateLastUpdated = LocalDateTime.now(),
    updatedByUsername = "SYSTEM",
  )

  fun submit(submittedBy: CommunityOffenderManager) = copy(
    statusCode = LicenceStatus.SUBMITTED,
    submittedBy = submittedBy,
    updatedByUsername = submittedBy.username,
    submittedDate = LocalDateTime.now(),
    dateLastUpdated = LocalDateTime.now(),
    updatedBy = submittedBy,
  )

  override fun getCreator() = createdBy ?: error("licence: $id has no COM/creator")

  override fun toString(): String = "PrrdLicence(" +
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
    "createdBy=$createdBy, " +
    "versionOfId=$versionOfId, " +
    "licenceVersion=$licenceVersion, " +
    "updatedBy=$updatedBy, " +
    "electronicMonitoringProvider=$electronicMonitoringProvider" +
    ")"

  override fun createNewElectronicMonitoringProvider(): ElectronicMonitoringProvider = ElectronicMonitoringProvider(
    licence = this,
    isToBeTaggedForProgramme = null,
    programmeName = null,
  )

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is PrrdLicence) return false
    if (!super.equals(other)) return false
    return true
  }

  override fun hashCode(): Int = super.hashCode()
}
