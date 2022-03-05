package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.mapping.PropertyReferenceException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentPersonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentTimeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ContactNumberRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StatusUpdateRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateAdditionalConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ReferVariationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdatePrisonInformationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateReasonForVariationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSentenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSpoDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateVloDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.BespokeConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.CommunityOffenderManagerRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StandardConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import javax.persistence.EntityNotFoundException
import javax.validation.ValidationException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition as EntityAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent as EntityAuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeCondition as EntityBespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent as EntityLicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition as EntityStandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData as ModelAdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence as ModelLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition as ModelStandardCondition

class LicenceServiceTest {
  private val standardConditionRepository = mock<StandardConditionRepository>()
  private val additionalConditionRepository = mock<AdditionalConditionRepository>()
  private val bespokeConditionRepository = mock<BespokeConditionRepository>()
  private val licenceRepository = mock<LicenceRepository>()
  private val communityOffenderManagerRepository = mock<CommunityOffenderManagerRepository>()
  private val licenceEventRepository = mock<LicenceEventRepository>()
  private val additionalConditionUploadDetailRepository = mock<AdditionalConditionUploadDetailRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()
  private val notifyService = mock<NotifyService>()

  private val service = LicenceService(
    licenceRepository,
    communityOffenderManagerRepository,
    standardConditionRepository,
    additionalConditionRepository,
    bespokeConditionRepository,
    licenceEventRepository,
    additionalConditionUploadDetailRepository,
    auditEventRepository,
    notifyService,
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
      standardConditionRepository,
      bespokeConditionRepository,
      licenceEventRepository,
      additionalConditionUploadDetailRepository,
      auditEventRepository,
      notifyService,
    )
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
    val expectedCom = CommunityOffenderManager(staffIdentifier = 2000, username = "smills", email = "testemail@probation.gov.uk", firstName = "X", lastName = "Y")

    whenever(standardConditionRepository.saveAllAndFlush(anyList())).thenReturn(someEntityStandardConditions)
    whenever(licenceRepository.saveAndFlush(any())).thenReturn(aLicenceEntity)
    whenever(communityOffenderManagerRepository.findByStaffIdentifier(2000)).thenReturn(expectedCom)
    whenever(communityOffenderManagerRepository.findByUsernameIgnoreCase("smills")).thenReturn(expectedCom)

    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

    val createResponse = service.createLicence(aCreateLicenceRequest)

    assertThat(createResponse.licenceStatus).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(createResponse.licenceType).isEqualTo(LicenceType.AP)

