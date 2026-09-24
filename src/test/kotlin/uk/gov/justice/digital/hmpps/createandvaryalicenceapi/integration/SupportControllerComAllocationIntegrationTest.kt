package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions.DeliusMockServer

class SupportControllerComAllocationIntegrationTest : IntegrationTestBase() {

  /**
   * Reproduces: "Multiple representations of the same entity [...CommunityOffenderManager with id '...']
   * are being merged".
   *
   * This requires a CRN with two (or more) in-flight licences, both currently assigned to a COM other
   * than the one Delius returns, so that `OffenderService.updateResponsibleCom` reassigns both licences
   * to the same, newly-created `CommunityOffenderManager`. If `StaffService.updateComDetails` and
   * `OffenderService.updateResponsibleCom` run in separate transactions (i.e. the calling method,
   * here `ComAllocatedHandler.syncComAllocation`, is not itself `@Transactional`), the new COM is detached
   * by the time it is cascaded onto the second licence, causing the Hibernate merge conflict.
   */
  @Test
  @Sql(
    "classpath:test_data/seed-two-licences-same-crn.sql",
  )
  fun `Sync COM allocation for a CRN with multiple in-flight licences does not throw a Hibernate merge conflict`() {
    // Given
    val crn = "X999999"
    val staffCode = "sc_BBB"
    val userName = "BBB"
    val emailAddress = "testBBB@probation.gov.uk"
    val staffIdentifier = 126L
    val firstName = "Com"
    val lastName = "BBB"

    deliusMockServer.stubGetOffenderManager(
      crn = crn,
      staffCode = staffCode,
      userName = userName,
      emailAddress = emailAddress,
      staffIdentifier = staffIdentifier,
      firstName = firstName,
      lastName = lastName,
    )
    deliusMockServer.stubAssignDeliusRole(userName = userName.uppercase())

    val licencesBefore = testRepository.findAllLicence().filter { it.crn == crn }
    assertThat(licencesBefore).hasSize(2)

    // When
    webTestClient.put()
      .uri("/offender/sync-com/crn/$crn")
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    // Then both licences are reassigned to the same, newly-allocated COM
    val licencesAfter = testRepository.findAllLicence().filter { it.crn == crn }
    assertThat(licencesAfter).hasSize(2)
    licencesAfter.forEach {
      val responsibleCom = it.responsibleCom as CommunityOffenderManager?
      assertThat(responsibleCom?.staffIdentifier).isEqualTo(staffIdentifier)
      assertThat(responsibleCom?.username).isEqualTo(userName.uppercase())
    }
  }

  private companion object {
    @RegisterExtension
    val deliusMockServer = DeliusMockServer()
  }
}
