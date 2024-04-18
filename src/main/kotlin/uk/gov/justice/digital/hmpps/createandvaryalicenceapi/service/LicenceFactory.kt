package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.VariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.Prison
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityOrPrisonOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OffenderDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDateTime

object LicenceFactory {

  fun createCrd(
    licenceType: LicenceType,
    nomsId: String,
    version: String,
    nomisRecord: PrisonerSearchPrisoner,
    prisonInformation: Prison,
    currentResponsibleOfficerDetails: CommunityOrPrisonOffenderManager,
    deliusRecord: OffenderDetail,
    creator: CommunityOffenderManager,
    responsibleCom: CommunityOffenderManager,
  ) = CrdLicence(
    typeCode = licenceType,
    version = version,
    statusCode = IN_PROGRESS,
    nomsId = nomsId,
    bookingNo = nomisRecord.bookNumber,
    bookingId = nomisRecord.bookingId?.toLong(),
    crn = deliusRecord.otherIds.crn,
    pnc = deliusRecord.otherIds.pncNumber,
    cro = deliusRecord.otherIds.croNumber ?: nomisRecord.croNumber,
    prisonCode = nomisRecord.prisonId,
    prisonDescription = prisonInformation.description,
    prisonTelephone = prisonInformation.getPrisonContactNumber(),
    forename = nomisRecord.firstName.convertToTitleCase(),
    middleNames = nomisRecord.middleNames?.convertToTitleCase() ?: "",
    surname = nomisRecord.lastName.convertToTitleCase(),
    dateOfBirth = nomisRecord.dateOfBirth,
    conditionalReleaseDate = nomisRecord.conditionalReleaseDateOverrideDate ?: nomisRecord.conditionalReleaseDate,
    actualReleaseDate = nomisRecord.confirmedReleaseDate,
    sentenceStartDate = nomisRecord.sentenceStartDate,
    sentenceEndDate = nomisRecord.sentenceExpiryDate,
    licenceStartDate = nomisRecord.confirmedReleaseDate ?: nomisRecord.conditionalReleaseDate,
    licenceExpiryDate = nomisRecord.licenceExpiryDate,
    topupSupervisionStartDate = nomisRecord.topupSupervisionStartDate,
    topupSupervisionExpiryDate = nomisRecord.topupSupervisionExpiryDate,
    probationAreaCode = currentResponsibleOfficerDetails.probationArea.code,
    probationAreaDescription = currentResponsibleOfficerDetails.probationArea.description,
    probationPduCode = currentResponsibleOfficerDetails.team.borough.code,
    probationPduDescription = currentResponsibleOfficerDetails.team.borough.description,
    probationLauCode = currentResponsibleOfficerDetails.team.district.code,
    probationLauDescription = currentResponsibleOfficerDetails.team.district.description,
    probationTeamCode = currentResponsibleOfficerDetails.team.code,
    probationTeamDescription = currentResponsibleOfficerDetails.team.description,
    dateCreated = LocalDateTime.now(),
    responsibleCom = responsibleCom,
    createdBy = creator,
  )

  fun createHardStop(
    licenceType: LicenceType,
    nomsId: String,
    version: String,
    nomisRecord: PrisonerSearchPrisoner,
    prisonInformation: Prison,
    currentResponsibleOfficerDetails: CommunityOrPrisonOffenderManager,
    deliusRecord: OffenderDetail,
    creator: PrisonUser,
    responsibleCom: CommunityOffenderManager,
    timedOutLicence: CrdLicence?,
  ) = HardStopLicence(
    typeCode = licenceType,
    version = version,
    statusCode = IN_PROGRESS,
    nomsId = nomsId,
    bookingNo = nomisRecord.bookNumber,
    bookingId = nomisRecord.bookingId?.toLong(),
    crn = deliusRecord.otherIds.crn,
    pnc = deliusRecord.otherIds.pncNumber,
    cro = deliusRecord.otherIds.croNumber ?: nomisRecord.croNumber,
    prisonCode = nomisRecord.prisonId,
    prisonDescription = prisonInformation.description,
    prisonTelephone = prisonInformation.getPrisonContactNumber(),
    forename = nomisRecord.firstName.convertToTitleCase(),
    middleNames = nomisRecord.middleNames?.convertToTitleCase() ?: "",
    surname = nomisRecord.lastName.convertToTitleCase(),
    dateOfBirth = nomisRecord.dateOfBirth,
    conditionalReleaseDate = nomisRecord.conditionalReleaseDateOverrideDate ?: nomisRecord.conditionalReleaseDate,
    actualReleaseDate = nomisRecord.confirmedReleaseDate,
    sentenceStartDate = nomisRecord.sentenceStartDate,
    sentenceEndDate = nomisRecord.sentenceExpiryDate,
    licenceStartDate = nomisRecord.confirmedReleaseDate ?: nomisRecord.conditionalReleaseDate,
    licenceExpiryDate = nomisRecord.licenceExpiryDate,
    topupSupervisionStartDate = nomisRecord.topupSupervisionStartDate,
    topupSupervisionExpiryDate = nomisRecord.topupSupervisionExpiryDate,
    probationAreaCode = currentResponsibleOfficerDetails.probationArea.code,
    probationAreaDescription = currentResponsibleOfficerDetails.probationArea.description,
    probationPduCode = currentResponsibleOfficerDetails.team.borough.code,
    probationPduDescription = currentResponsibleOfficerDetails.team.borough.description,
    probationLauCode = currentResponsibleOfficerDetails.team.district.code,
    probationLauDescription = currentResponsibleOfficerDetails.team.district.description,
    probationTeamCode = currentResponsibleOfficerDetails.team.code,
    probationTeamDescription = currentResponsibleOfficerDetails.team.description,
    dateCreated = LocalDateTime.now(),
    responsibleCom = responsibleCom,
    createdBy = creator,
    substituteOfId = timedOutLicence?.id,
  )

  fun createCopyToEdit(licence: CrdLicence, creator: CommunityOffenderManager): Licence {
    with(licence) {
      return licence.copy(
        id = -1,
        dateCreated = LocalDateTime.now(),
        statusCode = IN_PROGRESS,
        licenceVersion = getNextLicenceVersion(this.licenceVersion!!),
        versionOfId = licence.id,
        createdBy = creator,
      )
    }
  }

  fun createVariation(licence: Licence, creator: CommunityOffenderManager): Licence {
    with(licence) {
      return VariationLicence(
        id = -1,
        typeCode = this.typeCode,
        version = this.version,
        statusCode = VARIATION_IN_PROGRESS,
        variationOfId = licence.id,
        createdBy = creator,
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
        appointmentPersonType = this.appointmentPersonType,
        appointmentPerson = this.appointmentPerson,
        appointmentTime = this.appointmentTime,
        appointmentTimeType = this.appointmentTimeType,
        appointmentAddress = this.appointmentAddress,
        appointmentContact = this.appointmentContact,
        responsibleCom = this.responsibleCom,
        dateCreated = LocalDateTime.now(),
        licenceVersion = getVariationVersion(this.licenceVersion!!),
      )
    }
  }

  private fun getNextLicenceVersion(currentVersion: String): String {
    val (majorVersion, minorVersion) = getVersionParts(currentVersion)
    return "$majorVersion.${minorVersion + 1}"
  }

  private fun getVariationVersion(currentVersion: String): String {
    val (majorVersion) = getVersionParts(currentVersion)
    return "${majorVersion + 1}.0"
  }

  private fun getVersionParts(version: String): Pair<Int, Int> {
    val parts = version.split(".")
    return parts[0].toInt() to parts[1].toInt()
  }
}
