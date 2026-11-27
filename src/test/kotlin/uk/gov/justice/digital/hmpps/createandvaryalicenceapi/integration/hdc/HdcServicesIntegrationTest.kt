package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.hdc

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions.HdcApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcEventService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceLinkingService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

class HdcServicesIntegrationTest : IntegrationTestBase() {
  @Autowired
  private lateinit var hdcEventService: HdcEventService

  @Autowired
  private lateinit var licenceLinkingService: LicenceLinkingService

  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
    "classpath:test_data/seed-hdc-conversion-history.sql",
    "classpath:test_data/seed-opted-in-link-from-crd.sql",
  )
  @Test
  fun `find previous HDC licence from an opted in CRD licence`() {
    // When
    val hdcLicenceId = licenceLinkingService.getHdcLicenceIdForHdcReplacement(1L)

    // Then
    assertThat(hdcLicenceId).isEqualTo(3L)
  }

  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
    "classpath:test_data/seed-hdc-conversion-history.sql",
    "classpath:test_data/seed-crd-replacement-link-ordering.sql",
  )
  @Test
  fun `find newest HDC licence linked to a CRD replacement`() {
    // When
    val latestRefusedHdc = licenceLinkingService.getHdcLicenceIdForCrdReplacement(1L)
    val latestOptedOutHdc = licenceLinkingService.getHdcLicenceIdForCrdReplacement(4L)
    val tiedTimestampsHdc = licenceLinkingService.getHdcLicenceIdForCrdReplacement(5L)

    // Then
    assertThat(latestRefusedHdc).isEqualTo(3L)
    assertThat(latestOptedOutHdc).isEqualTo(3L)
    assertThat(tiedTimestampsHdc).isEqualTo(7L)
  }

  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
    "classpath:test_data/seed-hdc-conversion-details.sql",
  )
  @Test
  fun `CRD to HDC conversion rolls back when HDC data is unavailable`() {
    // Given
    prisonerSearchMockServer.stubSearchPrisonersByNomisIdsNoResult()
    hdcApiMockServer.stubGetHdcLicenceDataNotFound(123L)

    // When
    assertThrows<EntityNotFoundException> {
      hdcEventService.convertToHdcLicence("A1234AA")
    }

    // Then
    val licences = testRepository.findAllLicence()
    assertThat(licences).hasSize(1)
    assertThat(licences.single().statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(testRepository.findAllHdcApplications()).isEmpty()
    assertThat(testRepository.findAllHdcCurfewAddresses()).isEmpty()
  }

  companion object {
    @RegisterExtension
    private val prisonerSearchMockServer = PrisonerSearchMockServer()

    @RegisterExtension
    private val hdcApiMockServer = HdcApiMockServer()
  }
}
