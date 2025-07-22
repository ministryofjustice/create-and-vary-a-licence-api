package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Staff
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.DeliusMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComReviewCount
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdatePrisonUserRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.StaffKind
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future

class StaffIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var staffRepository: StaffRepository

  @Test
  fun `Add a COM record`() {
    // Given
    val request = UpdateComRequest(
      staffIdentifier = 2001L,
      staffUsername = "newbloggs",
      staffEmail = "newbloggs@probation.gov.uk",
      firstName = "New",
      lastName = "NewBloggs",
    )

    // When
    doUpdate("/com/update", request)

    // Then
    val staff = staffRepository.findByStaffIdentifier(2001)
    assertThat(staff).isNotNull
    assertComDetails(staff!!, request)
  }

  @Test
  fun `Update a COM record`() {
    // Given
    val request = UpdateComRequest(
      staffIdentifier = 2000L,
      staffUsername = "upatebloggs",
      staffEmail = "updatebloggs@probation.gov.uk",
      firstName = "Updated",
      lastName = "Bloggs",
    )

    // When
    doUpdate("/com/update", request)

    // Then
    val staff = staffRepository.findByStaffIdentifier(2000)
    assertThat(staff).isNotNull
    assertComDetails(staff!!, request)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-community-offender-manager-with-duplicates.sql",
  )
  fun `Update latest COM when multiple staff records are found with same user name and unknown staffIdentifier in different cases`() {
    // Given
    val unknownStaffIdentifier = 1001L
    val request = UpdateComRequest(
      staffIdentifier = unknownStaffIdentifier,
      staffUsername = "test-client-1",
      staffEmail = "updatebloggs@probation.gov.uk",
      firstName = "Updated",
      lastName = "Bloggs",
    )

    // When
    doUpdate("/com/update", request)

    // Then
    val staff = staffRepository.findByStaffIdentifier(unknownStaffIdentifier)
    assertThat(staff).isNotNull
    assertComDetails(staff!!, request)
  }

  // This will fail randomly due more than one thread finding no existing COM and so
  // attempting to create one, any threads attempting to save a COM after the first
  // will cause a Unique index or primary key violation
  @Disabled
  @Test
  fun `Test adding the same COM in multiple threads`() {
    val numberOfThreads = 5
    val executor = Executors.newFixedThreadPool(numberOfThreads)
    val countdownLatch = CountDownLatch(numberOfThreads)

    val futures = mutableListOf<Future<*>>()
    for (i in 1..numberOfThreads) {
      futures.add(
        executor.submit {
          try {
            doUpdate("/com/update", updateCom)
          } finally {
            countdownLatch.countDown()
          }
        },
      )
    }
    countdownLatch.await()
    futures.forEach {
      try {
        it.get()
      } catch (e: ExecutionException) {
        fail("Update com failed: $e")
      }
    }

    assertThat(staffRepository.count()).isEqualTo(1)

    with(staffRepository.findAll().first() as CommunityOffenderManager) {
      assertThat(staffIdentifier).isEqualTo(updateCom.staffIdentifier)
      assertThat(username).isEqualTo(updateCom.staffUsername.uppercase())
      assertThat(email).isEqualTo(updateCom.staffEmail)
      assertThat(firstName).isEqualTo(updateCom.firstName)
      assertThat(lastName).isEqualTo(updateCom.lastName)
    }
  }

  @Test
  @Sql(
    "classpath:test_data/clear-all-data.sql",
  )
  fun `Add and update a Prison Case Administrator record`() {

    doUpdate("/prison-user/update", updatePrison.copy(
      staffUsername = updatePrison.staffUsername.lowercase()))

    assertThat(staffRepository.count()).isEqualTo(1)

    with(staffRepository.findAll().first() as PrisonUser) {
      assertThat(username).isEqualTo(updatePrison.staffUsername.uppercase())
      assertThat(email).isEqualTo(updatePrison.staffEmail)
      assertThat(firstName).isEqualTo(updatePrison.firstName)
      assertThat(lastName).isEqualTo(updatePrison.lastName)
    }

    doUpdate("/prison-user/update", updatePrison.copy(
      firstName = "NEW NAME" ,
      staffUsername = updatePrison.staffUsername.lowercase()))

    assertThat(staffRepository.count()).isEqualTo(1)

    with(staffRepository.findAll().first() as PrisonUser) {
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
    doUpdate("/prison-user/update", updatePrison.copy(staffUsername = "A CA"))
    doUpdate("/prison-user/update", updatePrison.copy(staffUsername = "DIFFERENT CA"))

    assertThat(staffRepository.count()).isEqualTo(4)

    val staffByType = staffRepository.findAll().groupBy({ it::class }, { it.username })
    assertThat(staffByType).containsEntry(
      CommunityOffenderManager::class,
      listOf("A COM", "DIFFERENT COM"),
    )
    assertThat(staffByType).containsEntry(
      PrisonUser::class,
      listOf("A CA", "DIFFERENT CA"),
    )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-prison-case-administrator.sql",
    "classpath:test_data/seed-hard-stop-licences.sql",
  )
  fun `Get counts of cases needing a review`() {
    deliusMockServer.stubGetTeamCodesForUser(2000)

    val resultObject = webTestClient.get()
      .uri("/com/2000/review-counts")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ComReviewCount::class.java)
      .returnResult().responseBody

    val teamCount = resultObject!!.teams.first()

    assertThat(resultObject.myCount).isEqualTo(2)
    assertThat(teamCount.teamCode).isEqualTo("A01B02")
    assertThat(teamCount.count).isEqualTo(2)
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

  private fun assertComDetails(
    staff: Staff,
    request: UpdateComRequest,
  ) {
    assertThat(staff).isInstanceOf(CommunityOffenderManager::class.java)
    val com = staff as CommunityOffenderManager
    assertThat(com.kind).isEqualTo(StaffKind.COMMUNITY_OFFENDER_MANAGER)
    assertThat(com.staffIdentifier).isEqualTo(request.staffIdentifier)
    assertThat(com.username).isEqualTo(request.staffUsername.uppercase())
    assertThat(com.email).isEqualTo(request.staffEmail)
    assertThat(com.firstName).isEqualTo(request.firstName)
    assertThat(com.lastName).isEqualTo(request.lastName)
  }

  private companion object {
    val deliusMockServer = DeliusMockServer()

    val updateCom = UpdateComRequest(
      staffIdentifier = 2000,
      staffUsername = "joebloggs",
      staffEmail = "joebloggs@probation.gov.uk",
      firstName = "Joseph",
      lastName = "Bloggs",
    )
    val updatePrison = UpdatePrisonUserRequest(
      staffUsername = "boejoggs",
      staffEmail = "boejoggs@prison.gov.uk",
      firstName = "Boseph",
      lastName = "Joggs",
    )

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      deliusMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      deliusMockServer.stop()
    }
  }
}
