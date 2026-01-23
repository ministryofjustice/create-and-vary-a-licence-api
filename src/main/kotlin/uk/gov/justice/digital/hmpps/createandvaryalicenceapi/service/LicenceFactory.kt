package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.VariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.timeserved.TimeServedLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper.AppointmentMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.Prison
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.TeamDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.regex.Pattern

object LicenceFactory {

  fun createPrrd(
    licenceType: LicenceType,
    nomsId: String,
    version: String,
    nomisRecord: PrisonerSearchPrisoner,
    prisonInformation: Prison,
    team: TeamDetail,
    deliusRecord: ProbationCase,
    creator: CommunityOffenderManager,
    responsibleCom: CommunityOffenderManager,
    licenceStartDate: LocalDate?,
  ) = PrrdLicence(
    typeCode = licenceType,
    version = version,
    statusCode = IN_PROGRESS,
    nomsId = nomsId,
    bookingNo = nomisRecord.bookNumber,
    bookingId = nomisRecord.bookingId?.toLong(),
    crn = deliusRecord.crn,
    pnc = deliusRecord.pncNumber,
    cro = getCro(deliusRecord.croNumber, nomisRecord.croNumber),
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
    licenceStartDate = licenceStartDate,
    licenceExpiryDate = nomisRecord.licenceExpiryDate,
    topupSupervisionStartDate = nomisRecord.topupSupervisionStartDate,
    topupSupervisionExpiryDate = nomisRecord.topupSupervisionExpiryDate,
    postRecallReleaseDate = nomisRecord.postRecallReleaseDate!!,
    probationAreaCode = team.provider.code,
    probationAreaDescription = team.provider.description,
    probationPduCode = team.borough.code,
    probationPduDescription = team.borough.description,
    probationLauCode = team.district.code,
    probationLauDescription = team.district.description,
    probationTeamCode = team.code,
    probationTeamDescription = team.description,
    dateCreated = LocalDateTime.now(),
    responsibleCom = responsibleCom,
    createdBy = creator,
  )

  fun createCrd(
    licenceType: LicenceType,
    nomsId: String,
    version: String,
    nomisRecord: PrisonerSearchPrisoner,
    prisonInformation: Prison,
    team: TeamDetail,
    deliusRecord: ProbationCase,
    creator: CommunityOffenderManager,
    responsibleCom: CommunityOffenderManager,
    licenceStartDate: LocalDate?,
  ) = CrdLicence(
    typeCode = licenceType,
    version = version,
    statusCode = IN_PROGRESS,
    nomsId = nomsId,
    bookingNo = nomisRecord.bookNumber,
    bookingId = nomisRecord.bookingId?.toLong(),
    crn = deliusRecord.crn,
    pnc = deliusRecord.pncNumber,
    cro = getCro(deliusRecord.croNumber, nomisRecord.croNumber),
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
    licenceStartDate = licenceStartDate,
    licenceExpiryDate = nomisRecord.licenceExpiryDate,
    topupSupervisionStartDate = nomisRecord.topupSupervisionStartDate,
    topupSupervisionExpiryDate = nomisRecord.topupSupervisionExpiryDate,
    postRecallReleaseDate = nomisRecord.postRecallReleaseDate,
    probationAreaCode = team.provider.code,
    probationAreaDescription = team.provider.description,
    probationPduCode = team.borough.code,
    probationPduDescription = team.borough.description,
    probationLauCode = team.district.code,
    probationLauDescription = team.district.description,
    probationTeamCode = team.code,
    probationTeamDescription = team.description,
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
    team: TeamDetail,
    deliusRecord: ProbationCase,
    creator: PrisonUser,
    responsibleCom: CommunityOffenderManager,
    timedOutLicence: CrdLicence?,
    licenceStartDate: LocalDate?,
    eligibleKind: LicenceKind?,
  ) = HardStopLicence(
    typeCode = licenceType,
    version = version,
    statusCode = IN_PROGRESS,
    nomsId = nomsId,
    bookingNo = nomisRecord.bookNumber,
    bookingId = nomisRecord.bookingId?.toLong(),
    crn = deliusRecord.crn,
    pnc = deliusRecord.pncNumber,
    cro = getCro(deliusRecord.croNumber, nomisRecord.croNumber),
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
    licenceStartDate = licenceStartDate,
    licenceExpiryDate = nomisRecord.licenceExpiryDate,
    topupSupervisionStartDate = nomisRecord.topupSupervisionStartDate,
    topupSupervisionExpiryDate = nomisRecord.topupSupervisionExpiryDate,
    postRecallReleaseDate = nomisRecord.postRecallReleaseDate,
    probationAreaCode = team.provider.code,
    probationAreaDescription = team.provider.description,
    probationPduCode = team.borough.code,
    probationPduDescription = team.borough.description,
    probationLauCode = team.district.code,
    probationLauDescription = team.district.description,
    probationTeamCode = team.code,
    probationTeamDescription = team.description,
    dateCreated = LocalDateTime.now(),
    responsibleCom = responsibleCom,
    createdBy = creator,
    substituteOfId = timedOutLicence?.id,
    eligibleKind = eligibleKind,
  )

