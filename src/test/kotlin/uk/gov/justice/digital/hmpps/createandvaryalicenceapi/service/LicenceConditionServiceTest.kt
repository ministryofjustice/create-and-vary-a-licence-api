package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.*
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.*
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.*
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.AddAdditionalConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.BespokeConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class LicenceConditionServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val additionalConditionRepository = mock<AdditionalConditionRepository>()
  private val bespokeConditionRepository = mock<BespokeConditionRepository>()
  private val additionalConditionUploadDetailRepository = mock<AdditionalConditionUploadDetailRepository>()

  private val service = LicenceConditionService(
    licenceRepository,
    additionalConditionRepository,
    bespokeConditionRepository,
    additionalConditionUploadDetailRepository
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()
    whenever(authentication.name).thenReturn("smills")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    reset(
      licenceRepository,
      additionalConditionRepository,
      bespokeConditionRepository,
      additionalConditionUploadDetailRepository
    )
  }

  @Test
  fun `update standard conditions for an individual licence`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    val APConditions = listOf(
      StandardCondition(code = "goodBehaviour", sequence = 1, text = "Be of good behaviour")
    )

    val PSSConditions = listOf(
      StandardCondition(code = "goodBehaviour", sequence = 1, text = "Be of good behaviour"),
      StandardCondition(code = "doNotBreakLaw", sequence = 2, text = "Do not break any law"),
    )

    service.updateStandardConditions(
      1,
      UpdateStandardConditionDataRequest(
        standardLicenceConditions = APConditions,
        standardPssConditions = PSSConditions
      )
    )

    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value).extracting("updatedByUsername").isEqualTo("smills")

    assertThat(licenceCaptor.value.standardConditions).containsExactly(
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition(
        conditionCode = "goodBehaviour",
        conditionSequence = 1,
        conditionText = "Be of good behaviour",
        conditionType = "AP",
        licence = aLicenceEntity
      ),
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition(
        conditionCode = "goodBehaviour",
        conditionSequence = 1,
        conditionText = "Be of good behaviour",
        conditionType = "PSS",
        licence = aLicenceEntity
      ),
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition(
        conditionCode = "doNotBreakLaw",
        conditionSequence = 2,
        conditionText = "Do not break any law",
        conditionType = "PSS",
        licence = aLicenceEntity
      )
    )
  }

  @Test
  fun `delete one additional condition`() {
    whenever(licenceRepository.findById(1L))
      .thenReturn(
        Optional.of(
          aLicenceEntity.copy(
            additionalConditions = listOf(
              AdditionalCondition(
                id = 1,
                conditionVersion = "1.0",
                conditionCode = "code",
                conditionSequence = 5,
                conditionCategory = "oldCategory",
                conditionText = "oldText",
                additionalConditionData = someAdditionalConditionData,
                licence = aLicenceEntity,
                conditionType = "AP"
              ),
              AdditionalCondition(
                id = 2,
                conditionVersion = "1.0",
                conditionCode = "code2",
                conditionSequence = 6,
                conditionCategory = "removedCategory",
                conditionText = "removedText",
                additionalConditionData = someAdditionalConditionData,
                licence = aLicenceEntity,
                conditionType = "AP"
              ),
              AdditionalCondition(
                id = 3,
                conditionVersion = "1.0",
                conditionCode = "code3",
                conditionSequence = 6,
                conditionCategory = "oldCategory3",
                conditionText = "oldText3",
                additionalConditionData = someAdditionalConditionData,
                licence = aLicenceEntity,
                conditionType = "AP"
              ),
            )
          )
        )
      )

    service.deleteAdditionalCondition(1L, 2)

    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value.additionalConditions).containsExactly(
      AdditionalCondition(
        id = 1,
        conditionVersion = "1.0",
        conditionCode = "code",
        conditionCategory = "oldCategory",
        conditionSequence = 5,
        conditionText = "oldText",
        conditionType = "AP",
        additionalConditionData = someAdditionalConditionData,
        licence = aLicenceEntity
      ),
      AdditionalCondition(
        id = 3,
        conditionVersion = "1.0",
        conditionCode = "code3",
        conditionCategory = "oldCategory3",
        conditionSequence = 6,
        conditionText = "oldText3",
        conditionType = "AP",
        additionalConditionData = someAdditionalConditionData,
        licence = aLicenceEntity
      )
    )

    // Verify last contact info is recorded
    assertThat(licenceCaptor.value.updatedByUsername).isEqualTo("smills")
  }

  @Test
  fun `update additional conditions throws not found exception`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.updateAdditionalConditions(
        1L,
        AdditionalConditionsRequest(
          additionalConditions = listOf(
            uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition(
              code = "code",
              category = "category",
              text = "text",
              sequence = 0
            )
          ),
          conditionType = "AP"
        )
      )
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

    verify(licenceRepository, times(1)).findById(1L)
    verify(licenceRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `update additional conditions`() {
    whenever(licenceRepository.findById(1L))
      .thenReturn(
        Optional.of(
          aLicenceEntity.copy(
            additionalConditions = listOf(
              AdditionalCondition(
                id = 1,
                conditionVersion = "1.0",
                conditionCode = "code",
                conditionSequence = 5,
                conditionCategory = "oldCategory",
                conditionText = "oldText",
                additionalConditionData = someAdditionalConditionData,
                licence = aLicenceEntity,
                conditionType = "AP"
              ),
              AdditionalCondition(
                id = 2,
                conditionVersion = "1.0",
                conditionCode = "code2",
                conditionSequence = 6,
                conditionCategory = "removedCategory",
                conditionText = "removedText",
                additionalConditionData = someAdditionalConditionData,
                licence = aLicenceEntity,
                conditionType = "AP"
              ),
              AdditionalCondition(
                id = 3,
                conditionVersion = "1.0",
                conditionCode = "code3",
                conditionSequence = 6,
                conditionCategory = "pssCategory",
                conditionText = "pssText",
                additionalConditionData = someAdditionalConditionData,
                licence = aLicenceEntity,
                conditionType = "PSS"
              )
            )
          )
        )
      )

    val request = AdditionalConditionsRequest(
      additionalConditions = listOf(
        uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition(
          code = "code",
          category = "category",
          text = "text",
          sequence = 0
        )
      ),
      conditionType = "AP"
    )

    service.updateAdditionalConditions(1L, request)

    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value.additionalConditions).containsExactly(
      AdditionalCondition(
        id = 1,
        conditionVersion = "1.0",
        conditionCode = "code",
        conditionCategory = "category",
        conditionSequence = 0,
        conditionText = "text",
        conditionType = "AP",
        additionalConditionData = someAdditionalConditionData,
        licence = aLicenceEntity
      ),
      AdditionalCondition(
        id = 3,
        conditionVersion = "1.0",
        conditionCode = "code3",
        conditionSequence = 6,
        conditionCategory = "pssCategory",
        conditionText = "pssText",
        additionalConditionData = someAdditionalConditionData,
        licence = aLicenceEntity,
        conditionType = "PSS"
      )
    )

    // Verify last contact info is recorded
    assertThat(licenceCaptor.value.updatedByUsername).isEqualTo("smills")
  }

  @Test
  fun `update bespoke conditions persists multiple entities`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    val bespokeEntities = listOf(
      BespokeCondition(id = -1L, licence = aLicenceEntity, conditionSequence = 1, conditionText = "Condition 2"),
      BespokeCondition(id = -1L, licence = aLicenceEntity, conditionSequence = 2, conditionText = "Condition 3"),
      BespokeCondition(id = -1L, licence = aLicenceEntity, conditionSequence = 0, conditionText = "Condition 1"),
    )

    bespokeEntities.forEach { bespoke ->
      whenever(bespokeConditionRepository.saveAndFlush(bespoke)).thenReturn(bespoke)
    }

    service.updateBespokeConditions(1L, someBespokeConditions)

    // Verify licence entity is updated with last contact info
    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value).extracting("updatedByUsername").isEqualTo("smills")
    assertThat(licenceCaptor.value).extracting("bespokeConditions").isEqualTo(emptyList<BespokeCondition>())

    // Verify new bespoke conditions are added in their place
    bespokeEntities.forEach { bespoke ->
      verify(bespokeConditionRepository, times(1)).saveAndFlush(bespoke)
    }
  }

  @Test
  fun `update bespoke conditions with an empty list - removes previously persisted entities`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    service.updateBespokeConditions(1L, BespokeConditionRequest())

    verify(bespokeConditionRepository, times(0)).saveAndFlush(any())
    verify(licenceRepository, times(1)).saveAndFlush(any())
  }

  @Test
  fun `update bespoke conditions throws not found exception if licence not found`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.updateBespokeConditions(1L, someBespokeConditions)
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

    verify(licenceRepository, times(1)).findById(1L)
    verify(bespokeConditionRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `update additional condition data throws not found exception if licence is not found`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.updateAdditionalConditionData(
        1L, 1L,
        UpdateAdditionalConditionDataRequest(
          data = listOf(uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData(field = "field1", value = "value1", sequence = 0)),
          expandedConditionText = "expanded text"
        )
      )
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

    verify(licenceRepository, times(1)).findById(1L)
    verify(licenceRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `update additional condition data throws not found exception if condition is not found`() {
    whenever(licenceRepository.findById(1L))
      .thenReturn(
        Optional.of(
          aLicenceEntity.copy(
            additionalConditions = listOf(
              AdditionalCondition(
                id = 1,
                conditionVersion = "1.0",
                conditionCode = "code",
                conditionSequence = 5,
                conditionCategory = "oldCategory",
                conditionText = "oldText",
                additionalConditionData = emptyList(),
                licence = aLicenceEntity
              )
            )
          )
        )
      )

    whenever(additionalConditionRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.updateAdditionalConditionData(
        1L, 1L,
        UpdateAdditionalConditionDataRequest(
          data = listOf(uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData(field = "field1", value = "value1", sequence = 0)),
          expandedConditionText = "expanded text"
        )
      )
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

    verify(licenceRepository, times(1)).findById(1L)
    verify(licenceRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `update additional condition data`() {
    whenever(licenceRepository.findById(1L))
      .thenReturn(
        Optional.of(
          aLicenceEntity.copy(
            additionalConditions = listOf(
              AdditionalCondition(
                id = 1,
                conditionVersion = "1.0",
                conditionCode = "code",
                conditionSequence = 5,
                conditionCategory = "oldCategory",
                conditionText = "oldText",
                additionalConditionData = someAdditionalConditionData,
                licence = aLicenceEntity
              )
            )
          )
        )
      )
    whenever(additionalConditionRepository.findById(1L))
      .thenReturn(
        Optional.of(
          anAdditionalConditionEntity.copy()
        )
      )

    val request = UpdateAdditionalConditionDataRequest(
      data = listOf(uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData(field = "field1", value = "value1", sequence = 0)),
      expandedConditionText = "expanded text"
    )

    service.updateAdditionalConditionData(1L, 1L, request)

    val conditionCaptor = ArgumentCaptor.forClass(AdditionalCondition::class.java)
    val licenceCaptor = ArgumentCaptor.forClass(Licence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    verify(additionalConditionRepository, times(1)).saveAndFlush(conditionCaptor.capture())

    assertThat(conditionCaptor.value.expandedConditionText).isEqualTo("expanded text")
    assertThat(conditionCaptor.value.additionalConditionData).containsExactly(
      AdditionalConditionData(
        id = -1,
        additionalCondition = anAdditionalConditionEntity,
        dataSequence = 0,
        dataField = "field1",
        dataValue = "value1"
      )
    )

    // Verify last contact info is recorded
    assertThat(licenceCaptor.value.updatedByUsername).isEqualTo("smills")
  }



  private companion object {

    val someEntityStandardConditions = listOf(
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition(
        id = 1,
        conditionCode = "goodBehaviour",
        conditionSequence = 1,
        conditionText = "Be of good behaviour",
        licence = mock()
      ),
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition(
        id = 2,
        conditionCode = "notBreakLaw",
        conditionSequence = 2,
        conditionText = "Do not break any law",
        licence = mock()
      ),
      uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition(
        id = 3,
        conditionCode = "attendMeetings",
        conditionSequence = 3,
        conditionText = "Attend meetings",
        licence = mock()
      ),
    )

    val aLicenceEntity = Licence(
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
      probationAreaCode = "N01",
      probationAreaDescription = "Wales",
      probationPduCode = "N01A",
      probationPduDescription = "Cardiff",
      probationLauCode = "N01A2",
      probationLauDescription = "Cardiff South",
      probationTeamCode = "NA01A2-A",
      probationTeamDescription = "Cardiff South Team A",
      dateCreated = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
      standardConditions = someEntityStandardConditions,
      mailingList = mutableSetOf(
        CommunityOffenderManager(
          staffIdentifier = 2000,
          username = "smills",
          email = "testemail@probation.gov.uk",
          firstName = "X",
          lastName = "Y"
        )
      ),
      responsibleCom = CommunityOffenderManager(
        staffIdentifier = 2000,
        username = "smills",
        email = "testemail@probation.gov.uk",
        firstName = "X",
        lastName = "Y"
      ),
      createdBy = CommunityOffenderManager(
        staffIdentifier = 2000,
        username = "smills",
        email = "testemail@probation.gov.uk",
        firstName = "X",
        lastName = "Y"
      ),
    )

    val someAdditionalConditionData = listOf(
      AdditionalConditionData(
        id = 1,
        dataField = "dataField",
        dataValue = "dataValue",
        additionalCondition = AdditionalCondition(licence = aLicenceEntity, conditionVersion = "1.0")
      )
    )

    val someBespokeConditions =
      BespokeConditionRequest(conditions = listOf("Condition 1", "Condition 2", "Condition 3"))

    val anAdditionalConditionEntity = AdditionalCondition(
      id = 1,
      conditionVersion = "1.0",
      licence = aLicenceEntity,
      conditionCode = "code1",
      conditionCategory = "category1",
      conditionSequence = 4,
      conditionText = "text",
      additionalConditionData = someAdditionalConditionData,
      additionalConditionUploadSummary = emptyList(),
      conditionType = "AP"
    )
  }

}
