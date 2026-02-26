package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CreateLicenceResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CreateVariationResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.EditLicenceResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StatusUpdateRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.DeactivateLicenceAndVariationsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.LicenceType.CRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.LicenceType.HARD_STOP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.LicenceType.PRRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.MatchLicencesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.NotifyRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ReferVariationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdatePrisonInformationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateReasonForVariationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSpoDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateVloDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceCreationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.UpdateSentenceDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.SentenceDates
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.DateChangeLicenceDeativationReason
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [LicenceController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [LicenceController::class])
@WebAppConfiguration
class LicenceControllerTest {

  @MockitoBean
  private lateinit var licenceService: LicenceService

  @MockitoBean
  private lateinit var updateSentenceDateService: UpdateSentenceDateService

  @MockitoBean
  private lateinit var licenceCreationService: LicenceCreationService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(licenceService)

    mvc = MockMvcBuilders
      .standaloneSetup(
        LicenceController(
          licenceService,
          updateSentenceDateService,
          licenceCreationService,
          prisonEventHandlerEnabled = false,
        ),
      )
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `get a licence by id`() {
    whenever(licenceService.getLicenceById(1)).thenReturn(aLicence)

    val result = mvc.perform(get("/licence/id/1").accept(APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andExpect(
        jsonPath(
          "\$.additionalLicenceConditions.[*].data[?(@.field == 'field1')].contributesToLicence",
          contains(true),
        ),
      )
      .andExpect(
        jsonPath(
          "\$.additionalLicenceConditions.[*].data[?(@.field == 'numberOfCurfews')].contributesToLicence",
          contains(false),
        ),
      )
      .andReturn()

    assertThat(result.response.contentAsString)
      .isEqualTo(mapper.writeValueAsString(aLicence))

    verify(licenceService, times(1)).getLicenceById(1)
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
  fun `create a PRRD licence`() {
    whenever(licenceCreationService.createLicence(aCreateLicenceRequest.nomsId)).thenReturn(CreateLicenceResponse(1))

    val result = mvc.perform(
      post("/licence/create")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(aCreateLicenceRequest.copy(type = PRRD))),
    )
      .andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString).isEqualTo((mapper.writeValueAsString(CreateLicenceResponse(1))))

    verify(licenceCreationService, times(1)).createLicence(aCreateLicenceRequest.nomsId)
  }

  @Test
  fun `create a CRD licence`() {
    whenever(licenceCreationService.createLicence(aCreateLicenceRequest.nomsId)).thenReturn(CreateLicenceResponse(1))

    val result = mvc.perform(
      post("/licence/create")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(aCreateLicenceRequest.copy(type = CRD))),
    )
      .andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString).isEqualTo((mapper.writeValueAsString(CreateLicenceResponse(1))))

