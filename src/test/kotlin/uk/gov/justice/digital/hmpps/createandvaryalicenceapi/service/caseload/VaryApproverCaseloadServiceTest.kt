package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCaseRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceVaryApproverCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aProbationCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.Name
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.VaryApproverCaseloadSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.PSS
import java.time.LocalDate
import java.time.LocalDateTime

class VaryApproverCaseloadServiceTest {
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val deliusApiClient = mock<DeliusApiClient>()
  private val licenceCaseRepository = mock<LicenceCaseRepository>()

  private val service = VaryApproverCaseloadService(
    prisonerSearchApiClient,
    deliusApiClient,
    licenceCaseRepository,
  )

  @Test
  fun `should build the vary approver caseload for a probation region`() {
    // Given
    val probationAreaCode = "N01"
    val licenceSummaries = listOf(aLicenceVaryApproverCase(type = PSS))
    val probationCases = listOf(aProbationCase())

    whenever(licenceCaseRepository.findSubmittedVariationsByRegion(probationAreaCode)).thenReturn(licenceSummaries)
    whenever(
      deliusApiClient.getProbationCases(
        licenceSummaries.map { it.prisonNumber!! },
      ),
    ).thenReturn(probationCases)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(probationCases.map { it.nomisId!! })).thenReturn(
      listOf(
        prisonerSearchResult().copy(prisonerNumber = aProbationCase().nomisId!!),
      ),
    )
    whenever(deliusApiClient.getOffenderManagersWithoutUser(licenceSummaries.map { it.prisonNumber!! })).thenReturn(
      listOf(aCommunityManagerWithoutUser()),
    )

    // When
    val caseload =
      service.getVaryApproverCaseload(VaryApproverCaseloadSearchRequest(probationAreaCode = probationAreaCode))

