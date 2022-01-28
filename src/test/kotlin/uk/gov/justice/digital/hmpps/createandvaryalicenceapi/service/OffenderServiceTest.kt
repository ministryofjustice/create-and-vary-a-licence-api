package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateResponsibleComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

class OffenderServiceTest {
  private val licenceRepository = mock<LicenceRepository>()

  private val service = OffenderService(licenceRepository)

  @BeforeEach
  fun reset() {
    reset(licenceRepository)
  }

  @Test
  fun `updates all in-flight licences associated with an offender with COM details`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(Licence()))

    val comDetails = UpdateResponsibleComRequest(
      staffIdentifier = 2000,
      staffUsername = "joebloggs",
      staffEmail = "jbloggs@probation.gov.uk"
    )

    val expectedUpdatedLicences = listOf(
      Licence(comStaffId = 2000, comUsername = "joebloggs", comEmail = "jbloggs@probation.gov.uk")
    )

    service.updateOffenderWithResponsibleCom("exampleCrn", comDetails)

    verify(licenceRepository, times(1))
      .findAllByCrnAndStatusCodeIn("exampleCrn",
        listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED, LicenceStatus.APPROVED, LicenceStatus.ACTIVE))
    verify(licenceRepository, times(1))
      .saveAllAndFlush(expectedUpdatedLicences)
  }
}
