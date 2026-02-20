package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.CaseAccessRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aCaseAccessResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.CaseAccessDetails
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.CaseAccessRestrictionType
import java.util.Optional

private const val USER_NAME = "a user"
private const val CRN = "X12348"

class PermissionsServiceTest {
  private val caseAccessResponse = aCaseAccessResponse(
    crn = CRN,
    restricted = false,
    excluded = false,
  )
  private val caseAccessDetails = CaseAccessDetails(CaseAccessRestrictionType.NONE)

  private val deliusApiClient = mock<DeliusApiClient>()
  private val licenceRepository = mock<LicenceRepository>()
  private val permissionsService = PermissionsService(deliusApiClient, licenceRepository)

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn(USER_NAME)
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    whenever(deliusApiClient.getCheckUserAccessForCRN(any(), any())).thenReturn(caseAccessResponse)
  }

  @Test
  fun `should check case access when a CRN is provided`() {
    val response = permissionsService.checkCaseAccess(CaseAccessRequest(crn = CRN))

    assertThat(response).isEqualTo(caseAccessDetails)
    verify(deliusApiClient).getCheckUserAccessForCRN(USER_NAME, CRN)
    verify(deliusApiClient, times(0)).getProbationCase(any())
    verify(licenceRepository, times(0)).findById(any())
  }

  @Test
  fun `should check case access when a nomis id is provided`() {
    val nomisId = "AB1234E"
    whenever(deliusApiClient.getProbationCase(nomisId)).thenReturn(ProbationCase(nomisId = nomisId, crn = CRN))

    val response = permissionsService.checkCaseAccess(CaseAccessRequest(nomisId = nomisId))

    assertThat(response).isEqualTo(caseAccessDetails)
    verify(deliusApiClient).getProbationCase(nomisId)
    verify(deliusApiClient).getCheckUserAccessForCRN(USER_NAME, CRN)
    verify(licenceRepository, times(0)).findById(any())
  }

  @Test
  fun `should check case access when a licence id is provided`() {
    val licenceId = 4L

    whenever(
      licenceRepository
        .findById(licenceId),
    ).thenReturn(Optional.of(createCrdLicence().copy(crn = CRN)))

    val response = permissionsService.checkCaseAccess(CaseAccessRequest(licenceId = licenceId))

    assertThat(response).isEqualTo(caseAccessDetails)
    verify(deliusApiClient).getCheckUserAccessForCRN(USER_NAME, CRN)
    verify(deliusApiClient, times(0)).getProbationCase(any())
    verify(licenceRepository).findById(licenceId)
  }

  @Test
  fun `should throw a validation exception for a bad request`() {
    val exception = assertThrows<ValidationException> {
      permissionsService.checkCaseAccess(CaseAccessRequest())
    }
    assertThat(exception.message).isEqualTo("crn, nomisId or licenceId must be provided")
  }
}