  fun createTimeServe(
    licenceType: LicenceType,
    eligibleKind: LicenceKind?,
    nomsId: String,
    version: String,
    nomisRecord: PrisonerSearchPrisoner,
    prisonInformation: Prison,
    team: TeamDetail?,
    deliusRecord: ProbationCase,
    responsibleCom: CommunityOffenderManager?,
    creator: PrisonUser,
    licenceStartDate: LocalDate?,
  ) = TimeServedLicence(
    typeCode = licenceType,
    eligibleKind = eligibleKind,
    version = version,
    statusCode = IN_PROGRESS,
    nomsId = nomsId,
    bookingNo = nomisRecord.bookNumber,
    bookingId = nomisRecord.bookingId?.toLong(),
    crn = deliusRecord.crn,
    pnc = deliusRecord.pncNumber,
    cro = getCro(deliusRecord.croNumber, nomisRecord.croNumber),
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
    licenceStartDate = licenceStartDate,
    licenceExpiryDate = nomisRecord.licenceExpiryDate,
    topupSupervisionStartDate = nomisRecord.topupSupervisionStartDate,
    topupSupervisionExpiryDate = nomisRecord.topupSupervisionExpiryDate,
    postRecallReleaseDate = nomisRecord.postRecallReleaseDate,
    probationAreaCode = team?.provider?.code,
    probationAreaDescription = team?.provider?.description,
    probationPduCode = team?.borough?.code,
    probationPduDescription = team?.borough?.description,
    probationLauCode = team?.district?.code,
    probationLauDescription = team?.district?.description,
    probationTeamCode = team?.code,
    probationTeamDescription = team?.description,
    dateCreated = LocalDateTime.now(),
    responsibleCom = responsibleCom,
    createdBy = creator,
  )

  fun createCrdCopyToEdit(licence: CrdLicence, creator: CommunityOffenderManager): Licence {
    with(licence) {
      return licence.copy(
        id = null,
        dateCreated = LocalDateTime.now(),
        statusCode = IN_PROGRESS,
        licenceVersion = getNextLicenceVersion(this.licenceVersion!!),
        versionOfId = licence.id,
        createdBy = creator,
      )
    }
  }

  fun createPrrdCopyToEdit(licence: PrrdLicence, creator: CommunityOffenderManager): Licence {
    with(licence) {
      return licence.copy(
        id = null,
        dateCreated = LocalDateTime.now(),
        statusCode = IN_PROGRESS,
        licenceVersion = getNextLicenceVersion(this.licenceVersion!!),
        versionOfId = licence.id,
        createdBy = creator,
      )
    }
  }

  fun createHdcCopyToEdit(licence: HdcLicence, creator: CommunityOffenderManager): Licence {
    with(licence) {
      return licence.copy(
        id = null,
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
        typeCode = this.typeCode,
        eligibleKind = this.eligibleKind,
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
        postRecallReleaseDate = this.postRecallReleaseDate,
        probationAreaCode = this.probationAreaCode,
        probationAreaDescription = this.probationAreaDescription,
        probationPduCode = this.probationPduCode,
        probationPduDescription = this.probationPduDescription,
        probationLauCode = this.probationLauCode,
        probationLauDescription = this.probationLauDescription,
        probationTeamCode = this.probationTeamCode,
        probationTeamDescription = this.probationTeamDescription,
        appointment = AppointmentMapper.copy(this.appointment),
        responsibleCom = this.responsibleCom,
        dateCreated = LocalDateTime.now(),
        licenceVersion = getVariationVersion(this.licenceVersion!!),
      )
    }
  }

