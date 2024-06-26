package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyAttentionNeededLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.writeCsv
import java.time.LocalDate

@Service
class NotifyAttentionNeededLicencesService(
  @Value("\${internalEmailAddress}") private val emailAddress: String,
  private val licenceRepository: LicenceRepository,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val notifyService: NotifyService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val BATCH_SIZE = 500
  }

  @Transactional
  fun notifyAttentionNeededLicences() {
    val attentionNeededLicences = licenceRepository.getAttentionNeededLicences().distinctBy { it.nomsId }
    log.info("Job notifyAttentionNeededLicences found ${attentionNeededLicences.size} licences to process")
    if (attentionNeededLicences.isEmpty()) {
      return
    }

    val batchedNomsIds = attentionNeededLicences.map { it.nomsId!! }.chunked(BATCH_SIZE)
    val prisoners = batchedNomsIds.map {
      prisonerSearchApiClient.searchPrisonersByNomisIds(it)
    }.flatten()

    val prisonerToLegalStatus = prisoners.associateBy { it.prisonerNumber }
    val notifyLicences = attentionNeededLicences.map {
      val associatedLegalStatus = prisonerToLegalStatus[it.nomsId]?.legalStatus
      val associatedPrisonName = prisonerToLegalStatus[it.nomsId]?.prisonName
      NotifyAttentionNeededLicence(
        it.nomsId,
        associatedPrisonName,
        associatedLegalStatus,
        it.conditionalReleaseDate,
        it.actualReleaseDate,
        it.licenceStartDate,
      )
    }
      .sortedBy { it.licenceStartDate }

    val fileName = "attentionNeededLicences_" + LocalDate.now() + ".csv"
    val fileContents = writeCsv(notifyLicences)

    notifyService.sendAttentionNeededLicencesEmail(
      emailAddress,
      fileContents.toByteArray(),
      fileName,
    )
    log.info("Job notifyAttentionNeededLicences emailed ${attentionNeededLicences.size} licences")
  }
}
