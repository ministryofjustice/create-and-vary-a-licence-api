package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.data.mapping.PropertyReferenceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Variation
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.VariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummaryApproverView
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import java.time.LocalDate

private const val TWO_WEEKS = 14L

@Service
class PrisonApproverService(
  private val licenceRepository: LicenceRepository,
  private val releaseDateService: ReleaseDateService,
  private val licenceService: LicenceService,
) {
  @Transactional
  fun getLicencesForApproval(prisons: List<String>?): List<LicenceSummaryApproverView> {
    if (prisons.isNullOrEmpty()) {
      return emptyList()
    }
    val licences = licenceRepository.getLicencesReadyForApproval(prisons)
      .sortedWith(compareBy(nullsLast()) { it.licenceStartDate })
    return licences.map { it.toApprovalSummaryView() }
  }

  @Transactional
  fun findRecentlyApprovedLicences(
    prisonCodes: List<String>,
  ): List<LicenceSummaryApproverView> {
    try {
      val releasedAfterDate = LocalDate.now().minusDays(TWO_WEEKS)
      val recentActiveAndApprovedLicences =
        licenceRepository.getRecentlyApprovedLicences(prisonCodes, releasedAfterDate)

      // if a licence is an active variation then we want to return the original
      // licence that the variation was created from and not the variation itself
      val recentlyApprovedLicences = recentActiveAndApprovedLicences.map {
        if (it.statusCode == ACTIVE && it is Variation) {
          findOriginalLicenceForVariation(it)
        } else {
          it
        }
      }
      return recentlyApprovedLicences.map { it.toApprovalSummaryView() }
    } catch (e: PropertyReferenceException) {
      throw ValidationException(e.message, e)
    }
  }

  private fun findOriginalLicenceForVariation(variationLicence: Variation): Licence {
    var originalLicence = variationLicence
    while (originalLicence.variationOfId != null) {
      val licence = licenceRepository
        .findById(originalLicence.variationOfId!!)
        .orElseThrow { EntityNotFoundException("${originalLicence.variationOfId}") }
      when (licence) {
        is CrdLicence, is PrrdLicence, is HardStopLicence, is HdcLicence -> return licence
        is VariationLicence -> originalLicence = licence
        is HdcVariationLicence -> originalLicence = licence
        else -> error("Unknown licence type in hierarchy: ${licence.javaClass}")
      }
    }
    error("original licence not found for licence: ${variationLicence.id}")
  }

  private fun Licence.toApprovalSummaryView(): LicenceSummaryApproverView = transformToApprovalLicenceSummary(
    licence = this,
    isReviewNeeded = licenceService.isReviewNeeded(this),
    hardStopDate = releaseDateService.getHardStopDate(licenceStartDate),
    hardStopWarningDate = releaseDateService.getHardStopWarningDate(licenceStartDate),
    isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licenceStartDate),
    isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(
      licenceStartDate,
    ),
  )
}