    verify(licenceCreationService, times(1)).createLicence(aCreateLicenceRequest.nomsId)
  }

  @Test
  fun `create a Hard Stop licence`() {
    whenever(licenceCreationService.createHardStopLicence(aCreateLicenceRequest.nomsId)).thenReturn(
      CreateLicenceResponse(1),
    )

    val result = mvc.perform(
      post("/licence/create")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(aCreateLicenceRequest.copy(type = HARD_STOP))),
    )
      .andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString).isEqualTo(mapper.writeValueAsString(CreateLicenceResponse(1)))

    verify(licenceCreationService, times(1)).createHardStopLicence(aCreateLicenceRequest.nomsId)
  }

  @Test
  fun `create a licence where another is in progress`() {
    whenever(licenceCreationService.createLicence(aCreateLicenceRequest.nomsId))
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

    verify(licenceCreationService, times(1)).createLicence(aCreateLicenceRequest.nomsId)
  }

  @Test
  fun `match licences by pdu and status`() {
    // Given
    val licenceQueryObject = LicenceQueryObject(
      statusCodes = listOf(LicenceStatus.APPROVED, LicenceStatus.ACTIVE),
      sortBy = "id",
    )
    val request = MatchLicencesRequest(
      status = listOf(LicenceStatus.APPROVED, LicenceStatus.ACTIVE),
    )

    whenever(licenceService.findLicencesMatchingCriteria(licenceQueryObject)).thenReturn(listOf(aLicenceSummary()))

    // When
    val result = mvc.perform(
      post("/licence/match")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(
          mapper.writeValueAsBytes(
            request,
          ),
        ),
    )

    // Then
    result.andExpect(status().isOk)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.andReturn().response.contentAsString)
      .isEqualTo(mapper.writeValueAsString(listOf(aLicenceSummary())))

    verify(licenceService, times(1)).findLicencesMatchingCriteria(licenceQueryObject)
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
    whenever(licenceService.createVariation(4L)).thenReturn(CreateVariationResponse(4))

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
    whenever(licenceService.editLicence(licenceId)).thenReturn(EditLicenceResponse(licenceId))

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
    val expectedRequest = SentenceDates(
      conditionalReleaseDate = LocalDate.parse("2023-09-11"),
      actualReleaseDate = LocalDate.parse("2023-09-11"),
      sentenceStartDate = LocalDate.parse("2021-09-11"),
      sentenceEndDate = LocalDate.parse("2024-09-11"),
      licenceExpiryDate = LocalDate.parse("2024-09-11"),
      topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
      topupSupervisionExpiryDate = null,
      postRecallReleaseDate = null,
      homeDetentionCurfewEndDate = null,
      homeDetentionCurfewActualDate = null,
      homeDetentionCurfewEligibilityDate = null,
    )

    mvc.perform(
      put("/licence/id/4/sentence-dates")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(expectedRequest)),
    )
      .andExpect(status().isOk)

    verify(updateSentenceDateService, times(1)).updateSentenceDates(4)
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
  fun `activate a variation`() {
    mvc.perform(
      put("/licence/id/4/activate-variation")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON),
    )
      .andExpect(status().isOk)

    verify(licenceService, times(1)).activateVariation(4)
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

  @Test
  fun `mark licence as reviewed`() {
    mvc.perform(
      post("/licence/id/4/review-with-no-variation-required")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON),
    )
      .andExpect(status().isOk)

    verify(licenceService, times(1)).reviewWithNoVariationRequired(4L)
  }

  @Test
  fun `deactivate a licence and variations`() {
    val request = DeactivateLicenceAndVariationsRequest(DateChangeLicenceDeativationReason.RECALLED)
    mvc.perform(
      post("/licence/id/4/deactivate-licence-and-variations")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(request)),
    )

    verify(licenceService, times(1)).deactivateLicenceAndVariations(4, request)
  }

  private companion object {

    val someStandardConditions = listOf(
      StandardCondition(id = 1, code = "goodBehaviour", sequence = 1, text = "Be of good behaviour"),
      StandardCondition(id = 2, code = "notBreakLaw", sequence = 1, text = "Do not break any law"),
      StandardCondition(id = 3, code = "attendMeetings", sequence = 1, text = "Attend meetings"),
    )

    val someAssociationData = listOf(
      AdditionalConditionData(id = 1, field = "field1", value = "value1", sequence = 1),
      AdditionalConditionData(id = 2, field = "numberOfCurfews", value = "value2", sequence = 2),
    )

    val someAdditionalConditions = listOf(
      AdditionalCondition(
        id = 1,
        code = "associateWith",
        sequence = 1,
        text = "Do not associate with [NAME] for a period of [TIME PERIOD]",
        expandedText = "Do not associate with value1 for a period of value2",
        data = someAssociationData,
        readyToSubmit = true,
        requiresInput = true,
      ),
    )

    val someBespokeConditions = listOf(
      BespokeCondition(id = 1, sequence = 1, text = "Bespoke one text"),
      BespokeCondition(id = 2, sequence = 2, text = "Bespoke two text"),
    )

    val aLicence = CrdLicence(
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
      forename = "Person",
      surname = "One",
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
      comEmail = "test.com@probation.gov.uk",
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
    )

    val aCreateLicenceRequest = CreateLicenceRequest(nomsId = "NOMSID")

    val aStatusUpdateRequest =
      StatusUpdateRequest(status = LicenceStatus.APPROVED, username = "X", fullName = "Jon Smith")
  }
}
