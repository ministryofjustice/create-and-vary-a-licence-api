package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyList
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentPersonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentTimeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ContactNumberRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StatusUpdateRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateAdditionalConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.BespokeConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceHistoryRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StandardConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import javax.persistence.EntityNotFoundException
import javax.validation.ValidationException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition as EntityAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeCondition as EntityBespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceHistory as EntityLicenceHistory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition as EntityStandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData as ModelAdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence as ModelLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition as ModelStandardCondition

class LicenceServiceTest {
  private val standardConditionRepository = mock<StandardConditionRepository>()
  private val additionalConditionRepository = mock<AdditionalConditionRepository>()
  private val bespokeConditionRepository = mock<BespokeConditionRepository>()
  private val licenceRepository = mock<LicenceRepository>()
  private val licenceHistoryRepository = mock<LicenceHistoryRepository>()

  private val service = LicenceService(
    licenceRepository,
    standardConditionRepository,
    additionalConditionRepository,
    bespokeConditionRepository,
    licenceHistoryRepository,
  )

  @BeforeEach
  fun reset() {
    reset(licenceRepository, standardConditionRepository, bespokeConditionRepository, licenceHistoryRepository)
  }

  @Test
  fun `service returns a licence by ID`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    val licence = service.getLicenceById(1L)

    assertThat(licence).isExactlyInstanceOf(ModelLicence::class.java)

