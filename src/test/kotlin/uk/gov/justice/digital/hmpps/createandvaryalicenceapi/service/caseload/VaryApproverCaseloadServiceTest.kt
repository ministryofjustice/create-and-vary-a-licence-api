package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseloadItem
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aDeliusUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aProbationCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.someCvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.VaryApproverCaseloadSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType

class VaryApproverCaseloadServiceTest {
  private val caseloadService = mock<CaseloadService>()
  private val deliusApiClient = mock<DeliusApiClient>()
  private val licenceService = mock<LicenceService>()

  private val service = VaryApproverCaseloadService(
    caseloadService,
    deliusApiClient,
    licenceService,
  )

  @Test
  fun `should build the vary approver caseload for a probation region`() {
    // Given
    val probationAreaCode = "N01"
    val licenceSummaries = listOf(
      aLicenceSummary(
        kind = LicenceKind.VARIATION,
        type = LicenceType.PSS,
        status = LicenceStatus.VARIATION_SUBMITTED,
      ),
    )
    val probationCases = listOf(aProbationCase())

    whenever(licenceService.findSubmittedVariationsByRegion(probationAreaCode)).thenReturn(licenceSummaries)
    whenever(
      deliusApiClient.getProbationCases(
        licenceSummaries.map { it.nomisId },
      ),
    ).thenReturn(probationCases)
    whenever(caseloadService.getPrisonersByNumber(probationCases.map { it.nomisId!! })).thenReturn(
      listOf(
        CaseloadItem(
          prisoner = aPrisoner(),
          cvl = someCvlFields(licenceType = LicenceType.PSS),
        ),
      ),
    )
    whenever(deliusApiClient.getStaffDetailsByUsername(licenceSummaries.map { it.comUsername!! })).thenReturn(
      listOf(aDeliusUser()),
    )

    // When
    val caseload =
      service.getVaryApproverCaseload(VaryApproverCaseloadSearchRequest(probationAreaCode = probationAreaCode))

    // Then
    assertThat(caseload).hasSize(1)
    with(caseload.first()) {
      assertThat(licenceId).isEqualTo(1)
      assertThat(name).isEqualTo("Gary Pittard")
      assertThat(crnNumber).isEqualTo("X12348")
      assertThat(licenceType).isEqualTo(LicenceType.PSS)
      assertThat(variationRequestDate).isEqualTo(licenceSummaries.first().dateCreated?.toLocalDate())
      assertThat(releaseDate).isEqualTo(licenceSummaries.first().licenceStartDate)
      assertThat(probationPractitioner).isEqualTo("Joe Bloggs")
    }
    verify(licenceService).findSubmittedVariationsByRegion(probationAreaCode)
  }

  @Test
  fun `should return empty caseload if search does not match`() {
    // Given
    val probationAreaCode = "N01"
    val licenceSummaries = listOf(
      aLicenceSummary(
        kind = LicenceKind.VARIATION,
        type = LicenceType.PSS,
        status = LicenceStatus.VARIATION_SUBMITTED,
      ),
    )
    val probationCases = listOf(aProbationCase())

    whenever(licenceService.findSubmittedVariationsByRegion(probationAreaCode)).thenReturn(licenceSummaries)
    whenever(
      deliusApiClient.getProbationCases(
        licenceSummaries.map { it.nomisId },
      ),
    ).thenReturn(probationCases)
    whenever(caseloadService.getPrisonersByNumber(probationCases.map { it.nomisId!! })).thenReturn(
      listOf(
        CaseloadItem(
          prisoner = aPrisoner(),
          cvl = someCvlFields(licenceType = LicenceType.PSS),
        ),
      ),
    )
    whenever(deliusApiClient.getStaffDetailsByUsername(licenceSummaries.map { it.comUsername!! })).thenReturn(
      listOf(aDeliusUser()),
    )

    // When
    val caseload = service.getVaryApproverCaseload(
      VaryApproverCaseloadSearchRequest(
        probationAreaCode = probationAreaCode,
        searchTerm = "XXXX",
      ),
    )

    // Then
    assertThat(caseload).isEmpty()
  }

  @Test
  fun `should build the vary approver caseload for a probation delivery unit`() {
    // Given
    val pdus = listOf("N55PDV")
    val licenceSummaries = listOf(
      aLicenceSummary(
        kind = LicenceKind.VARIATION,
        type = LicenceType.PSS,
        status = LicenceStatus.VARIATION_SUBMITTED,
      ),
    )
    val probationCases = listOf(aProbationCase())
    val pduQuery = LicenceQueryObject(statusCodes = listOf(LicenceStatus.VARIATION_SUBMITTED), pdus = pdus)

    whenever(licenceService.findLicencesMatchingCriteria(pduQuery)).thenReturn(licenceSummaries)
    whenever(
      deliusApiClient.getProbationCases(
        licenceSummaries.map { it.nomisId },
      ),
    ).thenReturn(probationCases)
    whenever(caseloadService.getPrisonersByNumber(probationCases.map { it.nomisId!! })).thenReturn(
      listOf(
        CaseloadItem(
          prisoner = aPrisoner(),
          cvl = someCvlFields(licenceType = LicenceType.PSS),
        ),
      ),
    )
    whenever(deliusApiClient.getStaffDetailsByUsername(licenceSummaries.map { it.comUsername!! })).thenReturn(
      listOf(aDeliusUser()),
    )

    // When
    val caseload =
      service.getVaryApproverCaseload(VaryApproverCaseloadSearchRequest(probationPduCodes = pdus))

    // Then
    assertThat(caseload).hasSize(1)
    with(caseload.first()) {
      assertThat(licenceId).isEqualTo(1)
      assertThat(name).isEqualTo("Gary Pittard")
      assertThat(crnNumber).isEqualTo("X12348")
      assertThat(licenceType).isEqualTo(LicenceType.PSS)
      assertThat(variationRequestDate).isEqualTo(licenceSummaries.first().dateCreated?.toLocalDate())
      assertThat(releaseDate).isEqualTo(licenceSummaries.first().licenceStartDate)
      assertThat(probationPractitioner).isEqualTo("Joe Bloggs")
    }
    verify(licenceService).findLicencesMatchingCriteria(pduQuery)
  }
}