    // Then
    assertThat(caseload).hasSize(1)
    with(caseload.first()) {
      assertThat(licenceId).isEqualTo(1)
      assertThat(name).isEqualTo("A Prisoner")
      assertThat(crnNumber).isEqualTo("X12348")
      assertThat(licenceType).isEqualTo(PSS)
      assertThat(variationRequestDate).isEqualTo(licenceSummaries.first().dateCreated?.toLocalDate())
      assertThat(releaseDate).isEqualTo(licenceSummaries.first().licenceStartDate)
      with(probationPractitioner!!) {
        assertThat(staffCode).isEqualTo("AB012C")
        assertThat(name).isEqualTo("Delius User")
      }
    }
    verify(licenceCaseRepository).findSubmittedVariationsByRegion(probationAreaCode)
  }

  @Test
  fun `should return empty caseload if search does not match`() {
    // Given
    val probationAreaCode = "N01"
    val licenceSummaries = listOf(aLicenceVaryApproverCase(type = PSS))
    val probationCases = listOf(aProbationCase())

    whenever(licenceCaseRepository.findSubmittedVariationsByRegion(probationAreaCode)).thenReturn(licenceSummaries)
    whenever(
      deliusApiClient.getProbationCases(
        licenceSummaries.map { it.prisonNumber!! },
      ),
    ).thenReturn(probationCases)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(probationCases.map { it.nomisId!! })).thenReturn(
      listOf(prisonerSearchResult().copy(prisonerNumber = aProbationCase().nomisId!!)),
    )
    whenever(deliusApiClient.getOffenderManagersWithoutUser(licenceSummaries.map { it.prisonNumber!! })).thenReturn(
      listOf(aCommunityManagerWithoutUser()),
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
      aLicenceVaryApproverCase(
        type = PSS,
      ),
    )
    val probationCases = listOf(aProbationCase())

    whenever(licenceCaseRepository.findSubmittedVariationsByPduCodes(pdus)).thenReturn(licenceSummaries)
    whenever(
      deliusApiClient.getProbationCases(
        licenceSummaries.map { it.prisonNumber!! },
      ),
    ).thenReturn(probationCases)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(probationCases.map { it.nomisId!! })).thenReturn(
      listOf(
        prisonerSearchResult().copy(prisonerNumber = aProbationCase().nomisId!!),
      ),
    )
    whenever(deliusApiClient.getOffenderManagersWithoutUser(licenceSummaries.map { it.prisonNumber!! })).thenReturn(
      listOf(aCommunityManagerWithoutUser()),
    )

    // When
    val caseload =
      service.getVaryApproverCaseload(VaryApproverCaseloadSearchRequest(probationPduCodes = pdus))

    // Then
    assertThat(caseload).hasSize(1)
    with(caseload.first()) {
      assertThat(licenceId).isEqualTo(1)
      assertThat(name).isEqualTo("A Prisoner")
      assertThat(crnNumber).isEqualTo("X12348")
      assertThat(licenceType).isEqualTo(PSS)
      assertThat(variationRequestDate).isEqualTo(licenceSummaries.first().dateCreated?.toLocalDate())
      assertThat(releaseDate).isEqualTo(licenceSummaries.first().licenceStartDate)
      with(probationPractitioner!!) {
        assertThat(staffCode).isEqualTo("AB012C")
        assertThat(name).isEqualTo("Delius User")
      }
    }
    verify(licenceCaseRepository).findSubmittedVariationsByPduCodes(pdus)
  }

  @Test
  fun `should search for offender for a probation delivery unit`() {
    // Given
    val pdus = listOf("N55PDV")
    val licenceSummaries = listOf(aLicenceVaryApproverCase(type = PSS))
    val probationCases = listOf(aProbationCase())

    whenever(licenceCaseRepository.findSubmittedVariationsByPduCodes(pdus)).thenReturn(licenceSummaries)
    whenever(
      deliusApiClient.getProbationCases(
        licenceSummaries.map { it.prisonNumber!! },
      ),
    ).thenReturn(probationCases)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(probationCases.map { it.nomisId!! })).thenReturn(
      listOf(
        prisonerSearchResult().copy(prisonerNumber = aProbationCase().nomisId!!),
      ),
    )
    whenever(deliusApiClient.getOffenderManagersWithoutUser(licenceSummaries.map { it.prisonNumber!! })).thenReturn(
      listOf(aCommunityManagerWithoutUser()),
    )

    // When
    val searchResults =
      service.searchForOffenderOnVaryApproverCaseload(
        VaryApproverCaseloadSearchRequest(
          probationPduCodes = pdus,
          searchTerm = "A",
        ),
      )

    // Then
    assertThat(searchResults.pduCasesResponse).hasSize(1)
    with(searchResults.pduCasesResponse.first()) {
      assertThat(licenceId).isEqualTo(1)
      assertThat(name).isEqualTo("A Prisoner")
      assertThat(crnNumber).isEqualTo("X12348")
      assertThat(licenceType).isEqualTo(PSS)
      assertThat(variationRequestDate).isEqualTo(licenceSummaries.first().dateCreated?.toLocalDate())
      assertThat(releaseDate).isEqualTo(licenceSummaries.first().licenceStartDate)
      with(probationPractitioner!!) {
        assertThat(staffCode).isEqualTo("AB012C")
        assertThat(name).isEqualTo("Delius User")
      }
    }
    assertThat(searchResults.regionCasesResponse).hasSize(0)

    verify(licenceCaseRepository).findSubmittedVariationsByPduCodes(pdus)
    verify(licenceCaseRepository, times(0)).findSubmittedVariationsByRegion(any())
  }

  @Test
  fun `should search for offender for a probation region`() {
    // Given
    val probationAreaCode = "N01"
    val licenceSummaries = listOf(
      aLicenceVaryApproverCase(
        type = PSS,
      ),
    )
    val probationCases = listOf(aProbationCase())

    whenever(licenceCaseRepository.findSubmittedVariationsByRegion(probationAreaCode)).thenReturn(licenceSummaries)
    whenever(
      deliusApiClient.getProbationCases(
        licenceSummaries.map { it.prisonNumber!! },
      ),
    ).thenReturn(probationCases)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(probationCases.map { it.nomisId!! })).thenReturn(
      listOf(
        prisonerSearchResult().copy(prisonerNumber = aProbationCase().nomisId!!),
      ),
    )
    whenever(deliusApiClient.getOffenderManagersWithoutUser(licenceSummaries.map { it.prisonNumber!! })).thenReturn(
      listOf(aCommunityManagerWithoutUser()),
    )

    // When
    val searchResults =
      service.searchForOffenderOnVaryApproverCaseload(
        VaryApproverCaseloadSearchRequest(
          probationAreaCode = probationAreaCode,
          searchTerm = "A",
        ),
      )

    // Then
    assertThat(searchResults.regionCasesResponse).hasSize(1)
    with(searchResults.regionCasesResponse.first()) {
      assertThat(licenceId).isEqualTo(1)
      assertThat(name).isEqualTo("A Prisoner")
      assertThat(crnNumber).isEqualTo("X12348")
      assertThat(licenceType).isEqualTo(PSS)
      assertThat(variationRequestDate).isEqualTo(licenceSummaries.first().dateCreated?.toLocalDate())
      assertThat(releaseDate).isEqualTo(licenceSummaries.first().licenceStartDate)
      with(probationPractitioner!!) {
        assertThat(staffCode).isEqualTo("AB012C")
        assertThat(name).isEqualTo("Delius User")
      }
    }
    assertThat(searchResults.pduCasesResponse).hasSize(0)

    verify(licenceCaseRepository, times(0)).findSubmittedVariationsByPduCodes(any())
    verify(licenceCaseRepository).findSubmittedVariationsByRegion(probationAreaCode)
  }

  @Test
  fun `should search for offenders for both a probation delivery unit and probation region`() {
    // Given
    val pdus = listOf("N55PDV")
    val probationAreaCode = "N01"
    val licenceSummaries = listOf(
      aLicenceVaryApproverCase(
        type = PSS,
      ),
    )
    val probationCases = listOf(aProbationCase())

    whenever(licenceCaseRepository.findSubmittedVariationsByPduCodes(pdus)).thenReturn(licenceSummaries)
    whenever(licenceCaseRepository.findSubmittedVariationsByRegion(probationAreaCode)).thenReturn(licenceSummaries)
    whenever(
      deliusApiClient.getProbationCases(
        licenceSummaries.map { it.prisonNumber!! },
      ),
    ).thenReturn(probationCases)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(probationCases.map { it.nomisId!! })).thenReturn(
      listOf(
        prisonerSearchResult().copy(prisonerNumber = aProbationCase().nomisId!!),
      ),
    )
    whenever(deliusApiClient.getOffenderManagersWithoutUser(licenceSummaries.map { it.prisonNumber!! })).thenReturn(
      listOf(aCommunityManagerWithoutUser()),
    )

    // When
    val searchResults =
      service.searchForOffenderOnVaryApproverCaseload(
        VaryApproverCaseloadSearchRequest(
          probationPduCodes = pdus,
          probationAreaCode = probationAreaCode,
          searchTerm = "A",
        ),
      )

    // Then
    assertThat(searchResults.pduCasesResponse).hasSize(1)
    with(searchResults.pduCasesResponse.first()) {
      assertThat(licenceId).isEqualTo(1)
      assertThat(name).isEqualTo("A Prisoner")
      assertThat(crnNumber).isEqualTo("X12348")
      assertThat(licenceType).isEqualTo(PSS)
      assertThat(variationRequestDate).isEqualTo(licenceSummaries.first().dateCreated?.toLocalDate())
      assertThat(releaseDate).isEqualTo(licenceSummaries.first().licenceStartDate)
      with(probationPractitioner!!) {
        assertThat(staffCode).isEqualTo("AB012C")
        assertThat(name).isEqualTo("Delius User")
      }
    }

    assertThat(searchResults.regionCasesResponse).hasSize(1)
    with(searchResults.regionCasesResponse.first()) {
      assertThat(licenceId).isEqualTo(1)
      assertThat(name).isEqualTo("A Prisoner")
      assertThat(crnNumber).isEqualTo("X12348")
      assertThat(licenceType).isEqualTo(PSS)
      assertThat(variationRequestDate).isEqualTo(licenceSummaries.first().dateCreated?.toLocalDate())
      assertThat(releaseDate).isEqualTo(licenceSummaries.first().licenceStartDate)
      with(probationPractitioner!!) {
        assertThat(staffCode).isEqualTo("AB012C")
        assertThat(name).isEqualTo("Delius User")
      }
    }

    verify(licenceCaseRepository).findSubmittedVariationsByPduCodes(pdus)
    verify(licenceCaseRepository).findSubmittedVariationsByRegion(probationAreaCode)
  }

  @Test
  fun `should return no results when search term does not match`() {
    // Given
    val pdus = listOf("N55PDV")
    val probationAreaCode = "N01"
    val licenceSummaries = listOf(aLicenceVaryApproverCase(type = PSS))
    val probationCases = listOf(aProbationCase())

    whenever(licenceCaseRepository.findSubmittedVariationsByPduCodes(pdus)).thenReturn(licenceSummaries)
    whenever(licenceCaseRepository.findSubmittedVariationsByRegion(probationAreaCode)).thenReturn(licenceSummaries)
    whenever(
      deliusApiClient.getProbationCases(
        licenceSummaries.map { it.prisonNumber!! },
      ),
    ).thenReturn(probationCases)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(probationCases.map { it.nomisId!! })).thenReturn(
      listOf(
        prisonerSearchResult().copy(prisonerNumber = aProbationCase().nomisId!!),
      ),
    )
    whenever(deliusApiClient.getOffenderManagersWithoutUser(licenceSummaries.map { it.prisonNumber!! })).thenReturn(
      listOf(aCommunityManagerWithoutUser()),
    )

    // When
    val searchResults =
      service.searchForOffenderOnVaryApproverCaseload(
        VaryApproverCaseloadSearchRequest(
          probationPduCodes = pdus,
          probationAreaCode = probationAreaCode,
          searchTerm = "XXXX",
        ),
      )

    // Then
    assertThat(searchResults.pduCasesResponse).isEmpty()
    assertThat(searchResults.regionCasesResponse).isEmpty()
  }

  fun aLicenceVaryApproverCase(
    id: Long = 1,
    type: LicenceType = LicenceType.AP_PSS,
    prisonNumber: String = "AB1234E",
    licenceStartDate: LocalDate = LocalDate.now().plusDays(10),
  ) = LicenceVaryApproverCase(
    licenceId = id,
    crn = "X12348",
    comUsername = "joebloggs",
    licenceStartDate = licenceStartDate,
    dateCreated = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
    prisonNumber = prisonNumber,
    typeCode = type,
  )

  fun aCommunityManagerWithoutUser() = TestData.aCommunityManagerWithoutUser().copy(
    case = ProbationCase(crn = "X12348", nomisId = "AB1234E"),
    name = Name("Delius", null, "User"),
    code = "AB012C",
  )
}
