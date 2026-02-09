package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.validation.ValidationException
import org.springframework.data.mapping.PropertyReferenceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCaseRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceApproverCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.CRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HARD_STOP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HDC
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HDC_VARIATION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.PRRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.TIME_SERVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.VARIATION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import java.time.LocalDate
import java.time.LocalDateTime

private const val TWO_WEEKS = 14L

@Service
class PrisonApproverService(
  private val licenceCaseRepository: LicenceCaseRepository,
) {
  @Transactional
  fun getLicenceCasesReadyForApproval(prisons: List<String>?): List<LicenceApproverCase> {
    if (prisons.isNullOrEmpty()) {
      return emptyList()
    }

    val cases = licenceCaseRepository.findLicenceCasesReadyForApproval(prisons)
    return enrichWithSubmitterNames(cases.sortedWith(compareBy(nullsLast()) { it.licenceStartDate }))
  }

  @Transactional
  fun findRecentlyApprovedLicenceCases(
    prisonCodes: List<String>,
  ): List<LicenceApproverCase> {
    try {
      val activatedAfterDate = LocalDate.now().minusDays(TWO_WEEKS).atStartOfDay()
      val recentlyApprovedCases = getRecentlyApprovedLicenceCases(prisonCodes, activatedAfterDate)

      // if a licence is an active variation then we want to return the original
      // licence that the variation was created from and not the variation itself
      val originalRecentlyApprovedCases = recentlyApprovedCases.map {
        if (it.statusCode == ACTIVE && it.kind.isVariation()) {
          findOriginalLicenceForVariation(it)
        } else {
          it
        }
      }
      return enrichWithSubmitterNames(originalRecentlyApprovedCases)
    } catch (e: PropertyReferenceException) {
      throw ValidationException(e.message, e)
    }
  }

  private fun getRecentlyApprovedLicenceCases(
    prisonCodes: List<String>,
    approvedSince: LocalDateTime,
  ): List<LicenceApproverCase> = licenceCaseRepository.findRecentlyApprovedLicenceCasesAfter(prisonCodes, approvedSince)

  private fun enrichWithSubmitterNames(cases: List<LicenceApproverCase>): List<LicenceApproverCase> {
    if (cases.isNotEmpty()) {
      val submittedByNames = licenceCaseRepository.findSubmittedByNames(cases.map { it.licenceId })
        .associate { row -> row.licenceId to row.fullName }
      cases.forEach { it.submittedByFullName = submittedByNames[it.licenceId] }
    }
    return cases
  }

  private fun findOriginalLicenceForVariation(variationLicenceCase: LicenceApproverCase): LicenceApproverCase {
    var currentLicence = variationLicenceCase

    while (currentLicence.variationOfId != null) {
      val parentLicence = licenceCaseRepository.findLicenceApproverCase(currentLicence.variationOfId!!)

      when (parentLicence.kind) {
        CRD, PRRD, HARD_STOP, HDC, TIME_SERVED -> return parentLicence
        VARIATION, HDC_VARIATION -> currentLicence = parentLicence
      }
    }
    error("Original licence not found for licenceId=${variationLicenceCase.licenceId}")
  }
}