  fun createHdc(
    licenceType: LicenceType,
    nomsId: String,
    version: String,
    nomisRecord: PrisonerSearchPrisoner,
    prisonInformation: Prison,
    team: TeamDetail,
    deliusRecord: ProbationCase,
    creator: CommunityOffenderManager,
    responsibleCom: CommunityOffenderManager,
    licenceStartDate: LocalDate?,
  ) = HdcLicence(
    typeCode = licenceType,
    version = version,
    statusCode = IN_PROGRESS,
    nomsId = nomsId,
    bookingNo = nomisRecord.bookNumber,
    bookingId = nomisRecord.bookingId?.toLong(),
    crn = deliusRecord.crn,
    pnc = deliusRecord.pncNumber,
    cro = getCro(deliusRecord.croNumber, nomisRecord.croNumber),
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
    licenceStartDate = licenceStartDate,
    licenceExpiryDate = nomisRecord.licenceExpiryDate,
    homeDetentionCurfewActualDate = nomisRecord.homeDetentionCurfewActualDate,
    homeDetentionCurfewEndDate = nomisRecord.homeDetentionCurfewEndDate,
    topupSupervisionStartDate = nomisRecord.topupSupervisionStartDate,
    topupSupervisionExpiryDate = nomisRecord.topupSupervisionExpiryDate,
    postRecallReleaseDate = nomisRecord.postRecallReleaseDate,
    probationAreaCode = team.provider.code,
    probationAreaDescription = team.provider.description,
    probationPduCode = team.borough.code,
    probationPduDescription = team.borough.description,
    probationLauCode = team.district.code,
    probationLauDescription = team.district.description,
    probationTeamCode = team.code,
    probationTeamDescription = team.description,
    dateCreated = LocalDateTime.now(),
    responsibleCom = responsibleCom,
    createdBy = creator,
  )

  fun createHdcVariation(licence: HdcLicence, creator: CommunityOffenderManager): HdcVariationLicence {
    with(licence) {
      return HdcVariationLicence(
        typeCode = this.typeCode,
        version = this.version,
        statusCode = VARIATION_IN_PROGRESS,
        variationOfId = this.id,
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
        postRecallReleaseDate = this.postRecallReleaseDate,
        homeDetentionCurfewActualDate = this.homeDetentionCurfewActualDate,
        homeDetentionCurfewEndDate = this.homeDetentionCurfewEndDate,
        probationAreaCode = this.probationAreaCode,
        probationAreaDescription = this.probationAreaDescription,
        probationPduCode = this.probationPduCode,
        probationPduDescription = this.probationPduDescription,
        probationLauCode = this.probationLauCode,
        probationLauDescription = this.probationLauDescription,
        probationTeamCode = this.probationTeamCode,
        probationTeamDescription = this.probationTeamDescription,
        appointment = AppointmentMapper.copy(this.appointment),
        responsibleCom = this.getCom(),
        dateCreated = LocalDateTime.now(),
        licenceVersion = getVariationVersion(this.licenceVersion!!),
      )
    }
  }

  /*
   * Check if a string contains a valid criminal records office number. See
   * https://assets.publishing.service.gov.uk/government/uploads/system/uploads/attachment_data/file/862971/cjs-data-standards-catalogue-6.pdf
   */
  fun isValidCro(cro: String?): Boolean {
    if (cro.isNullOrBlank()) {
      return false
    }

    val croPattern =
      Pattern.compile("^(([1-9]\\d{0,5}/\\d{2})|(SF(39|[4-8][0-9]|9[0-5])/[1-9]\\d{0,5}))[^IOS0-9]$")
    return croPattern.matcher(cro).matches()
  }

  private fun getCro(deliusRecordCro: String?, nomisRecordCro: String?): String? {
    val deliusCro = deliusRecordCro?.trim()
    val nomisCro = nomisRecordCro?.trim()
    return when {
      (deliusCro != null && isValidCro(deliusCro)) -> deliusCro
      (nomisCro != null && isValidCro(nomisCro)) -> nomisCro
      else -> null
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
