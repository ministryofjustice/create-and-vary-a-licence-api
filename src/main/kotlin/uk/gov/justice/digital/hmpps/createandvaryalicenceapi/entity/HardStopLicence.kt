package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@DiscriminatorValue(value = "HARDSTOP")
class HardStopLicence(
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
  probationAreaCode: String? = null,
  probationAreaDescription: String? = null,
  probationPduCode: String? = null,
  probationPduDescription: String? = null,
  probationLauCode: String? = null,
  probationLauDescription: String? = null,
  probationTeamCode: String? = null,
  probationTeamDescription: String? = null,
  appointmentPerson: String? = null,
  appointmentTime: LocalDateTime? = null,
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
  licenceVersion: String? = "1.0",
  standardConditions: List<StandardCondition> = emptyList(),
  additionalConditions: List<AdditionalCondition> = emptyList(),
  bespokeConditions: List<BespokeCondition> = emptyList(),
  responsibleCom: CommunityOffenderManager? = null,

  var reviewDate: LocalDate? = null,
  var substituteOfId: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_ca_id", nullable = false)
  var createdBy: PrisonCaseAdministrator? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "submitted_by_ca_id", nullable = true)
  var submittedBy: PrisonCaseAdministrator? = null,
) : Licence(
  id = id,
  kind = LicenceKind.HARDSTOP,
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
  probationAreaCode = probationAreaCode,
  probationAreaDescription = probationAreaDescription,
  probationPduCode = probationPduCode,
  probationPduDescription = probationPduDescription,
  probationLauCode = probationLauCode,
  probationLauDescription = probationLauDescription,
  probationTeamCode = probationTeamCode,
  probationTeamDescription = probationTeamDescription,
  appointmentPerson = appointmentPerson,
  appointmentTime = appointmentTime,
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
    submittedBy: PrisonCaseAdministrator? = this.submittedBy,
    createdBy: PrisonCaseAdministrator? = this.createdBy,
    substituteOfId: Long? = this.substituteOfId,
    reviewDate: LocalDate? = this.reviewDate,
    licenceVersion: String? = this.licenceVersion,
  ): HardStopLicence {
    return HardStopLicence(
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
      probationAreaCode = probationAreaCode,
      probationAreaDescription = probationAreaDescription,
      probationPduCode = probationPduCode,
      probationPduDescription = probationPduDescription,
      probationLauCode = probationLauCode,
      probationLauDescription = probationLauDescription,
      probationTeamCode = probationTeamCode,
      probationTeamDescription = probationTeamDescription,
      appointmentPerson = appointmentPerson,
      appointmentTime = appointmentTime,
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
      substituteOfId = substituteOfId,
      reviewDate = reviewDate,
      licenceVersion = licenceVersion,
    )
  }

  override fun activate() = copy(
    statusCode = LicenceStatus.ACTIVE,
    licenceActivatedDate = LocalDateTime.now(),
  )

  override fun deactivate() = copy(statusCode = LicenceStatus.INACTIVE)

  fun submit(submittedBy: PrisonCaseAdministrator) = copy(
    statusCode = LicenceStatus.SUBMITTED,
    submittedBy = submittedBy,
    updatedByUsername = submittedBy.username,
    submittedDate = LocalDateTime.now(),
    dateLastUpdated = LocalDateTime.now(),
  )

  override fun updatePrisonInfo(
    prisonCode: String,
    prisonDescription: String,
    prisonTelephone: String?,
    updatedByUsername: String?,
  ) = copy(
    prisonCode = prisonCode,
    prisonDescription = prisonDescription,
    prisonTelephone = prisonTelephone,
    dateLastUpdated = LocalDateTime.now(),
    updatedByUsername = updatedByUsername,
  )

  override fun updateAppointmentAddress(appointmentAddress: String?, updatedByUsername: String?) = copy(
    appointmentAddress = appointmentAddress,
    dateLastUpdated = LocalDateTime.now(),
    updatedByUsername = updatedByUsername,
  )

  override fun updateAppointmentContactNumber(appointmentContact: String?, updatedByUsername: String?) = copy(
    appointmentContact = appointmentContact,
    dateLastUpdated = LocalDateTime.now(),
    updatedByUsername = updatedByUsername,
  )

  override fun updateAppointmentPerson(appointmentPerson: String?, updatedByUsername: String?) = copy(
    appointmentPerson = appointmentPerson,
    dateLastUpdated = LocalDateTime.now(),
    updatedByUsername = updatedByUsername,
  )

  override fun updateAppointmentTime(appointmentTime: LocalDateTime, updatedByUsername: String?) = copy(
    appointmentTime = appointmentTime,
    dateLastUpdated = LocalDateTime.now(),
    updatedByUsername = updatedByUsername,
  )

  override fun updateStatus(
    statusCode: LicenceStatus,
    updatedByUsername: String,
    approvedByUsername: String?,
    approvedByName: String?,
    approvedDate: LocalDateTime?,
    supersededDate: LocalDateTime?,
    submittedDate: LocalDateTime?,
    licenceActivatedDate: LocalDateTime?,
  ) = copy(
    statusCode = statusCode,
    dateLastUpdated = LocalDateTime.now(),
    updatedByUsername = updatedByUsername,
    approvedByUsername = approvedByUsername,
    approvedByName = approvedByName,
    approvedDate = approvedDate,
    supersededDate = supersededDate,
    submittedDate = submittedDate,
    licenceActivatedDate = licenceActivatedDate,
  )

  override fun overrideStatus(
    statusCode: LicenceStatus,
    updatedByUsername: String?,
    licenceActivatedDate: LocalDateTime?,
  ) = copy(
    statusCode = statusCode,
    updatedByUsername = updatedByUsername,
    dateLastUpdated = LocalDateTime.now(),
    licenceActivatedDate = licenceActivatedDate,
  )

  override fun updateConditions(
    updatedAdditionalConditions: List<AdditionalCondition>?,
    updatedStandardConditions: List<StandardCondition>?,
    updatedBespokeConditions: List<BespokeCondition>?,
    updatedByUsername: String?,
  ) = copy(
    additionalConditions = updatedAdditionalConditions ?: additionalConditions,
    standardConditions = updatedStandardConditions ?: standardConditions,
    bespokeConditions = updatedBespokeConditions ?: bespokeConditions,
    dateLastUpdated = LocalDateTime.now(),
    updatedByUsername = updatedByUsername,
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
    updatedByUsername: String?,
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
    dateLastUpdated = LocalDateTime.now(),
    updatedByUsername = updatedByUsername,
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

  override fun toString(): String {
    return "HardStopLicence(" +
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
      "appointmentPerson=$appointmentPerson, " +
      "appointmentTime=$appointmentTime, " +
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
      "substituteOfId=$substituteOfId, " +
      "reviewDate=$reviewDate, " +
      "licenceVersion=$licenceVersion" +
      ")"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is HardStopLicence) return false
    if (!super.equals(other)) return false
    return true
  }

  override fun hashCode(): Int {
    return super.hashCode()
  }
}
