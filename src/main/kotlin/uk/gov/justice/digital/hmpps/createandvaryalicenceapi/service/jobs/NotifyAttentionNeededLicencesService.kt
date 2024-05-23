package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.NotifyAttentionNeededLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.writeCsv
import java.time.LocalDate

@Service
class NotifyAttentionNeededLicencesService(
  @Value("\${notify.attentionNeededLicences.emailAddress}") private val emailAddress: String,
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
      .sortedWith(compareBy({ it.actualReleaseDate }, { it.conditionalReleaseDate }))

    val fileName = "attentionNeededLicences_" + LocalDate.now() + ".csv"
    val fileContents = writeCsv(notifyLicences)

    notifyService.sendAttentionNeededLicencesEmail(
      emailAddress,
      fileContents.toByteArray(),
      fileName,
    )
  }
}
