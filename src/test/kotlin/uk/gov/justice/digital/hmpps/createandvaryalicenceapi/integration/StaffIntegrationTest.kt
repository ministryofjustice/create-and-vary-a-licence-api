package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonCaseAdministrator
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdatePrisonCaseAdminRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository

class StaffIntegrationTest : IntegrationTestBase() {
  @Autowired
  lateinit var staffRepository: StaffRepository

  @Test
  @Sql(
    "classpath:test_data/clear-all-data.sql",
  )
  fun `Add and update a COM record`() {
    doUpdate("/com/update", updateCom)

    assertThat(staffRepository.count()).isEqualTo(1)

    with(staffRepository.findAll().first() as CommunityOffenderManager) {
      assertThat(staffIdentifier).isEqualTo(updateCom.staffIdentifier)
      assertThat(username).isEqualTo(updateCom.staffUsername.uppercase())
      assertThat(email).isEqualTo(updateCom.staffEmail)
      assertThat(firstName).isEqualTo(updateCom.firstName)
      assertThat(lastName).isEqualTo(updateCom.lastName)
    }

    doUpdate("/com/update", updateCom.copy(firstName = "NEW NAME"))

    assertThat(staffRepository.count()).isEqualTo(1)

    with(staffRepository.findAll().first() as CommunityOffenderManager) {
      assertThat(staffIdentifier).isEqualTo(updateCom.staffIdentifier)
      assertThat(username).isEqualTo(updateCom.staffUsername.uppercase())
      assertThat(email).isEqualTo(updateCom.staffEmail)
      assertThat(firstName).isEqualTo("NEW NAME")
      assertThat(lastName).isEqualTo(updateCom.lastName)
    }
  }

  @Test
  @Sql(
    "classpath:test_data/clear-all-data.sql",
  )
  fun `Add and update a Prison Case Administrator record`() {
    doUpdate("/prison-case-administrator/update", updatePrison)

    assertThat(staffRepository.count()).isEqualTo(1)

    with(staffRepository.findAll().first() as PrisonCaseAdministrator) {
      assertThat(username).isEqualTo(updatePrison.staffUsername.uppercase())
      assertThat(email).isEqualTo(updatePrison.staffEmail)
      assertThat(firstName).isEqualTo(updatePrison.firstName)
      assertThat(lastName).isEqualTo(updatePrison.lastName)
    }

    doUpdate("/prison-case-administrator/update", updatePrison.copy(firstName = "NEW NAME"))

    assertThat(staffRepository.count()).isEqualTo(1)

    with(staffRepository.findAll().first() as PrisonCaseAdministrator) {
      assertThat(username).isEqualTo(updatePrison.staffUsername.uppercase())
      assertThat(email).isEqualTo(updatePrison.staffEmail)
      assertThat(firstName).isEqualTo("NEW NAME")
      assertThat(lastName).isEqualTo(updatePrison.lastName)
    }
  }

  @Test
  @Sql(
    "classpath:test_data/clear-all-data.sql",
  )
  fun `Persist multiple records`() {
    doUpdate("/com/update", updateCom.copy(staffIdentifier = 1, staffUsername = "A COM"))
    doUpdate("/com/update", updateCom.copy(staffIdentifier = 2, staffUsername = "DIFFERENT COM"))
    doUpdate("/prison-case-administrator/update", updatePrison.copy(staffUsername = "A CA"))
    doUpdate("/prison-case-administrator/update", updatePrison.copy(staffUsername = "DIFFERENT CA"))

    assertThat(staffRepository.count()).isEqualTo(4)

    val staffByType = staffRepository.findAll().groupBy({ it::class }, { it.username })
    assertThat(staffByType).containsEntry(
      CommunityOffenderManager::class,
      listOf("A COM", "DIFFERENT COM"),
    )
    assertThat(staffByType).containsEntry(
      PrisonCaseAdministrator::class,
      listOf("A CA", "DIFFERENT CA"),
    )
  }

  private fun doUpdate(uri: String, body: Any) {
    webTestClient.put()
      .uri(uri)
      .bodyValue(body)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
  }

  private companion object {
    val updateCom = UpdateComRequest(
      staffIdentifier = 2000,
      staffUsername = "joebloggs",
      staffEmail = "joebloggs@probation.gov.uk",
      firstName = "Joseph",
      lastName = "Bloggs",
    )
    val updatePrison = UpdatePrisonCaseAdminRequest(
      staffUsername = "boejoggs",
      staffEmail = "boejoggs@prison.gov.uk",
      firstName = "Boseph",
      lastName = "Joggs",
    )
  }
}
