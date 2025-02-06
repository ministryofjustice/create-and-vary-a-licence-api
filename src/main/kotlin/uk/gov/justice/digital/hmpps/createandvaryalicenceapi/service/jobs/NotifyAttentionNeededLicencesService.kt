package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
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
  }

  @Transactional
  fun notifyAttentionNeededLicences() {
    val attentionNeededLicences = licenceRepository.getAttentionNeededLicences().distinctBy { it.nomsId }
    log.info("Job notifyAttentionNeededLicences found ${attentionNeededLicences.size} licences to process")
    if (attentionNeededLicences.isEmpty()) {
      return
    }

    val nomisIds = attentionNeededLicences.map { it.nomsId!! }
    val prisoners = prisonerSearchApiClient.searchPrisonersByNomisIds(nomisIds)

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
    val fileContents = notifyLicences.toCsv()

    notifyService.sendAttentionNeededLicencesEmail(
      emailAddress,
      fileContents.toByteArray(),
      fileName,
    )
    log.info("Job notifyAttentionNeededLicences emailed ${attentionNeededLicences.size} licences")
  }

  fun List<NotifyAttentionNeededLicence>.toCsv(): String {
    val csv = StringBuilder()
    csv.append("Noms ID,Prison Name,Noms Legal Status,ARD,CRD,Licence Start Date\r\n")
    this.forEach {
      csv.append("${it.nomsId},${it.prisonName},${it.legalStatus},${it.actualReleaseDate},${it.conditionalReleaseDate},${it.licenceStartDate}\r\n")
    }
    return csv.toString()
  }

  data class NotifyAttentionNeededLicence(
    val nomsId: String?,
    var prisonName: String? = null,
    var legalStatus: String? = null,
    val conditionalReleaseDate: LocalDate?,
    val actualReleaseDate: LocalDate?,
    val licenceStartDate: LocalDate?,
  )
}
