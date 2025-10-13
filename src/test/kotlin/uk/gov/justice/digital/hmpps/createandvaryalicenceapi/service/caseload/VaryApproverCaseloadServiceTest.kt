package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.times
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.VaryApproverCaseloadSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

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
          licenceStartDate = LocalDate.now().plusDays(10),
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
      assertThat(name).isEqualTo("First-1 Surname-2")
      assertThat(crnNumber).isEqualTo("X12348")
      assertThat(licenceType).isEqualTo(LicenceType.PSS)
      assertThat(variationRequestDate).isEqualTo(licenceSummaries.first().dateCreated?.toLocalDate())
      assertThat(releaseDate).isEqualTo(licenceSummaries.first().licenceStartDate)
      assertThat(probationPractitioner).isEqualTo("Delius User")
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
          licenceStartDate = LocalDate.now().plusDays(10),
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
          licenceStartDate = LocalDate.now().plusDays(10),
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
      assertThat(name).isEqualTo("First-1 Surname-2")
      assertThat(crnNumber).isEqualTo("X12348")
      assertThat(licenceType).isEqualTo(LicenceType.PSS)
      assertThat(variationRequestDate).isEqualTo(licenceSummaries.first().dateCreated?.toLocalDate())
      assertThat(releaseDate).isEqualTo(licenceSummaries.first().licenceStartDate)
      assertThat(probationPractitioner).isEqualTo("Delius User")
    }
    verify(licenceService).findLicencesMatchingCriteria(pduQuery)
  }

  @Test
  fun `should search for offender for a probation delivery unit`() {
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
          licenceStartDate = LocalDate.now().plusDays(10),
        ),
      ),
    )
    whenever(deliusApiClient.getStaffDetailsByUsername(licenceSummaries.map { it.comUsername!! })).thenReturn(
      listOf(aDeliusUser()),
    )

    // When
    val searchResults =
      service.searchForOffenderOnVaryApproverCaseload(VaryApproverCaseloadSearchRequest(probationPduCodes = pdus, searchTerm = "First"))

    // Then
    assertThat(searchResults.pduCasesResponse).hasSize(1)
    with(searchResults.pduCasesResponse.first()) {
      assertThat(licenceId).isEqualTo(1)
      assertThat(name).isEqualTo("First-1 Surname-2")
      assertThat(crnNumber).isEqualTo("X12348")
      assertThat(licenceType).isEqualTo(LicenceType.PSS)
      assertThat(variationRequestDate).isEqualTo(licenceSummaries.first().dateCreated?.toLocalDate())
      assertThat(releaseDate).isEqualTo(licenceSummaries.first().licenceStartDate)
      assertThat(probationPractitioner).isEqualTo("Delius User")
    }
    assertThat(searchResults.regionCasesResponse).hasSize(0)

    verify(licenceService).findLicencesMatchingCriteria(pduQuery)
    verify(licenceService, times(0)).findSubmittedVariationsByRegion(any())
  }

  @Test
  fun `should search for offender for a probation region`() {
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
          licenceStartDate = LocalDate.now().plusDays(10),
        ),
      ),
    )
    whenever(deliusApiClient.getStaffDetailsByUsername(licenceSummaries.map { it.comUsername!! })).thenReturn(
      listOf(aDeliusUser()),
    )

    // When
    val searchResults =
      service.searchForOffenderOnVaryApproverCaseload(VaryApproverCaseloadSearchRequest(probationAreaCode = probationAreaCode, searchTerm = "First"))

    // Then
    assertThat(searchResults.regionCasesResponse).hasSize(1)
    with(searchResults.regionCasesResponse.first()) {
      assertThat(licenceId).isEqualTo(1)
      assertThat(name).isEqualTo("First-1 Surname-2")
      assertThat(crnNumber).isEqualTo("X12348")
      assertThat(licenceType).isEqualTo(LicenceType.PSS)
      assertThat(variationRequestDate).isEqualTo(licenceSummaries.first().dateCreated?.toLocalDate())
      assertThat(releaseDate).isEqualTo(licenceSummaries.first().licenceStartDate)
      assertThat(probationPractitioner).isEqualTo("Delius User")
    }
    assertThat(searchResults.pduCasesResponse).hasSize(0)

    verify(licenceService, times(0)).findLicencesMatchingCriteria(any())
    verify(licenceService).findSubmittedVariationsByRegion(probationAreaCode)
  }

  @Test
  fun `should search for offenders for both a probation delivery unit and probation region`() {
    // Given
    val pdus = listOf("N55PDV")
    val probationAreaCode = "N01"
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
          licenceStartDate = LocalDate.now().plusDays(10),
        ),
      ),
    )
    whenever(deliusApiClient.getStaffDetailsByUsername(licenceSummaries.map { it.comUsername!! })).thenReturn(
      listOf(aDeliusUser()),
    )

    // When
    val searchResults =
      service.searchForOffenderOnVaryApproverCaseload(VaryApproverCaseloadSearchRequest(probationPduCodes = pdus, probationAreaCode = probationAreaCode, searchTerm = "First"))

    // Then
    assertThat(searchResults.pduCasesResponse).hasSize(1)
    with(searchResults.pduCasesResponse.first()) {
      assertThat(licenceId).isEqualTo(1)
      assertThat(name).isEqualTo("First-1 Surname-2")
      assertThat(crnNumber).isEqualTo("X12348")
      assertThat(licenceType).isEqualTo(LicenceType.PSS)
      assertThat(variationRequestDate).isEqualTo(licenceSummaries.first().dateCreated?.toLocalDate())
      assertThat(releaseDate).isEqualTo(licenceSummaries.first().licenceStartDate)
      assertThat(probationPractitioner).isEqualTo("Delius User")
    }

    assertThat(searchResults.regionCasesResponse).hasSize(1)
    with(searchResults.regionCasesResponse.first()) {
      assertThat(licenceId).isEqualTo(1)
      assertThat(name).isEqualTo("First-1 Surname-2")
      assertThat(crnNumber).isEqualTo("X12348")
      assertThat(licenceType).isEqualTo(LicenceType.PSS)
      assertThat(variationRequestDate).isEqualTo(licenceSummaries.first().dateCreated?.toLocalDate())
      assertThat(releaseDate).isEqualTo(licenceSummaries.first().licenceStartDate)
      assertThat(probationPractitioner).isEqualTo("Delius User")
    }

    verify(licenceService).findLicencesMatchingCriteria(pduQuery)
    verify(licenceService).findSubmittedVariationsByRegion(probationAreaCode)
  }

  @Test
  fun `should return no results when search term does not match`() {
    // Given
    val pdus = listOf("N55PDV")
    val probationAreaCode = "N01"
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
          licenceStartDate = LocalDate.now().plusDays(10),
        ),
      ),
    )
    whenever(deliusApiClient.getStaffDetailsByUsername(licenceSummaries.map { it.comUsername!! })).thenReturn(
      listOf(aDeliusUser()),
    )

    // When
    val searchResults =
      service.searchForOffenderOnVaryApproverCaseload(VaryApproverCaseloadSearchRequest(probationPduCodes = pdus, probationAreaCode = probationAreaCode, searchTerm = "XXXX"))

    // Then
    assertThat(searchResults.pduCasesResponse).isEmpty()
    assertThat(searchResults.regionCasesResponse).isEmpty()
  }
}
