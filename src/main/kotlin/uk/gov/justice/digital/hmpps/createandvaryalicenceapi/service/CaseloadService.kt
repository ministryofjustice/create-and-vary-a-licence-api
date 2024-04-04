package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseloadItem
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType

@Service
class CaseloadService(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val releaseDateService: ReleaseDateService,
) {

  fun getPrisonersByNumber(nomisIds: List<String>) =
    prisonerSearchApiClient.searchPrisonersByNomisIds(nomisIds).map { it.toCaseloadItem() }

  fun getPrisoner(nomisId: String) =
    getPrisonersByNumber(listOf(nomisId)).firstOrNull() ?: throw EntityNotFoundException(nomisId)

  private fun PrisonerSearchPrisoner.toCaseloadItem(): CaseloadItem {
    val sentenceDateHolder = this.toSentenceDateHolder()
    return CaseloadItem(
      prisoner = this.toPrisoner(),
      cvl = CvlFields(
        licenceType = LicenceType.getLicenceType(this),
        hardStopDate = releaseDateService.getHardStopDate(sentenceDateHolder),
        hardStopWarningDate = releaseDateService.getHardStopWarningDate(sentenceDateHolder),
      ),
    )
  }

  fun PrisonerSearchPrisoner.toSentenceDateHolder() = object : SentenceDateHolder {
    override val conditionalReleaseDate = this@toSentenceDateHolder.conditionalReleaseDate
    override val actualReleaseDate = confirmedReleaseDate
    override val licenceStartDate = this.actualReleaseDate ?: this.conditionalReleaseDate
  }
}
