package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentPersonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentTimeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ContactNumberRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StatusUpdateRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateAdditionalConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateStandardConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.AddAdditionalConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.MatchLicencesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.NotifyRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.RecentlyApprovedLicencesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ReferVariationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdatePrisonInformationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateReasonForVariationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSentenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSpoDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateVloDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceConditionService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.UpdateSentenceDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [LicenceController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [LicenceController::class])
@WebAppConfiguration
class LicenceControllerTest {

  @MockBean
  private lateinit var licenceService: LicenceService

  @MockBean
  private lateinit var updateSentenceDateService: UpdateSentenceDateService

  @MockBean
  private lateinit var licenceConditionService: LicenceConditionService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(licenceService)

    mvc = MockMvcBuilders
      .standaloneSetup(LicenceController(licenceService, updateSentenceDateService, licenceConditionService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `get a licence by id`() {
    whenever(licenceService.getLicenceById(1)).thenReturn(aLicence)

    val result = mvc.perform(get("/licence/id/1").accept(APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString)
      .isEqualTo(mapper.writeValueAsString(aLicence))

    verify(licenceService, times(1)).getLicenceById(1)
  }

  @Test
  fun `get variation submitted for region`() {
    whenever(licenceService.findSubmittedVariationsByRegion("N01A")).thenReturn(listOf(aLicenceSummary))

    val result = mvc.perform(get("/licence/variations/submitted/area/N01A").accept(APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString)
      .isEqualTo(mapper.writeValueAsString(listOf(aLicenceSummary)))

    verify(licenceService, times(1)).findSubmittedVariationsByRegion("N01A")
  }

  @Test
  fun `404 licence not found`() {
    whenever(licenceService.getLicenceById(1)).thenThrow(EntityNotFoundException(""))

    val result = mvc.perform(get("/licence/id/1").accept(APPLICATION_JSON))
      .andExpect(status().isNotFound)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString).contains("Not found")

    verify(licenceService, times(1)).getLicenceById(1)
  }

  @Test
  fun `create a licence`() {
    whenever(licenceService.createLicence(aCreateLicenceRequest)).thenReturn(aLicenceSummary)

    val result = mvc.perform(
      post("/licence/create")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(aCreateLicenceRequest)),
    )
      .andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString).isEqualTo(mapper.writeValueAsString(aLicenceSummary))

    verify(licenceService, times(1)).createLicence(aCreateLicenceRequest)
  }

  @Test
  fun `create a licence where another is in progress`() {
    whenever(licenceService.createLicence(aCreateLicenceRequest))
      .thenThrow(ValidationException("A licence already exists for this person"))

    val result = mvc.perform(
      post("/licence/create")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(aCreateLicenceRequest)),
    )
      .andExpect(status().isBadRequest)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString).contains("A licence already exists for this person")