    verify(licenceRepository, times(1)).findById(1L)
  }

  @Test
  fun `service transforms key fields of a licence object correctly`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    val licence = service.getLicenceById(1L)

    assertThat(licence.cro).isEqualTo(aLicenceEntity.cro)
    assertThat(licence.nomsId).isEqualTo(aLicenceEntity.nomsId)
    assertThat(licence.bookingId).isEqualTo(aLicenceEntity.bookingId)
    assertThat(licence.pnc).isEqualTo(aLicenceEntity.pnc)

    verify(licenceRepository, times(1)).findById(1L)
  }

  @Test
  fun `service throws a not found exception for unknown ID`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.getLicenceById(1L)
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

    verify(licenceRepository, times(1)).findById(1L)
  }

  @Test
  fun `service creates a licence with standard conditions`() {
    whenever(standardConditionRepository.saveAllAndFlush(anyList())).thenReturn(someEntityStandardConditions)
    whenever(licenceRepository.saveAndFlush(any())).thenReturn(aLicenceEntity)

    val createResponse = service.createLicence(aCreateLicenceRequest)

    assertThat(createResponse.licenceStatus).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(createResponse.licenceType).isEqualTo(LicenceType.AP)

    verify(standardConditionRepository, times(1)).saveAllAndFlush(anyList())
    verify(licenceRepository, times(1)).saveAndFlush(any())
  }

  @Test
  fun `service throws a validation exception if an in progress licence exists for this person`() {
    whenever(
      licenceRepository
        .findAllByNomsIdAndStatusCodeIn(
          aCreateLicenceRequest.nomsId!!,
          listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED, LicenceStatus.APPROVED, LicenceStatus.REJECTED)
        )
    ).thenReturn(listOf(aLicenceEntity))

    val exception = assertThrows<ValidationException> {
      service.createLicence(aCreateLicenceRequest)
    }

    assertThat(exception)
      .isInstanceOf(ValidationException::class.java)
      .withFailMessage("A licence already exists for this person (IN_PROGRESS, SUBMITTED, APPROVED or REJECTED)")

    verify(licenceRepository, times(0)).saveAndFlush(any())
    verify(standardConditionRepository, times(0)).saveAllAndFlush(anyList())
  }

  @Test
  fun `update initial appointment person persists updated entity correctly`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    service.updateAppointmentPerson(1L, AppointmentPersonRequest(appointmentPerson = "John Smith"))

    val expectedUpdatedEntity = aLicenceEntity.copy(appointmentPerson = "John Smith")

    verify(licenceRepository, times(1)).saveAndFlush(expectedUpdatedEntity)
  }

  @Test
  fun `update initial appointment person throws not found exception if licence not found`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.updateAppointmentPerson(1L, AppointmentPersonRequest(appointmentPerson = "John Smith"))
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

    verify(licenceRepository, times(1)).findById(1L)
  }

  @Test
  fun `update initial appointment time persists the updated entity`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    service.updateAppointmentTime(1L, AppointmentTimeRequest(appointmentTime = tenDaysFromNow))

    val expectedUpdatedEntity = aLicenceEntity.copy(appointmentTime = tenDaysFromNow)

    verify(licenceRepository, times(1)).saveAndFlush(expectedUpdatedEntity)
  }

  @Test
  fun `update initial appointment time throws not found exception if licence not found`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.updateAppointmentTime(1L, AppointmentTimeRequest(appointmentTime = tenDaysFromNow))
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

    verify(licenceRepository, times(1)).findById(1L)
  }

  @Test
  fun `update contact number persists the updated entity`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    service.updateContactNumber(1L, ContactNumberRequest(comTelephone = "0114 2565555"))

    val expectedUpdatedEntity = aLicenceEntity.copy(comTelephone = "0114 2565555")

    verify(licenceRepository, times(1)).saveAndFlush(expectedUpdatedEntity)
  }

  @Test
  fun `update contact number throws not found exception if licence not found`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.updateContactNumber(1L, ContactNumberRequest(comTelephone = "0114 2565555"))
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

    verify(licenceRepository, times(1)).findById(1L)
  }

  @Test
  fun `update appointment address persists the updated entity`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    service.updateAppointmentAddress(
      1L,
      AppointmentAddressRequest(appointmentAddress = "221B Baker Street, London, City of London, NW1 6XE")
    )

    val expectedUpdatedEntity =
      aLicenceEntity.copy(appointmentAddress = "221B Baker Street, London, City of London, NW1 6XE")

    verify(licenceRepository, times(1)).saveAndFlush(expectedUpdatedEntity)
  }

  @Test
  fun `update appointment address throws not found exception if licence not found`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.updateAppointmentAddress(
        1L,
        AppointmentAddressRequest(appointmentAddress = "221B Baker Street, London, City of London, NW1 6XE")
      )
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

    verify(licenceRepository, times(1)).findById(1L)
  }

  @Test
  fun `update bespoke conditions persists multiple entities`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(bespokeConditionRepository.deleteByLicenceId(1L)).thenReturn(0)
    val bespokeEntities = listOf(
      // -1 on the id as this is a generated value in the database
      EntityBespokeCondition(id = -1L, licenceId = 1L, conditionSequence = 0, conditionText = "Condition 1"),
      EntityBespokeCondition(id = -1L, licenceId = 1L, conditionSequence = 1, conditionText = "Condition 2"),
      EntityBespokeCondition(id = -1L, licenceId = 1L, conditionSequence = 2, conditionText = "Condition 3"),
    )
    bespokeEntities.forEach { bespoke ->
      whenever(bespokeConditionRepository.saveAndFlush(bespoke)).thenReturn(bespoke)
    }

    service.updateBespokeConditions(1L, someBespokeConditions)

    verify(bespokeConditionRepository, times(1)).deleteByLicenceId(1L)
    bespokeEntities.forEach { bespoke ->
      verify(bespokeConditionRepository, times(1)).saveAndFlush(bespoke)
    }
  }

  @Test
  fun `update bespoke conditions with an empty list - removes previously persisted entities`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(bespokeConditionRepository.deleteByLicenceId(1L)).thenReturn(0)

    service.updateBespokeConditions(1L, BespokeConditionRequest())

    verify(bespokeConditionRepository, times(1)).deleteByLicenceId(1L)
    verify(bespokeConditionRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `update bespoke conditions throws not found exception if licence not found`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.updateBespokeConditions(1L, someBespokeConditions)
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)
    verify(licenceRepository, times(1)).findById(1L)
    verify(bespokeConditionRepository, times(0)).deleteByLicenceId(1L)
    verify(bespokeConditionRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `find licences by staff ID - empty result`() {
    whenever(licenceRepository.findAllByComStaffId(1L)).thenReturn(emptyList())

    val licenceSummaries = service.findLicencesByStaffIdAndStatuses(1L, null)
    val expectedEmptyList: List<LicenceSummary> = emptyList()

    assertThat(licenceSummaries).isEqualTo(expectedEmptyList)
    verify(licenceRepository, times(1)).findAllByComStaffId(1L)
  }

  @Test
  fun `find licences by staff ID`() {
    whenever(licenceRepository.findAllByComStaffId(1L)).thenReturn(listOf(aLicenceEntity))

    val licenceSummaries = service.findLicencesByStaffIdAndStatuses(1L, null)

    assertThat(licenceSummaries).isEqualTo(listOf(aLicenceSummary))
    verify(licenceRepository, times(1)).findAllByComStaffId(1L)
  }

  @Test
  fun `find licences by staff ID and status`() {
    whenever(licenceRepository.findAllByComStaffIdAndStatusCodeIn(1L, listOf(LicenceStatus.IN_PROGRESS))).thenReturn(listOf(aLicenceEntity))

    val licenceSummaries = service.findLicencesByStaffIdAndStatuses(1L, listOf(LicenceStatus.IN_PROGRESS))

    assertThat(licenceSummaries).isEqualTo(listOf(aLicenceSummary))
    verify(licenceRepository, times(1)).findAllByComStaffIdAndStatusCodeIn(1L, listOf(LicenceStatus.IN_PROGRESS))
  }

  @Test
  fun `find approval candidates - single prison and empty result list`() {
    whenever(licenceRepository.findAllByStatusCodeAndPrisonCodeIn(LicenceStatus.SUBMITTED, listOf("MDI")))
      .thenReturn(emptyList())

    val licenceSummaries = service.findLicencesForApprovalByPrisonCaseload(listOf("MDI"))

    assertThat(licenceSummaries).isEmpty()
    verify(licenceRepository, times(1))
      .findAllByStatusCodeAndPrisonCodeIn(LicenceStatus.SUBMITTED, listOf("MDI"))
    verify(licenceRepository, times(0)).findAllByStatusCode(any())
  }

  @Test
  fun `find approval candidates - list of prisons`() {
    whenever(licenceRepository.findAllByStatusCodeAndPrisonCodeIn(LicenceStatus.SUBMITTED, listOf("MDI", "LEI")))
      .thenReturn(listOf(aLicenceEntity))

    val licenceSummaries = service.findLicencesForApprovalByPrisonCaseload(listOf("MDI", "LEI"))

    assertThat(licenceSummaries).isEqualTo(listOf(aLicenceSummary))
    verify(licenceRepository, times(1))
      .findAllByStatusCodeAndPrisonCodeIn(LicenceStatus.SUBMITTED, listOf("MDI", "LEI"))
    verify(licenceRepository, times(0)).findAllByStatusCode(any())
  }

  @Test
  fun `find approval candidates - no prisons specified returns all`() {
    whenever(licenceRepository.findAllByStatusCode(LicenceStatus.SUBMITTED))
      .thenReturn(listOf(aLicenceEntity))

    val licenceSummaries = service.findLicencesForApprovalByPrisonCaseload(null)

    assertThat(licenceSummaries).isEqualTo(listOf(aLicenceSummary))
    verify(licenceRepository, times(1)).findAllByStatusCode(LicenceStatus.SUBMITTED)
    verify(licenceRepository, times(0)).findAllByStatusCodeAndPrisonCodeIn(any(), any())
  }

  @Test
  fun `find licences matching criteria - no parameters matches all`() {
    val licenceQueryObject = LicenceQueryObject(null, null, null, null)
    whenever(licenceRepository.findAll(any<Specification<EntityLicence>>())).thenReturn(listOf(aLicenceEntity))

    val licenceSummaries = service.findLicencesMatchingCriteria(licenceQueryObject)

    assertThat(licenceSummaries).isEqualTo(listOf(aLicenceSummary))
    verify(licenceRepository, times(1)).findAll(any<Specification<EntityLicence>>())
  }

  @Test
  fun `find licences matching criteria - multiple parameters`() {
    val licenceQueryObject = LicenceQueryObject(listOf("MDI"), listOf(LicenceStatus.APPROVED), listOf(1, 2, 3), listOf("A1234AA"))
    whenever(licenceRepository.findAll(any<Specification<EntityLicence>>())).thenReturn(listOf(aLicenceEntity))

    val licenceSummaries = service.findLicencesMatchingCriteria(licenceQueryObject)

    assertThat(licenceSummaries).isEqualTo(listOf(aLicenceSummary))
    verify(licenceRepository, times(1)).findAll(any<Specification<EntityLicence>>())
  }

  @Test
  fun `update licence status persists the licence and history correctly`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    service.updateLicenceStatus(1L, StatusUpdateRequest(status = LicenceStatus.REJECTED, username = "X"))

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val historyCaptor = ArgumentCaptor.forClass(EntityLicenceHistory::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceHistoryRepository, times(1)).saveAndFlush(historyCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername")
      .isEqualTo(listOf(1L, LicenceStatus.REJECTED, "X"))

    assertThat(historyCaptor.value)
      .extracting("licenceId", "statusCode", "actionUsername", "actionDescription")
      .isEqualTo(listOf(1L, LicenceStatus.REJECTED.name, "X", "Status changed to ${LicenceStatus.REJECTED.name}"))
  }

  @Test
  fun `update licence status to APPROVED sets additional values`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    service.updateLicenceStatus(1L, StatusUpdateRequest(status = LicenceStatus.APPROVED, username = "X", fullName = "Y"))

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val historyCaptor = ArgumentCaptor.forClass(EntityLicenceHistory::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceHistoryRepository, times(1)).saveAndFlush(historyCaptor.capture())

    assertThat(licenceCaptor.value.statusCode).isEqualTo(LicenceStatus.APPROVED)
    assertThat(licenceCaptor.value.approvedByUsername).isEqualTo("X")
    assertThat(licenceCaptor.value.approvedByName).isEqualTo("Y")
    assertThat(licenceCaptor.value.approvedDate).isAfter(LocalDateTime.now().minusMinutes(5L))

    assertThat(historyCaptor.value.statusCode).isEqualTo(LicenceStatus.APPROVED.name)
    assertThat(historyCaptor.value.actionDescription).isEqualTo("Status changed to ${LicenceStatus.APPROVED.name}")
  }

  @Test
  fun `update an APPROVED licence back to IN_PROGRESS clears the approval fields`() {
    whenever(licenceRepository.findById(1L))
      .thenReturn(Optional.of(aLicenceEntity.copy(statusCode = LicenceStatus.APPROVED, approvedByUsername = "X", approvedByName = "Y")))

    service.updateLicenceStatus(1L, StatusUpdateRequest(status = LicenceStatus.IN_PROGRESS, username = "X"))

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val historyCaptor = ArgumentCaptor.forClass(EntityLicenceHistory::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceHistoryRepository, times(1)).saveAndFlush(historyCaptor.capture())

    assertThat(licenceCaptor.value.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(licenceCaptor.value.updatedByUsername).isEqualTo("X")
    assertThat(licenceCaptor.value.approvedByName).isNull()
    assertThat(licenceCaptor.value.approvedDate).isNull()

    assertThat(historyCaptor.value.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS.name)
    assertThat(historyCaptor.value.actionDescription).isEqualTo("Status changed to ${LicenceStatus.IN_PROGRESS.name}")
  }

  @Test
  fun `update licence status throws not found exception`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.updateLicenceStatus(1L, StatusUpdateRequest(status = LicenceStatus.REJECTED, username = "X"))
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

    verify(licenceRepository, times(1)).findById(1L)
    verify(licenceRepository, times(0)).saveAndFlush(any())
    verify(licenceHistoryRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `update additional conditions throws not found exception`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.updateAdditionalConditions(1L, AdditionalConditionsRequest(additionalConditions = listOf(AdditionalCondition(code = "code", category = "category", text = "text", sequence = 0)), conditionType = "AP"))
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
              EntityAdditionalCondition(
                id = 1,
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

    val request = AdditionalConditionsRequest(additionalConditions = listOf(AdditionalCondition(code = "code", category = "category", text = "text", sequence = 0)), conditionType = "AP")

    service.updateAdditionalConditions(1L, request)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value.additionalConditions).containsExactly(
      EntityAdditionalCondition(
        id = 1, conditionCode = "code", conditionCategory = "category", conditionSequence = 0, conditionText = "text", conditionType = "AP",
        additionalConditionData = someAdditionalConditionData,
        licence = aLicenceEntity
      )
    )
  }

  @Test
  fun `update additional condition data throws not found exception if licence is not found`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.updateAdditionalConditionData(1L, 1L, UpdateAdditionalConditionDataRequest(data = listOf(ModelAdditionalConditionData(field = "field1", value = "value1", sequence = 0))))
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
              EntityAdditionalCondition(
                id = 1,
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
      service.updateAdditionalConditionData(1L, 1L, UpdateAdditionalConditionDataRequest(data = listOf(ModelAdditionalConditionData(field = "field1", value = "value1", sequence = 0))))
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
              EntityAdditionalCondition(
                id = 1,
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

    val request = UpdateAdditionalConditionDataRequest(data = listOf(ModelAdditionalConditionData(field = "field1", value = "value1", sequence = 0)))

    service.updateAdditionalConditionData(1L, 1L, request)

    val conditionCaptor = ArgumentCaptor.forClass(EntityAdditionalCondition::class.java)

    verify(additionalConditionRepository, times(1)).saveAndFlush(conditionCaptor.capture())

    assertThat(conditionCaptor.value.additionalConditionData).containsExactly(
      AdditionalConditionData(
        id = -1, additionalCondition = anAdditionalConditionEntity, dataSequence = 0, dataField = "field1", dataValue = "value1"
      )
    )
  }

  private companion object {
    val tenDaysFromNow: LocalDateTime = LocalDateTime.now().plusDays(10)

    val someBespokeConditions = BespokeConditionRequest(conditions = listOf("Condition 1", "Condition 2", "Condition 3"))

    val someStandardConditions = listOf(
      ModelStandardCondition(id = 1, code = "goodBehaviour", sequence = 1, text = "Be of good behaviour"),
      ModelStandardCondition(id = 2, code = "notBreakLaw", sequence = 2, text = "Do not break any law"),
      ModelStandardCondition(id = 3, code = "attendMeetings", sequence = 3, text = "Attend meetings"),
    )

    val someEntityStandardConditions = listOf(
      EntityStandardCondition(id = 1, conditionCode = "goodBehaviour", conditionSequence = 1, conditionText = "Be of good behaviour"),
      EntityStandardCondition(id = 2, conditionCode = "notBreakLaw", conditionSequence = 2, conditionText = "Do not break any law"),
      EntityStandardCondition(id = 3, conditionCode = "attendMeetings", conditionSequence = 3, conditionText = "Attend meetings"),
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
      comFirstName = "Stephen",
      comLastName = "Mills",
      comUsername = "X12345",
      comStaffId = 12345,
      comEmail = "stephen.mills@nps.gov.uk",
      comTelephone = "0116 2788777",
      probationAreaCode = "N01",
      probationLduCode = "LDU1",
      standardLicenceConditions = someStandardConditions,
      standardPssConditions = someStandardConditions,
    )

    val aLicenceEntity = EntityLicence(
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
      comFirstName = "Stephen",
      comLastName = "Mills",
      comUsername = "X12345",
      comStaffId = 12345,
      comEmail = "stephen.mills@nps.gov.uk",
      comTelephone = "0116 2788777",
      probationAreaCode = "N01",
      probationLduCode = "LDU1",
      dateCreated = LocalDateTime.now(),
      createdByUsername = "X12345",
      standardConditions = someEntityStandardConditions,
    )

    val someAdditionalConditionData = listOf(AdditionalConditionData(id = 1, dataField = "dataField", dataValue = "dataValue", additionalCondition = EntityAdditionalCondition(licence = aLicenceEntity)))

    val anAdditionalConditionEntity = EntityAdditionalCondition(
      id = 1,
      licence = aLicenceEntity,
      conditionCode = "code1",
      conditionCategory = "category1",
      conditionSequence = 1,
      conditionText = "text",
      additionalConditionData = someAdditionalConditionData
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
      conditionalReleaseDate = LocalDate.of(2021, 10, 22),
      actualReleaseDate = LocalDate.of(2021, 10, 22)
    )
  }
}
