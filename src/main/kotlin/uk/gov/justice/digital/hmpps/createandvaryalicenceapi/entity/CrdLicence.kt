package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@DiscriminatorValue(value = "CRD")
class CrdLicence(
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
  spoDiscussion: String? = null,
  vloDiscussion: String? = null,
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
  submittedBy: CommunityOffenderManager? = null,
  createdBy: CommunityOffenderManager? = null,
  variationOfId: Long? = null,
  versionOfId: Long? = null,
  licenceVersion: String? = "1.0",
) : Licence(
  id,
  LicenceKind.CRD,
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
) {

  override fun copy(
    id: Long,
    typeCode: LicenceType,
    version: String?,
    statusCode: LicenceStatus,
    nomsId: String?,
    bookingNo: String?,
    bookingId: Long?,
    crn: String?,
    pnc: String?,
    cro: String?,
    prisonCode: String?,
    prisonDescription: String?,
    prisonTelephone: String?,
    forename: String?,
    middleNames: String?,
    surname: String?,
    dateOfBirth: LocalDate?,
    conditionalReleaseDate: LocalDate?,
    actualReleaseDate: LocalDate?,
    sentenceStartDate: LocalDate?,
    sentenceEndDate: LocalDate?,
    licenceStartDate: LocalDate?,
    licenceExpiryDate: LocalDate?,
    licenceActivatedDate: LocalDateTime?,
    topupSupervisionStartDate: LocalDate?,
    topupSupervisionExpiryDate: LocalDate?,
    probationAreaCode: String?,
    probationAreaDescription: String?,
    probationPduCode: String?,
    probationPduDescription: String?,
    probationLauCode: String?,
    probationLauDescription: String?,
    probationTeamCode: String?,
    probationTeamDescription: String?,
    appointmentPerson: String?,
    appointmentTime: LocalDateTime?,
    appointmentAddress: String?,
    appointmentContact: String?,
    spoDiscussion: String?,
    vloDiscussion: String?,
    approvedDate: LocalDateTime?,
    approvedByUsername: String?,
    approvedByName: String?,
    supersededDate: LocalDateTime?,
    submittedDate: LocalDateTime?,
    dateCreated: LocalDateTime?,
    dateLastUpdated: LocalDateTime?,
    updatedByUsername: String?,
    standardConditions: List<StandardCondition>,
    additionalConditions: List<AdditionalCondition>,
    bespokeConditions: List<BespokeCondition>,
    responsibleCom: CommunityOffenderManager?,
    submittedBy: CommunityOffenderManager?,
    createdBy: CommunityOffenderManager?,
    variationOfId: Long?,
    versionOfId: Long?,
    licenceVersion: String?,
  ): CrdLicence {
    return CrdLicence(
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
    return "CrdLicence(" +
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
      "createdBy=$createdBy, " +
      "variationOfId=$variationOfId, " +
      "versionOfId=$versionOfId, " +
      "licenceVersion=$licenceVersion" +
      ")"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CrdLicence) return false
    if (!super.equals(other)) return false
    return true
  }

  override fun hashCode(): Int {
    return super.hashCode()
  }
}
