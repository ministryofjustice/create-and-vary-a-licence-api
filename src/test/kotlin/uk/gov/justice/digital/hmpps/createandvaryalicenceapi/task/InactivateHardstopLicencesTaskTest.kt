package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.task

import jakarta.persistence.LockTimeoutException
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PotentialHardstopCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PotentialHardstopCaseStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.PotentialHardstopCaseRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.UpdateSentenceDateService.Companion.LICENCE_DEACTIVATION_HARD_STOP_TASK
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService

class InactivateHardstopLicencesTaskTest {
  private val licenceService = mock<LicenceService>()

  private val potentialHardstopCaseRepository = mock<PotentialHardstopCaseRepository>()

  private val releaseDateService = mock<ReleaseDateService>()

  private val inactivateHardstopLicencesTask =
    InactivateHardstopLicencesTask(licenceService, potentialHardstopCaseRepository, releaseDateService)

  @Test
  fun `should inactivate licences no longer in hard stop period`() {
    val licence1 = createHardStopLicence()
    val licence2 = licence1.copy(id = licence1.id + 1, licenceStartDate = licence1.licenceStartDate?.plusDays(6))

    val potentialHardstopCase1 = PotentialHardstopCase(licence1, PotentialHardstopCaseStatus.PENDING)
    val potentialHardstopCase2 = PotentialHardstopCase(licence2, PotentialHardstopCaseStatus.PENDING)

    whenever(
      potentialHardstopCaseRepository.findAllByStatusAndDateCreatedBefore(
        eq(PotentialHardstopCaseStatus.PENDING),
        any(),
      ),
    ).thenReturn(
      listOf(
        potentialHardstopCase1,
        potentialHardstopCase2,
      ),
    )
    whenever(releaseDateService.isInHardStopPeriod(licence1.licenceStartDate, licence1.kind)).thenReturn(true)
    whenever(releaseDateService.isInHardStopPeriod(licence2.licenceStartDate, licence2.kind)).thenReturn(false)
    inactivateHardstopLicencesTask.runTask()

    verify(
      potentialHardstopCaseRepository,
    ).findAllByStatusAndDateCreatedBefore(
      eq(PotentialHardstopCaseStatus.PENDING),
      any(),
    )

    verify(licenceService, times(0)).inactivateLicences(listOf(licence1), LICENCE_DEACTIVATION_HARD_STOP_TASK)
    verify(licenceService).inactivateLicences(listOf(licence2), LICENCE_DEACTIVATION_HARD_STOP_TASK)

    verify(potentialHardstopCaseRepository).save(potentialHardstopCase1)
    verify(potentialHardstopCaseRepository).save(potentialHardstopCase2)
  }

  @Test
  fun `should not inactivate licences if lock exception is thrown`() {
    whenever(
      potentialHardstopCaseRepository.findAllByStatusAndDateCreatedBefore(
        eq(PotentialHardstopCaseStatus.PENDING),
        any(),
      ),
    ).thenThrow(LockTimeoutException("timed out!"))

    inactivateHardstopLicencesTask.runTask()

    verify(potentialHardstopCaseRepository).findAllByStatusAndDateCreatedBefore(
      eq(PotentialHardstopCaseStatus.PENDING),
      any(),
    )
    verify(potentialHardstopCaseRepository, times(0)).save(any())
    verifyNoInteractions(licenceService)
  }
}
