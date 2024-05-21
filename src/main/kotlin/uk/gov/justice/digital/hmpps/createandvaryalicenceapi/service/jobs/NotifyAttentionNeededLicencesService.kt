package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.NotifyAttentionNeededLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.LocalDate

@Service
class NotifyAttentionNeededLicencesService(
  private val licenceRepository: LicenceRepository,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val notifyService: NotifyService,
) {

  @Transactional
  fun notifyAttentionNeededLicences() {
    val attentionNeededLicences = licenceRepository.getAttentionNeededLicences()
    if (attentionNeededLicences.isEmpty()) {
      return
    }

    val prisoners = prisonerSearchApiClient.searchPrisonersByNomisIds(attentionNeededLicences.map { it.nomsId!! })

    val prisonerToLegalStatus = prisoners.associateBy { it.prisonerNumber }
    val notifyLicences = attentionNeededLicences.map {
      val associatedLegalStatus = prisonerToLegalStatus[it.nomsId]?.legalStatus
      NotifyAttentionNeededLicence(
        it.nomsId,
        associatedLegalStatus,
        it.conditionalReleaseDate,
        it.actualReleaseDate,
      )
    }
    val fileName = "attentionNeededLicences_" + LocalDate.now() + ".csv"
    val fileContents =
      FileOutputStream(fileName).apply { writeCsv(notifyLicences) }


    notifyService.sendAttentionNeededLicencesEmail(
      "nishanth.mahasamudram@digital.justice.gov.uk",
      fileContents,
      fileName,
    )
  }

  fun OutputStream.writeCsv(licences: List<NotifyAttentionNeededLicence>) {
    val writer = bufferedWriter()
    writer.write(""""NomsID", "NomsLegalStatus", "ARD"""")
    writer.newLine()
    licences.forEach {
      writer.write("${it.nomsId}, ${it.legalStatus}, \"${it.conditionalReleaseDate}\"")
      writer.newLine()
    }
    writer.flush()
  }

}