    verify(licenceService, times(1)).createLicence(aCreateLicenceRequest)
  }

  @Test
  fun `update initial appointment person`() {
    mvc.perform(
      put("/licence/id/4/appointmentPerson")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(anUpdateAppointmentPersonRequest)),
    )
      .andExpect(status().isOk)

    verify(licenceService, times(1)).updateAppointmentPerson(4, anUpdateAppointmentPersonRequest)
  }

  @Test
  fun `update initial appointment person - invalid request body`() {
    mvc.perform(
      put("/licence/id/4/appointmentPerson")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes({ })),
    )
      .andExpect(status().isBadRequest)
      .andExpect(content().contentType(APPLICATION_JSON))
  }

  @Test
  fun `update initial appointment time`() {
    mvc.perform(
      put("/licence/id/4/appointmentTime")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(anAppointmentTimeRequest)),
    )
      .andExpect(status().isOk)

    verify(licenceService, times(1)).updateAppointmentTime(4, anAppointmentTimeRequest)
  }

  @Test
  fun `update appointment time - no date specified`() {
    mvc.perform(
      put("/licence/id/4/appointmentTime")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes({})),
    )
      .andExpect(status().isBadRequest)
      .andExpect(content().contentType(APPLICATION_JSON))
  }

  @Test
  fun `update initial appointment time - lower precision datetime`() {
    mvc.perform(
      put("/licence/id/4/appointmentTime")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(anAppointmentTimeRequestDateOnly)),
    )
      .andExpect(status().isOk)

    verify(licenceService, times(1)).updateAppointmentTime(4, anAppointmentTimeRequestDateOnly)
  }

  @Test
  fun `update officer contact number`() {
    mvc.perform(
      put("/licence/id/4/contact-number")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(aContactNumberRequest)),
    )
      .andExpect(status().isOk)

    verify(licenceService, times(1)).updateContactNumber(4, aContactNumberRequest)
  }

  @Test
  fun `update officer contact number - invalid request body`() {
    mvc.perform(
      put("/licence/id/4/contact-number")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes({ })),
    )
      .andExpect(status().isBadRequest)
      .andExpect(content().contentType(APPLICATION_JSON))
  }

  @Test
  fun `update appointment address`() {
    mvc.perform(
      put("/licence/id/4/appointment-address")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(anAppointmentAddressRequest)),
    )
      .andExpect(status().isOk)

    verify(licenceService, times(1)).updateAppointmentAddress(4, anAppointmentAddressRequest)
  }

  @Test
  fun `update appointment address - invalid request body`() {
    mvc.perform(
      put("/licence/id/4/appointment-address")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes({ })),
    )
      .andExpect(status().isBadRequest)
      .andExpect(content().contentType(APPLICATION_JSON))
  }

  @Test
  fun `update bespoke conditions`() {
    mvc.perform(
      put("/licence/id/4/bespoke-conditions")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(aBespokeConditionsRequest)),
    )
      .andExpect(status().isOk)

    verify(licenceConditionService, times(1)).updateBespokeConditions(4, aBespokeConditionsRequest)
  }

  @Test
  fun `update bespoke conditions with an empty request removes previous bespoke conditions`() {
    mvc.perform(
      put("/licence/id/4/bespoke-conditions")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(BespokeConditionRequest())),
    )
      .andExpect(status().isOk)

    verify(licenceConditionService, times(1)).updateBespokeConditions(4, BespokeConditionRequest())
  }

  @Test
  fun `Add additional condition`() {
    mvc.perform(
      post("/licence/id/4/additional-condition/AP")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(anAddAdditionalConditionRequest)),
    )
      .andExpect(status().isOk)

    verify(licenceConditionService, times(1)).addAdditionalCondition(4, anAddAdditionalConditionRequest)
  }

  @Test
  fun `Delete additional condition`() {
    mvc.perform(
      delete("/licence/id/4/additional-condition/id/1")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON),
    )
      .andExpect(status().isNoContent)

    verify(licenceConditionService, times(1)).deleteAdditionalCondition(4, 1)
  }

  @Test
  fun `match licences by prison code and status`() {
    val licenceQueryObject = LicenceQueryObject(
      prisonCodes = listOf("LEI"),
      statusCodes = listOf(LicenceStatus.APPROVED),
    )
    whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(listOf(aLicenceSummary))

    val result = mvc.perform(
      post("/licence/match")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(
          mapper.writeValueAsBytes(
            MatchLicencesRequest(
              prison = listOf("LEI"),
              status = listOf(LicenceStatus.APPROVED),
            ),
          ),
        ),
    )
      .andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString)
      .isEqualTo(mapper.writeValueAsString(listOf(aLicenceSummary)))

    verify(licenceService, times(1)).findLicencesMatchingCriteria(licenceQueryObject)
  }

  @Test
  fun `match licences by staffId and status`() {
    val licenceQueryObject = LicenceQueryObject(
      staffIds = listOf(1, 2, 3),
      statusCodes = listOf(LicenceStatus.APPROVED, LicenceStatus.ACTIVE),
    )

    whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(listOf(aLicenceSummary))

    val result = mvc.perform(
      post("/licence/match")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(
          mapper.writeValueAsBytes(
            MatchLicencesRequest(
              staffId = listOf(1, 2, 3),
              status = listOf(LicenceStatus.APPROVED, LicenceStatus.ACTIVE),
            ),
          ),
        ),
    )
      .andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString)
      .isEqualTo(mapper.writeValueAsString(listOf(aLicenceSummary)))

    verify(licenceService, times(1)).findLicencesMatchingCriteria(licenceQueryObject)
  }

  @Test
  fun `match licences by pdu and status`() {
    val licenceQueryObject = LicenceQueryObject(
      pdus = listOf("A", "B", "C"),
      statusCodes = listOf(LicenceStatus.APPROVED, LicenceStatus.ACTIVE),
    )

    whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(listOf(aLicenceSummary))

    val result = mvc.perform(
      post("/licence/match")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(
          mapper.writeValueAsBytes(
            MatchLicencesRequest(
              status = listOf(LicenceStatus.APPROVED, LicenceStatus.ACTIVE),
              pdu = listOf("A", "B", "C"),
            ),
          ),
        ),
    )
      .andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString)
      .isEqualTo(mapper.writeValueAsString(listOf(aLicenceSummary)))

    verify(licenceService, times(1)).findLicencesMatchingCriteria(licenceQueryObject)
  }

  @Test
  fun `get recently approved licences by prison code`() {
    val prisonCodes = listOf("LEI")

    whenever(licenceService.findRecentlyApprovedLicences(any())).thenReturn(
      listOf(
        aLicenceSummary,
      ),
    )

    val result = mvc.perform(
      post("/licence/recently-approved")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(
          mapper.writeValueAsBytes(
            RecentlyApprovedLicencesRequest(
              prisonCodes = prisonCodes,
            ),
          ),
        ),
    )
      .andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString)
      .isEqualTo(mapper.writeValueAsString(listOf(aLicenceSummary)))

    verify(licenceService, times(1)).findRecentlyApprovedLicences(prisonCodes)
  }

  @Test
  fun `update licence status`() {
    mvc.perform(
      put("/licence/id/4/status")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(aStatusUpdateRequest)),
    )
      .andExpect(status().isOk)

    verify(licenceService, times(1)).updateLicenceStatus(4, aStatusUpdateRequest)
  }

  @Test
  fun `update the list of additional conditions`() {
    mvc.perform(
      put("/licence/id/4/additional-conditions")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(anUpdateAdditionalConditionsListRequest)),
    )
      .andExpect(status().isOk)

    verify(licenceConditionService, times(1)).updateAdditionalConditions(4, anUpdateAdditionalConditionsListRequest)
  }

  @Test
  fun `update the list of standard conditions`() {
    mvc.perform(
      put("/licence/id/4/standard-conditions")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(anUpdateStandardConditionRequest)),
    )
      .andExpect(status().isOk)

    verify(licenceConditionService, times(1)).updateStandardConditions(4, anUpdateStandardConditionRequest)
  }

  @Test
  fun `update the data associated with an additional condition`() {
    mvc.perform(
      put("/licence/id/4/additional-conditions/condition/1")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(anUpdateAdditionalConditionsDataRequest)),
    )
      .andExpect(status().isOk)

    verify(licenceConditionService, times(1)).updateAdditionalConditionData(
      4,
      1,
      anUpdateAdditionalConditionsDataRequest,
    )
  }

  @Test
  fun `submit a licence`() {
    mvc.perform(
      put("/licence/id/4/submit")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(listOf(NotifyRequest("testName", "testEmail")))),
    )
      .andExpect(status().isOk)

    verify(licenceService, times(1)).submitLicence(4, listOf(NotifyRequest("testName", "testEmail")))
  }

  @Test
  fun `create variation`() {
    whenever(licenceService.createVariation(4L)).thenReturn(aLicenceSummary)

    mvc.perform(
      post("/licence/id/4/create-variation")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON),
    )
      .andExpect(status().isOk)

    verify(licenceService, times(1)).createVariation(4)
  }

  @Test
  fun `edit a licence`() {
    val licenceId = 42L
    whenever(licenceService.editLicence(licenceId)).thenReturn(aLicenceSummary)

    mvc.perform(
      post("/licence/id/$licenceId/edit")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON),
    )
      .andExpect(status().isOk)

    verify(licenceService).editLicence(licenceId)
  }

  @Test
  fun `update spo discussion`() {
    mvc.perform(
      put("/licence/id/4/spo-discussion")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(UpdateSpoDiscussionRequest(spoDiscussion = "Yes"))),
    )
      .andExpect(status().isOk)

    verify(licenceService, times(1)).updateSpoDiscussion(4, UpdateSpoDiscussionRequest(spoDiscussion = "Yes"))
  }

  @Test
  fun `update vlo discussion`() {
    mvc.perform(
      put("/licence/id/4/vlo-discussion")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(UpdateVloDiscussionRequest(vloDiscussion = "Yes"))),
    )
      .andExpect(status().isOk)

    verify(licenceService, times(1)).updateVloDiscussion(4, UpdateVloDiscussionRequest(vloDiscussion = "Yes"))
  }

  @Test
  fun `update reason for variation`() {
    mvc.perform(
      put("/licence/id/4/reason-for-variation")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(UpdateReasonForVariationRequest(reasonForVariation = "reason"))),
    )
      .andExpect(status().isOk)

    verify(licenceService, times(1)).updateReasonForVariation(
      4,
      UpdateReasonForVariationRequest(reasonForVariation = "reason"),
    )
  }

  @Test
  fun `discard a licence`() {
    mvc.perform(
      delete("/licence/id/4/discard")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON),
    )
      .andExpect(status().isOk)

    verify(licenceService, times(1)).discardLicence(4)
  }

  @Test
  fun `update prison information`() {
    val expectedRequest = UpdatePrisonInformationRequest(
      prisonCode = "PVI",
      prisonDescription = "Pentonville (HMP)",
      prisonTelephone = "+44 276 54545",
    )

    mvc.perform(
      put("/licence/id/4/prison-information")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(expectedRequest)),
    )
      .andExpect(status().isOk)

    verify(licenceService, times(1)).updatePrisonInformation(4, expectedRequest)
  }

  @Test
  fun `update sentence dates`() {
    val expectedRequest = UpdateSentenceDatesRequest(
      conditionalReleaseDate = LocalDate.parse("2023-09-11"),
      actualReleaseDate = LocalDate.parse("2023-09-11"),
      sentenceStartDate = LocalDate.parse("2021-09-11"),
      sentenceEndDate = LocalDate.parse("2024-09-11"),
      licenceStartDate = LocalDate.parse("2023-09-11"),
      licenceExpiryDate = LocalDate.parse("2024-09-11"),
      topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
      topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
    )

    mvc.perform(
      put("/licence/id/4/sentence-dates")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(expectedRequest)),
    )
      .andExpect(status().isOk)

    verify(updateSentenceDateService, times(1)).updateSentenceDates(4, expectedRequest)
  }

  @Test
  fun `approve a variation`() {
    mvc.perform(
      put("/licence/id/4/approve-variation")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON),
    )
      .andExpect(status().isOk)

    verify(licenceService, times(1)).approveLicenceVariation(4)
  }

  @Test
  fun `refer a variation`() {
    val expectedRequest = ReferVariationRequest(reasonForReferral = "Reason")

    mvc.perform(
      put("/licence/id/4/refer-variation")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(expectedRequest)),
    )
      .andExpect(status().isOk)

    verify(licenceService, times(1)).referLicenceVariation(4, expectedRequest)
  }

  private companion object {

    val someStandardConditions = listOf(
      StandardCondition(id = 1, code = "goodBehaviour", sequence = 1, text = "Be of good behaviour"),
      StandardCondition(id = 2, code = "notBreakLaw", sequence = 1, text = "Do not break any law"),
      StandardCondition(id = 3, code = "attendMeetings", sequence = 1, text = "Attend meetings"),
    )

    val someAssociationData = listOf(
      AdditionalConditionData(id = 1, field = "field1", value = "value1", sequence = 1),
      AdditionalConditionData(id = 2, field = "field2", value = "value2", sequence = 2),
    )

    val someAdditionalConditions = listOf(
      AdditionalCondition(
        id = 1,
        code = "associateWith",
        sequence = 1,
        text = "Do not associate with [NAME] for a period of [TIME PERIOD]",
        expandedText = "Do not associate with value1 for a period of value2",
        data = someAssociationData,
      ),
    )

    val someBespokeConditions = listOf(
      BespokeCondition(id = 1, sequence = 1, text = "Bespoke one text"),
      BespokeCondition(id = 2, sequence = 2, text = "Bespoke two text"),
    )

    val aLicence = Licence(
      id = 1,
      typeCode = LicenceType.AP,
      version = "1.1",
      statusCode = LicenceStatus.IN_PROGRESS,
      nomsId = "A1234AA",
      bookingNo = "123456",
      bookingId = 54321,
      crn = "X12345",
      pnc = "2019/123445",
      cro = "12345",
      prisonCode = "MDI",
      prisonDescription = "Moorland (HMP)",
      forename = "Bob",
      surname = "Mortimer",
      dateOfBirth = LocalDate.of(1985, 12, 28),
      conditionalReleaseDate = LocalDate.of(2021, 10, 22),
      actualReleaseDate = LocalDate.of(2021, 10, 22),
      sentenceStartDate = LocalDate.of(2018, 10, 22),
      sentenceEndDate = LocalDate.of(2021, 10, 22),
      licenceStartDate = LocalDate.of(2021, 10, 22),
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
      topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
      comUsername = "X12345",
      comStaffId = 12345,
      comEmail = "stephen.mills@nps.gov.uk",
      probationAreaCode = "N01",
      probationAreaDescription = "Wales",
      probationPduCode = "N01A",
      probationPduDescription = "Cardiff",
      probationLauCode = "N01A2",
      probationLauDescription = "Cardiff South",
      probationTeamCode = "NA01A2-A",
      probationTeamDescription = "Cardiff South Team A",
      dateCreated = LocalDateTime.now(),
      createdByUsername = "X12345",
      standardLicenceConditions = someStandardConditions,
      standardPssConditions = someStandardConditions,
      additionalLicenceConditions = someAdditionalConditions,
      additionalPssConditions = someAdditionalConditions,
      bespokeConditions = someBespokeConditions,
      isVariation = false,
    )

    val aCreateLicenceRequest = CreateLicenceRequest(
      typeCode = LicenceType.AP,
      version = "1.4",
      nomsId = "NOMSID",
      bookingNo = "BOOKINGNO",
      bookingId = 1L,
      crn = "CRN1",
      pnc = "PNC1",
      cro = "CRO1",
      prisonCode = "MDI",
      prisonDescription = "Moorland (HMP)",
      forename = "Mike",
      surname = "Myers",
      dateOfBirth = LocalDate.of(2001, 10, 1),
      conditionalReleaseDate = LocalDate.of(2021, 10, 22),
      actualReleaseDate = LocalDate.of(2021, 10, 22),
      sentenceStartDate = LocalDate.of(2018, 10, 22),
      sentenceEndDate = LocalDate.of(2021, 10, 22),
      licenceStartDate = LocalDate.of(2021, 10, 22),
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
      topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
      probationAreaCode = "N01",
      probationAreaDescription = "Wales",
      probationPduCode = "N01A",
      probationPduDescription = "Cardiff",
      probationLauCode = "N01A2",
      probationLauDescription = "Cardiff South",
      probationTeamCode = "NA01A2-A",
      probationTeamDescription = "Cardiff South Team A",
      standardLicenceConditions = someStandardConditions,
      standardPssConditions = someStandardConditions,
      responsibleComStaffId = 2000,
    )

    val aLicenceSummary = LicenceSummary(
      licenceId = 1,
      licenceType = LicenceType.AP,
      licenceStatus = LicenceStatus.IN_PROGRESS,
      nomisId = "A1234AA",
      forename = "Bob",
      surname = "Mortimer",
      crn = "X12345",
      dateOfBirth = LocalDate.of(1985, 12, 28),
      prisonCode = "MDI",
      prisonDescription = "Moorland (HMP)",
      probationAreaCode = "N01",
      probationAreaDescription = "Wales",
      probationPduCode = "N01A",
      probationPduDescription = "Cardiff",
      probationLauCode = "N01A2",
      probationLauDescription = "Cardiff South",
      probationTeamCode = "NA01A2-A",
      probationTeamDescription = "Cardiff South Team A",
      conditionalReleaseDate = LocalDate.of(2022, 12, 28),
      actualReleaseDate = LocalDate.of(2022, 12, 30),
      sentenceStartDate = LocalDate.of(2018, 10, 22),
      sentenceEndDate = LocalDate.of(2021, 10, 22),
      licenceStartDate = LocalDate.of(2021, 10, 22),
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
      topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
      comUsername = "jsmith",
      bookingId = 54321,
      dateCreated = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
      approvedByName = "jim smith",
      approvedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
      licenceVersion = "1.0",
    )

    val anUpdateAppointmentPersonRequest = AppointmentPersonRequest(
      appointmentPerson = "John Smith",
    )

    val anAppointmentTimeRequest = AppointmentTimeRequest(
      appointmentTime = LocalDateTime.now().plusDays(1L).truncatedTo(ChronoUnit.MINUTES),
    )

    val anAppointmentTimeRequestDateOnly = AppointmentTimeRequest(
      appointmentTime = LocalDateTime.now().plusDays(1L).truncatedTo(ChronoUnit.DAYS),
    )

    val aContactNumberRequest = ContactNumberRequest(
      telephone = "0114 2566555",
    )

    val anAppointmentAddressRequest = AppointmentAddressRequest(
      appointmentAddress = "221B Baker Street, London, City of London, NW1 6XE",
    )

    val anUpdateStandardConditionRequest = UpdateStandardConditionDataRequest(
      standardLicenceConditions = listOf(
        StandardCondition(code = "code1", sequence = 0, text = "text"),
        StandardCondition(code = "code2", sequence = 1, text = "text"),
        StandardCondition(code = "code3", sequence = 2, text = "text"),
      ),
    )

    val anAddAdditionalConditionRequest = AddAdditionalConditionRequest(
      conditionCode = "code",
      conditionType = "AP",
      conditionCategory = "category",
      sequence = 4,
      conditionText = "text",
      expandedText = "some more text",
    )

    val aBespokeConditionsRequest = BespokeConditionRequest(conditions = listOf("Bespoke 1", "Bespoke 2"))

    val aStatusUpdateRequest =
      StatusUpdateRequest(status = LicenceStatus.APPROVED, username = "X", fullName = "Jon Smith")

    val anUpdateAdditionalConditionsListRequest = AdditionalConditionsRequest(
      additionalConditions = listOf(
        AdditionalConditionRequest(code = "code", category = "category", sequence = 0, text = "text"),
      ),
      conditionType = "AP",
    )

    val anUpdateAdditionalConditionsDataRequest = UpdateAdditionalConditionDataRequest(
      data = listOf(AdditionalConditionData(field = "field1", value = "value1", sequence = 0)),
    )
  }
}
