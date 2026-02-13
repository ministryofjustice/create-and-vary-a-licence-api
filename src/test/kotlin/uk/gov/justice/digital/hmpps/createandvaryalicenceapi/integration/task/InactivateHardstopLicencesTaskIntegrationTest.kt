package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.task

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PotentialHardstopCaseStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.PotentialHardstopCaseRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.task.InactivateHardstopLicencesTask
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

class InactivateHardstopLicencesTaskIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var auditEventRepository: AuditEventRepository

  @Autowired
  lateinit var inactivateHardstopLicencesTask: InactivateHardstopLicencesTask

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Autowired
  lateinit var licenceEventRepository: LicenceEventRepository

  @Autowired
  lateinit var potentialHardstopCaseRepository: PotentialHardstopCaseRepository

  @Test
  @Sql(
    "classpath:test_data/seed-potential-hard-stop-case.sql",
  )
  fun `Run inactivate hard stop task`() {
    inactivateHardstopLicencesTask.runTask()

    assertThat(licenceRepository.count()).isEqualTo(1)
    assertThat(licenceEventRepository.count()).isEqualTo(1)

    val licence = licenceRepository.findAll().first()
    assertThat(licence.statusCode).isEqualTo(LicenceStatus.INACTIVE)

    val potentialHardstopCase = potentialHardstopCaseRepository.findAll().first()
    assertThat(potentialHardstopCase.status).isEqualTo(PotentialHardstopCaseStatus.PROCESSED)
  }

  private companion object {
    val govUkApiMockServer = GovUkMockServer()
    val prisonerSearchMockServer = PrisonerSearchMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkApiMockServer.start()
      prisonerSearchMockServer.start()
      prisonerSearchMockServer.stubSearchPrisonersByNomisIds()
      govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      govUkApiMockServer.stop()
      prisonerSearchMockServer.stop()
    }
  }
}
