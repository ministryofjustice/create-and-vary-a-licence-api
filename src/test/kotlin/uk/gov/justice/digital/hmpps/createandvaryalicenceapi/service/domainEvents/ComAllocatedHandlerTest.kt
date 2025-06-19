package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateProbationTeamRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.OffenderService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.StaffService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.com
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.offenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient

class ComAllocatedHandlerTest {
  private val deliusApiClient = mock<DeliusApiClient>()
  private val offenderService = mock<OffenderService>()
  private val objectMapper = jacksonObjectMapper()
  private val staffService = mock<StaffService>()

  private val handler = ComAllocatedHandler(deliusApiClient, offenderService, objectMapper, staffService)

  @Test
  fun `should allocate a com to an offender`() {
    val crn = "X666322"
    val offenderManager = offenderManager()
    val com = com()

    whenever(deliusApiClient.getOffenderManager(crn)).thenReturn(offenderManager)
    whenever(staffService.updateComDetails(any())).thenReturn(com)

    handler.handleEvent(aComAllocatedEventMessage(crn))

    verify(deliusApiClient).assignDeliusRole(offenderManager.username?.trim()?.uppercase()!!)
    verify(staffService).updateComDetails(
      UpdateComRequest(
        staffIdentifier = offenderManager.id,
        staffUsername = offenderManager.username!!,
        staffEmail = offenderManager.email,
        firstName = offenderManager.name.forename,
        lastName = offenderManager.name.forename,
      ),
    )

    verify(offenderService).updateOffenderWithResponsibleCom(crn, com)

    verify(offenderService).updateProbationTeam(
      crn,
      UpdateProbationTeamRequest(
        probationAreaCode = offenderManager.provider.code,
        probationAreaDescription = offenderManager.provider.description,
        probationPduCode = offenderManager.team.borough.code,
        probationPduDescription = offenderManager.team.borough.description,
        probationLauCode = offenderManager.team.district.code,
        probationLauDescription = offenderManager.team.district.description,
        probationTeamCode = offenderManager.team.code,
        probationTeamDescription = offenderManager.team.description,
      ),
    )
  }

  @Test
  fun `should stop processing if offender manager can't be found`() {
    val crn = "X696325"

    whenever(deliusApiClient.getOffenderManager(crn)).thenReturn(null)

    handler.handleEvent(aComAllocatedEventMessage(crn))

    verify(deliusApiClient, never()).assignDeliusRole(any())
    verifyNoInteractions(staffService)
    verifyNoInteractions(offenderService)
  }

  private fun aComAllocatedEventMessage(crn: String) = jacksonObjectMapper().writeValueAsString(
    HMPPSDomainEvent(
      eventType = COM_ALLOCATED_EVENT_TYPE,
      version = 0,
      occurredAt = "2023-12-05T00:00:00Z",
      personReference = PersonReference(
        listOf(Identifiers("CRN", crn)),
      ),
    ),
  )
}