    verify(standardConditionRepository, times(1)).saveAllAndFlush(anyList())
    verify(licenceRepository, times(1)).saveAndFlush(any())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(listOf(1L, "smills", "X Y", "Licence created for ${aCreateLicenceRequest.forename} ${aCreateLicenceRequest.surname}"))

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "username", "forenames", "surname", "eventDescription")
      .isEqualTo(listOf(1L, LicenceEventType.CREATED, "smills", "X", "Y", "Licence created for ${aLicenceEntity.forename} ${aLicenceEntity.surname}"))
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
    verify(auditEventRepository, times(0)).saveAndFlush(any())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `update initial appointment person persists updated entity correctly`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    service.updateAppointmentPerson(1L, AppointmentPersonRequest(appointmentPerson = "John Smith"))

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("appointmentPerson", "updatedByUsername")
      .isEqualTo(listOf("John Smith", "smills"))
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

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("appointmentTime", "updatedByUsername")
      .isEqualTo(listOf(tenDaysFromNow, "smills"))
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

    service.updateContactNumber(1L, ContactNumberRequest(telephone = "0114 2565555"))

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("appointmentContact", "updatedByUsername")
      .isEqualTo(listOf("0114 2565555", "smills"))
  }

  @Test
  fun `update contact number throws not found exception if licence not found`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.updateContactNumber(1L, ContactNumberRequest(telephone = "0114 2565555"))
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

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("appointmentAddress", "updatedByUsername")
      .isEqualTo(listOf("221B Baker Street, London, City of London, NW1 6XE", "smills"))
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

    val bespokeEntities = listOf(
      EntityBespokeCondition(id = -1L, licence = aLicenceEntity, conditionSequence = 0, conditionText = "Condition 1"),
      EntityBespokeCondition(id = -1L, licence = aLicenceEntity, conditionSequence = 1, conditionText = "Condition 2"),
      EntityBespokeCondition(id = -1L, licence = aLicenceEntity, conditionSequence = 2, conditionText = "Condition 3"),
    )

    bespokeEntities.forEach { bespoke ->
      whenever(bespokeConditionRepository.saveAndFlush(bespoke)).thenReturn(bespoke)
    }

    service.updateBespokeConditions(1L, someBespokeConditions)

    // Verify licence entity is updated with last contact info
    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value).extracting("updatedByUsername").isEqualTo("smills")
    assertThat(licenceCaptor.value).extracting("bespokeConditions").isEqualTo(emptyList<EntityBespokeCondition>())

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
  fun `find licences matching criteria - no parameters matches all`() {
    val licenceQueryObject = LicenceQueryObject()
    whenever(licenceRepository.findAll(any<Specification<EntityLicence>>(), any<Sort>())).thenReturn(listOf(aLicenceEntity))

    val licenceSummaries = service.findLicencesMatchingCriteria(licenceQueryObject)

    assertThat(licenceSummaries).isEqualTo(listOf(aLicenceSummary))
    verify(licenceRepository, times(1)).findAll(any<Specification<EntityLicence>>(), eq(Sort.unsorted()))
  }

  @Test
  fun `find licences matching criteria - multiple parameters`() {
    val licenceQueryObject = LicenceQueryObject(
      prisonCodes = listOf("MDI"),
      statusCodes = listOf(LicenceStatus.APPROVED),
      staffIds = listOf(1, 2, 3),
      nomsIds = listOf("A1234AA"),
    )
    whenever(licenceRepository.findAll(any<Specification<EntityLicence>>(), any<Sort>())).thenReturn(listOf(aLicenceEntity))

    val licenceSummaries = service.findLicencesMatchingCriteria(licenceQueryObject)

    assertThat(licenceSummaries).isEqualTo(listOf(aLicenceSummary))
    verify(licenceRepository, times(1)).findAll(any<Specification<EntityLicence>>(), eq(Sort.unsorted()))
  }

  @Test
  fun `find licences matching criteria - list of PDUs`() {
    val licenceQueryObject = LicenceQueryObject(pdus = listOf("A", "B"))
    whenever(licenceRepository.findAll(any<Specification<EntityLicence>>(), any<Sort>())).thenReturn(listOf(aLicenceEntity))

    val licenceSummaries = service.findLicencesMatchingCriteria(licenceQueryObject)

    assertThat(licenceSummaries).isEqualTo(listOf(aLicenceSummary))
    verify(licenceRepository, times(1)).findAll(any<Specification<EntityLicence>>(), eq(Sort.unsorted()))
  }

  @Test
  fun `find licences matching criteria - sorted`() {
    val licenceQueryObject = LicenceQueryObject(sortBy = "conditionalReleaseDate", sortOrder = "DESC")
    whenever(licenceRepository.findAll(any<Specification<EntityLicence>>(), any<Sort>())).thenReturn(listOf(aLicenceEntity))

    val licenceSummaries = service.findLicencesMatchingCriteria(licenceQueryObject)

    assertThat(licenceSummaries).isEqualTo(listOf(aLicenceSummary))
    verify(licenceRepository, times(1)).findAll(any<Specification<EntityLicence>>(), eq(Sort.by(Sort.Direction.DESC, "conditionalReleaseDate")))
  }

  @Test
  fun `find licences matching criteria - unknown property`() {
    val licenceQueryObject = LicenceQueryObject(sortBy = "unknown", sortOrder = "DESC")
    whenever(licenceRepository.findAll(any<Specification<EntityLicence>>(), any<Sort>())).thenThrow(mock<PropertyReferenceException>())

    assertThrows<ValidationException> {
      service.findLicencesMatchingCriteria(licenceQueryObject)
    }
  }

  @Test
  fun `find licences matching criteria - unknown sort order`() {
    val licenceQueryObject = LicenceQueryObject(sortBy = "conditionalReleaseDate", sortOrder = "unknown")
    whenever(licenceRepository.findAll(any<Specification<EntityLicence>>(), any<Sort>())).thenReturn(listOf(aLicenceEntity))

    assertThrows<ValidationException> {
      service.findLicencesMatchingCriteria(licenceQueryObject)
    }
  }

  @Test
  fun `update licence status persists the licence and history correctly`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    service.updateLicenceStatus(1L, StatusUpdateRequest(status = LicenceStatus.REJECTED, username = "X", fullName = "Y"))

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername")
      .isEqualTo(listOf(1L, LicenceStatus.REJECTED, "X"))

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(listOf(1L, "X", "Y", "Licence rejected for ${aLicenceEntity.forename} ${aLicenceEntity.surname}"))
  }

  @Test
  fun `update licence status to APPROVED sets additional values`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    service.updateLicenceStatus(1L, StatusUpdateRequest(status = LicenceStatus.APPROVED, username = "X", fullName = "Y"))

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "approvedByUsername", "approvedByName")
      .isEqualTo(listOf(1L, LicenceStatus.APPROVED, "X", "Y"))

    assertThat(licenceCaptor.value.approvedDate).isAfter(LocalDateTime.now().minusMinutes(5L))

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "eventDescription")
      .isEqualTo(listOf(1L, LicenceEventType.APPROVED, "Licence updated to APPROVED for ${aLicenceEntity.forename} ${aLicenceEntity.surname}"))

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(listOf(1L, "X", "Y", "Licence approved for ${aLicenceEntity.forename} ${aLicenceEntity.surname}"))

    verify(notifyService, times(1)).sendLicenceApprovedEmail(
      "testemail@probation.gov.uk",
      mapOf(
        Pair("fullName", "${aLicenceEntity.forename} ${aLicenceEntity.surname}"),
        Pair("prisonName", aLicenceEntity.prisonDescription.orEmpty())
      ),
      "1",
    )
  }

  @Test
  fun `update an APPROVED licence back to IN_PROGRESS clears the approval fields`() {
    whenever(licenceRepository.findById(1L))
      .thenReturn(Optional.of(aLicenceEntity.copy(statusCode = LicenceStatus.APPROVED, approvedByUsername = "X", approvedByName = "Y")))

    service.updateLicenceStatus(1L, StatusUpdateRequest(status = LicenceStatus.IN_PROGRESS, username = "X", fullName = "Y"))

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername", "approvedByUsername", "approvedDate")
      .isEqualTo(listOf(1L, LicenceStatus.IN_PROGRESS, "X", null, null))

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(listOf(1L, "X", "Y", "Licence edited for ${aLicenceEntity.forename} ${aLicenceEntity.surname}"))
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
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `submit licence throws not found exception`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.submitLicence(1L)
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

    verify(licenceRepository, times(1)).findById(1L)
    verify(licenceRepository, times(0)).saveAndFlush(any())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `submit a licence saves new fields to the licence`() {
    val expectedCom = CommunityOffenderManager(staffIdentifier = 2000, username = "smills", email = "testemail@probation.gov.uk", firstName = "X", lastName = "Y")

    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(communityOffenderManagerRepository.findByUsernameIgnoreCase("smills")).thenReturn(expectedCom)

    service.submitLicence(1L)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername")
      .isEqualTo(listOf(1L, LicenceStatus.SUBMITTED, "smills"))

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "eventDescription")
      .isEqualTo(listOf(1L, LicenceEventType.SUBMITTED, "Licence submitted for approval for ${aLicenceEntity.forename} ${aLicenceEntity.surname}"))

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(listOf(1L, "smills", "X Y", "Licence submitted for approval for ${aLicenceEntity.forename} ${aLicenceEntity.surname}"))
  }

  @Test
  fun `submitting a licence variation`() {
    val expectedCom = CommunityOffenderManager(staffIdentifier = 2000, username = "smills", email = "testemail@probation.gov.uk", firstName = "X", lastName = "Y")
    val licence = aLicenceEntity.copy(variationOfId = 1)

    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(licence))
    whenever(communityOffenderManagerRepository.findByUsernameIgnoreCase("smills")).thenReturn(expectedCom)

    service.submitLicence(1L)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("id", "statusCode", "updatedByUsername")
      .isEqualTo(listOf(1L, LicenceStatus.VARIATION_SUBMITTED, "smills"))

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "eventDescription")
      .isEqualTo(listOf(1L, LicenceEventType.VARIATION_SUBMITTED, "Licence submitted for approval for ${aLicenceEntity.forename} ${aLicenceEntity.surname}"))

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(listOf(1L, "smills", "X Y", "Licence submitted for approval for ${aLicenceEntity.forename} ${aLicenceEntity.surname}"))
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

    // Verify last contact info is recorded
    assertThat(licenceCaptor.value.updatedByUsername).isEqualTo("smills")
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
  fun `activate licences sets licence statuses to ACTIVE`() {
    whenever(licenceRepository.findAllById(listOf(1))).thenReturn(listOf(aLicenceEntity.copy(statusCode = LicenceStatus.APPROVED)))

    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

    service.activateLicences(listOf(1L))

    verify(licenceRepository, times(1)).findAllById(listOf(1L))
    verify(licenceRepository, times(1)).saveAllAndFlush(listOf(aLicenceEntity.copy(statusCode = LicenceStatus.ACTIVE)))
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(listOf(1L, "SYSTEM", "SYSTEM", "Licence automatically activated for ${aLicenceEntity.forename} ${aLicenceEntity.surname}"))

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "forenames", "surname")
      .isEqualTo(listOf(1L, LicenceEventType.ACTIVATED, "SYSTEM", "SYSTEM"))
  }

  @Test
  fun `activate licences does not activate if status is not approved`() {
    whenever(licenceRepository.findAllById(listOf(1))).thenReturn(listOf(aLicenceEntity.copy(statusCode = LicenceStatus.IN_PROGRESS)))

    service.activateLicences(listOf(1L))

    verify(licenceRepository, times(1)).findAllById(listOf(1L))
    verify(licenceRepository, times(0)).saveAllAndFlush(anyList())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
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
    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    verify(additionalConditionRepository, times(1)).saveAndFlush(conditionCaptor.capture())

    assertThat(conditionCaptor.value.additionalConditionData).containsExactly(
      AdditionalConditionData(
        id = -1, additionalCondition = anAdditionalConditionEntity, dataSequence = 0, dataField = "field1", dataValue = "value1"
      )
    )

    // Verify last contact info is recorded
    assertThat(licenceCaptor.value.updatedByUsername).isEqualTo("smills")
  }

  @Test
  fun `update spo discussion persists the updated entity`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    service.updateSpoDiscussion(1L, UpdateSpoDiscussionRequest(spoDiscussion = "Yes"))

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("spoDiscussion", "updatedByUsername")
      .isEqualTo(listOf("Yes", "smills"))
  }

  @Test
  fun `update vlo discussion persists the updated entity`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    service.updateVloDiscussion(1L, UpdateVloDiscussionRequest(vloDiscussion = "Yes"))

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("vloDiscussion", "updatedByUsername")
      .isEqualTo(listOf("Yes", "smills"))
  }

  @Test
  fun `update reason for variation creates a licence event containing the reason`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)

    service.updateReasonForVariation(1L, UpdateReasonForVariationRequest(reasonForVariation = "reason"))

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())

    assertThat(licenceCaptor.value).extracting("updatedByUsername").isEqualTo("smills")

    assertThat(eventCaptor.value)
      .extracting("eventType", "username", "eventDescription")
      .isEqualTo(listOf(LicenceEventType.VARIATION_SUBMITTED_REASON, "smills", "reason"))
  }

  @Test
  fun `discarding a licence`() {
    val expectedCom = CommunityOffenderManager(staffIdentifier = 2000, username = "smills", email = "testemail@probation.gov.uk", firstName = "X", lastName = "Y")
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(communityOffenderManagerRepository.findByUsernameIgnoreCase("smills")).thenReturn(expectedCom)

    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    service.discardLicence(1L)

    verify(licenceRepository, times(1)).delete(aLicenceEntity)
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(listOf(1L, "smills", "X Y", "Licence variation discarded for ${aLicenceEntity.forename} ${aLicenceEntity.surname}"))
  }

  @Test
  fun `update prison information persists the updated entity`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    service.updatePrisonInformation(1L, UpdatePrisonInformationRequest(prisonCode = "PVI", prisonDescription = "Pentonville (HMP)", prisonTelephone = "+44 276 54545"))

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("prisonCode", "prisonDescription", "prisonTelephone", "updatedByUsername")
      .isEqualTo(listOf("PVI", "Pentonville (HMP)", "+44 276 54545", "smills"))

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(listOf(1L, "SYSTEM", "SYSTEM", "Prison information updated for ${aLicenceEntity.forename} ${aLicenceEntity.surname}"))
  }

  @Test
  fun `update sentence dates persists the updated entity`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))

    service.updateSentenceDates(
      1L,
      UpdateSentenceDatesRequest(
        conditionalReleaseDate = LocalDate.parse("2023-09-11"),
        actualReleaseDate = LocalDate.parse("2023-09-11"),
        sentenceStartDate = LocalDate.parse("2021-09-11"),
        sentenceEndDate = LocalDate.parse("2024-09-11"),
        licenceStartDate = LocalDate.parse("2023-09-11"),
        licenceExpiryDate = LocalDate.parse("2024-09-11"),
        topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
        topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
      )
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting(
        "conditionalReleaseDate",
        "actualReleaseDate",
        "sentenceStartDate",
        "sentenceEndDate",
        "licenceStartDate",
        "licenceExpiryDate",
        "topupSupervisionStartDate",
        "topupSupervisionExpiryDate",
        "updatedByUsername"
      )
      .isEqualTo(
        listOf(
          LocalDate.parse("2023-09-11"),
          LocalDate.parse("2023-09-11"),
          LocalDate.parse("2021-09-11"),
          LocalDate.parse("2024-09-11"),
          LocalDate.parse("2023-09-11"),
          LocalDate.parse("2024-09-11"),
          LocalDate.parse("2024-09-11"),
          LocalDate.parse("2025-09-11"),
          "smills"
        )
      )

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(listOf(1L, "SYSTEM", "SYSTEM", "Sentence dates updated for ${aLicenceEntity.forename} ${aLicenceEntity.surname}"))
  }

  @Test
  fun `referring a licence variation`() {
    val referVariationRequest = ReferVariationRequest(reasonForReferral = "reason")
    val expectedCom = CommunityOffenderManager(staffIdentifier = 2000, username = "smills", email = "testemail@probation.gov.uk", firstName = "X", lastName = "Y")
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(communityOffenderManagerRepository.findByUsernameIgnoreCase("smills")).thenReturn(expectedCom)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    service.referLicenceVariation(1L, referVariationRequest)

    verify(licenceRepository, times(1)).findById(1L)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("statusCode", "updatedByUsername")
      .isEqualTo(listOf(LicenceStatus.VARIATION_REJECTED, "smills"))

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "username", "eventDescription")
      .isEqualTo(listOf(1L, LicenceEventType.VARIATION_REFERRED, "smills", "reason"))

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(listOf(1L, "smills", "X Y", "Licence variation rejected for ${aLicenceEntity.forename} ${aLicenceEntity.surname}"))
  }

  @Test
  fun `approving a licence variation`() {
    val expectedCom = CommunityOffenderManager(staffIdentifier = 2000, username = "smills", email = "testemail@probation.gov.uk", firstName = "X", lastName = "Y")

    whenever(licenceRepository.findById(2L)).thenReturn(Optional.of(aLicenceEntity.copy(id = 2, variationOfId = 1L)))
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(communityOffenderManagerRepository.findByUsernameIgnoreCase("smills")).thenReturn(expectedCom)

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    val eventCaptor = ArgumentCaptor.forClass(EntityLicenceEvent::class.java)
    val auditCaptor = ArgumentCaptor.forClass(EntityAuditEvent::class.java)

    service.approveLicenceVariation(2L)

    verify(licenceRepository, times(1)).findById(2L)
    verify(licenceRepository, times(1)).findById(1L)

    // Capture all calls to licence, history and audit saveAndFlush - they will be a list
    verify(licenceRepository, times(2)).saveAndFlush(licenceCaptor.capture())
    verify(licenceEventRepository, times(2)).saveAndFlush(eventCaptor.capture())
    verify(auditEventRepository, times(2)).saveAndFlush(auditCaptor.capture())

    // Check all calls were made
    assertThat(licenceCaptor.allValues.size).isEqualTo(2)
    assertThat(eventCaptor.allValues.size).isEqualTo(2)
    assertThat(auditCaptor.allValues.size).isEqualTo(2)

    assertThat(licenceCaptor.allValues[0])
      .extracting("id", "statusCode", "updatedByUsername")
      .isEqualTo(listOf(2L, LicenceStatus.ACTIVE, "smills"))

    assertThat(licenceCaptor.allValues[1])
      .extracting("id", "statusCode", "updatedByUsername")
      .isEqualTo(listOf(1L, LicenceStatus.INACTIVE, "smills"))

    assertThat(eventCaptor.allValues[0])
      .extracting("licenceId", "eventType", "username")
      .isEqualTo(listOf(2L, LicenceEventType.VARIATION_APPROVED, "smills"))

    assertThat(eventCaptor.allValues[1])
      .extracting("licenceId", "eventType", "username")
      .isEqualTo(listOf(1L, LicenceEventType.SUPERSEDED, "smills"))

    assertThat(auditCaptor.allValues[0])
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(listOf(2L, "smills", "X Y", "Licence variation approved for ${aLicenceEntity.forename} ${aLicenceEntity.surname}"))

    assertThat(auditCaptor.allValues[1])
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(listOf(1L, "smills", "X Y", "Licence superseded for ${aLicenceEntity.forename} ${aLicenceEntity.surname} by ID 2"))
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
      EntityStandardCondition(id = 1, conditionCode = "goodBehaviour", conditionSequence = 1, conditionText = "Be of good behaviour", licence = mock()),
      EntityStandardCondition(id = 2, conditionCode = "notBreakLaw", conditionSequence = 2, conditionText = "Do not break any law", licence = mock()),
      EntityStandardCondition(id = 3, conditionCode = "attendMeetings", conditionSequence = 3, conditionText = "Attend meetings", licence = mock()),
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
      responsibleComStaffId = 2000
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
      probationAreaCode = "N01",
      probationAreaDescription = "Wales",
      probationPduCode = "N01A",
      probationPduDescription = "Cardiff",
      probationLauCode = "N01A2",
      probationLauDescription = "Cardiff South",
      probationTeamCode = "NA01A2-A",
      probationTeamDescription = "Cardiff South Team A",
      dateCreated = LocalDateTime.now(),
      standardConditions = someEntityStandardConditions,
      mailingList = mutableSetOf(CommunityOffenderManager(staffIdentifier = 2000, username = "smills", email = "testemail@probation.gov.uk", firstName = "X", lastName = "Y")),
      responsibleCom = CommunityOffenderManager(staffIdentifier = 2000, username = "smills", email = "testemail@probation.gov.uk", firstName = "X", lastName = "Y"),
      createdBy = CommunityOffenderManager(staffIdentifier = 2000, username = "smills", email = "testemail@probation.gov.uk", firstName = "X", lastName = "Y"),
    )

    val someAdditionalConditionData = listOf(AdditionalConditionData(id = 1, dataField = "dataField", dataValue = "dataValue", additionalCondition = EntityAdditionalCondition(licence = aLicenceEntity)))

    val anAdditionalConditionEntity = EntityAdditionalCondition(
      id = 1,
      licence = aLicenceEntity,
      conditionCode = "code1",
      conditionCategory = "category1",
      conditionSequence = 1,
      conditionText = "text",
      additionalConditionData = someAdditionalConditionData,
      additionalConditionUploadSummary = emptyList(),
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
      conditionalReleaseDate = LocalDate.of(2021, 10, 22),
      actualReleaseDate = LocalDate.of(2021, 10, 22),
      comUsername = "smills",
    )
  }
}
