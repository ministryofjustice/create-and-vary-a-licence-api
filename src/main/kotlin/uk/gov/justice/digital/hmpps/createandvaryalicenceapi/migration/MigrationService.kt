package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Appointment
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcCurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Staff
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.Address
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.AddressSource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.repository.MigrationRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateAppointmentAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateCurfewTime
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateFirstNight
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateFromHdcToCvlRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecordService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceCreationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDateTime
import java.util.UUID

@Service
class MigrationService(
  val staffRepository: StaffRepository,
  val deliusApiClient: DeliusApiClient,
  val licenceCreationService: LicenceCreationService,
  val licenceRepository: LicenceRepository,
  val migrationRepository: MigrationRepository,
  val cvlRecordService: CvlRecordService,
  val prisonerSearchApiClient: PrisonerSearchApiClient,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Transactional
  fun migrate(request: MigrateFromHdcToCvlRequest) {
    if (isNotValid(request)) return

    log.info("Starting migration for bookingId={}", request.bookingId)
    val hdcLicence = request.toHdcLicence()
    saveMetaData(request, hdcLicence)
    log.info("Ending migration for bookingId={} cvl licence id ={}", request.bookingId, hdcLicence.id)
  }

  private fun isNotValid(request: MigrateFromHdcToCvlRequest): Boolean {
    request.prisoner.prisonerNumber ?: throw EntityNotFoundException("No prisoner given found!")

    if (migrationRepository.hasBeenAlreadyMigrated(request.licence.licenceId)) {
      log.info("Licence {} has already been migrated", request.licence.licenceId)
      return true
    }
    if (migrationRepository.hasExistingLicence(request.prisoner.prisonerNumber)) {
      log.info("Licence for prisoner {} already exists", request.prisoner.prisonerNumber)
      return true
    }
    return false
  }
  private fun saveMetaData(
    request: MigrateFromHdcToCvlRequest,
    hdcLicence: HdcLicence,
  ) {
    request.conditions.additional.forEach { hdcCondition ->
      val saveCondition = hdcLicence.bespokeConditions.findLast { it.conditionText == hdcCondition.text }!!
      migrationRepository.saveConditionMetaData(
        licenceId = hdcLicence.id,
        saveCondition.id!!,
        hdcCondition.conditionCode,
        hdcCondition.conditionsVersion,
      )
    }
    migrationRepository.saveMetaData(
      hdcLicence.id,
      request.licence.licenceId,
      request.licence.licenceVersion,
      request.licence.varyVersion,
    )
  }

  fun MigrateFromHdcToCvlRequest.toHdcLicence(): HdcLicence {
    val offenderManager = getOffenderManager(prisoner.prisonerNumber!!)
    val probationTeam = offenderManager.team
    val responsibleCom = licenceCreationService.getOrCreateCom(offenderManager.id)

    val comSet = mutableSetOf(responsibleCom)
    val submittedByCom = comSet.getCommAndAdd(lifecycle.submittedByUserName)
    val createdByCom = comSet.getCommAndAdd(lifecycle.createdByUserName)
    val approvedByStaff = lifecycle.approvedByUsername?.let { getStaff(it) }
    val licence = HdcLicence(
      // Hard coded values
      version = "3.0",
      licenceVersion = "1.0",
      // Main states
      typeCode = licence.typeCode,
      statusCode = LicenceStatus.ACTIVE,

      // Prisoner details
      nomsId = prisoner.prisonerNumber,
      bookingNo = bookingNo,
      bookingId = bookingId,
      pnc = pnc,
      cro = cro,
      crn = offenderManager.case.crn,
      forename = prisoner.forename,
      middleNames = prisoner.middleNames,
      surname = prisoner.surname,
      dateOfBirth = prisoner.dateOfBirth,

      // Prison details
      prisonCode = prison.prisonCode,
      prisonDescription = prison.prisonDescription,
      prisonTelephone = prison.prisonTelephone,

      // Probation
      probationAreaCode = probationTeam.provider.code,
      probationAreaDescription = probationTeam.provider.description,
      probationPduCode = probationTeam.borough.code,
      probationPduDescription = probationTeam.borough.description,
      probationLauCode = probationTeam.district.code,
      probationLauDescription = probationTeam.district.description,
      probationTeamCode = probationTeam.code,
      probationTeamDescription = probationTeam.description,

      // Sentence details
      sentenceStartDate = sentence.sentenceStartDate,
      sentenceEndDate = sentence.sentenceEndDate,
      conditionalReleaseDate = sentence.conditionalReleaseDate,
      actualReleaseDate = sentence.actualReleaseDate,
      topupSupervisionStartDate = sentence.topupSupervisionStartDate,
      topupSupervisionExpiryDate = sentence.topupSupervisionExpiryDate,
      postRecallReleaseDate = sentence.postRecallReleaseDate,

      // License details
      licenceStartDate = licence.homeDetentionCurfewActualDate,
      licenceActivatedDate = licence.licenceActivationDate?.atStartOfDay(),
      licenceExpiryDate = licence.licenceExpiryDate,

      // HDC fields
      homeDetentionCurfewActualDate = licence.homeDetentionCurfewActualDate,
      homeDetentionCurfewEndDate = licence.homeDetentionCurfewEndDate,
      homeDetentionCurfewEligibilityDate = licence.homeDetentionCurfewEligibilityDate,

      // Lifecycle
      responsibleCom = responsibleCom,
      createdBy = createdByCom,
      dateCreated = lifecycle.dateCreated,
      submittedBy = submittedByCom,
      submittedDate = lifecycle.submittedDate,
      approvedByUsername = lifecycle.approvedByUsername,
      approvedByName = approvedByStaff?.fullName,
      approvedDate = lifecycle.approvedDate,
      firstNightCurfewTimes = curfew?.firstNight?.toCvlDomain(),
    )

    val additionalConditions = conditions.additional.mapIndexed { index, condition ->
      BespokeCondition(conditionText = condition.text, licence = licence, conditionSequence = index)
    }

    val lastIndex = additionalConditions.lastOrNull()?.conditionSequence?.plus(1) ?: 0

    val bespokeConditions = conditions.bespoke.mapIndexed { index, text ->
      BespokeCondition(conditionText = text, licence = licence, conditionSequence = index + lastIndex)
    }

    licence.bespokeConditions.addAll(additionalConditions + bespokeConditions)

    appointment?.let {
      licence.appointment = Appointment(
        person = it.person,
        time = it.time,
        timeType = AppointmentTimeType.SPECIFIC_DATE_TIME,
        telephoneContactNumber = it.telephone,
        address = it.address?.toCvlDomain(),
        addressText = it.address?.toSingleLineAddress(),
      )
    }

    curfewAddress?.let {
      licence.curfewAddress = HdcCurfewAddress(
        addressLine1 = it.addressLine1 ?: "",
        addressLine2 = it.addressLine2,
        townOrCity = it.townOrCity ?: "",
        postcode = it.postcode,
        licence = licence,
      )
    }

    licence.weeklyCurfewTimes.addAll(
      curfew?.curfewTimes?.mapIndexed { index, curfewTime -> curfewTime.toCvlDomain(index) }.orEmpty(),
    )
    return licenceRepository.saveAndFlush(licence)
  }

  private fun getStaff(username: String): Staff? = staffRepository.findByUsernameIgnoreCase(username)

  private fun MigrateAppointmentAddress.toSingleLineAddress(): String = listOfNotNull(firstLine, secondLine, townOrCity, postcode)
    .joinToString(", ")

  private fun MigrateAppointmentAddress.toCvlDomain(): Address = Address(
    reference = UUID.randomUUID().toString(),
    firstLine = firstLine ?: "",
    secondLine = secondLine,
    townOrCity = townOrCity ?: "",
    postcode = postcode ?: "",
    source = AddressSource.MANUAL_MIGRATED,
  )

  private fun MigrateCurfewTime.toCvlDomain(index: Int): CurfewTimes = CurfewTimes(
    curfewTimesSequence = index,
    fromDay = fromDay,
    fromTime = fromTime,
    untilDay = untilDay,
    untilTime = untilTime,
    createdTimestamp = LocalDateTime.now(),
  )

  private fun MigrateFirstNight.toCvlDomain(): CurfewTimes = CurfewTimes(
    fromTime = firstNightFrom,
    untilTime = firstNightUntil,
    createdTimestamp = LocalDateTime.now(),
  )

  private fun getOffenderManager(prisonNumber: String): CommunityManager = deliusApiClient.getOffenderManager(prisonNumber)
    ?: throw ValidationException("Could not find offender manager for $prisonNumber in delius")

  private fun MutableSet<CommunityOffenderManager>.getCommAndAdd(
    userName: String?,
  ): CommunityOffenderManager? = userName
    ?.let { getCommunityOffenderManager(it, this) }
    ?.also { add(it) }

  private fun getCommunityOffenderManager(
    userName: String,
    coms: Set<CommunityOffenderManager>,
  ): CommunityOffenderManager = coms.firstOrNull { it.username == userName }
    ?: licenceCreationService.getOrCreateCom(userName)
}
