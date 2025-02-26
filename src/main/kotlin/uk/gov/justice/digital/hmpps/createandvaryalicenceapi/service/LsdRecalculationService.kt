package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import java.time.LocalDateTime

@Service
class LsdRecalculationService(
  private val licenceRepository: LicenceRepository,
  private val releaseDateService: ReleaseDateService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val auditEventRepository: AuditEventRepository,
) {
  @Transactional
  fun batchUpdateLicenceStartDate(numberOfLicences: Long, lastUpdatedLicenceId: Long = 0): Long {
    val licences = licenceRepository.findLicencesToBatchUpdateLsd(numberOfLicences, lastUpdatedLicenceId)
    if (licences.isEmpty()) {
      return -1
    }

    val maxLicenceId = licences.maxOf { it.id }
    val prisonNumbers = licences.mapNotNull { it.nomsId }
    if (prisonNumbers.isEmpty()) {
      return maxLicenceId
    }

    val prisoners = this.prisonerSearchApiClient.searchPrisonersByNomisIds(prisonNumbers)
    val prisonersToLSDs = releaseDateService.getLicenceStartDates(prisoners)

    licences.forEach {
      val licenceStartDate = prisonersToLSDs[it.nomsId]

      if (it.licenceStartDate != licenceStartDate) {
        val oldLsd = it.licenceStartDate

        val updatedLicence = it.updateLicenceDates(
          status = it.statusCode,
          conditionalReleaseDate = it.conditionalReleaseDate,
          actualReleaseDate = it.actualReleaseDate,
          sentenceStartDate = it.sentenceStartDate,
          sentenceEndDate = it.sentenceEndDate,
          licenceStartDate = licenceStartDate,
          licenceExpiryDate = it.licenceExpiryDate,
          topupSupervisionStartDate = it.topupSupervisionStartDate,
          topupSupervisionExpiryDate = it.topupSupervisionExpiryDate,
          postRecallReleaseDate = it.postRecallReleaseDate,
          homeDetentionCurfewActualDate = if (it is HdcLicence) it.homeDetentionCurfewActualDate else null,
          homeDetentionCurfewEndDate = if (it is HdcLicence) it.homeDetentionCurfewEndDate else null,
          staffMember = null,
        )

        licenceRepository.saveAndFlush(updatedLicence)

        auditEventRepository.saveAndFlush(
          AuditEvent(
            licenceId = it.id,
            detail = "ID ${it.id} type ${it.typeCode} status ${it.statusCode} version ${it.version}",
            eventTime = LocalDateTime.now(),
            eventType = AuditEventType.SYSTEM_EVENT,
            username = SYSTEM_USER,
            fullName = SYSTEM_USER,
            summary = "Licence Start Date recalculated from $oldLsd to $licenceStartDate for ${it.forename} ${it.surname}",
          ),
        )
      }
    }

    return maxLicenceId
  }
}
