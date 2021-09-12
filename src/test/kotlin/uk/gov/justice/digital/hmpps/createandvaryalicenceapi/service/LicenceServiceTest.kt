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
import org.mockito.ArgumentMatchers.anyList
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardTerm
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CreateLicenceRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceStandardTermsRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import javax.persistence.EntityNotFoundException
import javax.validation.ValidationException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence as ModelLicence

class LicenceServiceTest {
  private val standardTermsRepository = mock<LicenceStandardTermsRepository>()
  private val licenceRepository = mock<LicenceRepository>()
  private val service = LicenceService(licenceRepository, standardTermsRepository)

  @BeforeEach
  fun reset() {
    reset(licenceRepository, standardTermsRepository)
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
    whenever(standardTermsRepository.saveAllAndFlush(anyList())).thenReturn(someEntityStandardTerms)
    whenever(licenceRepository.saveAndFlush(any())).thenReturn(aLicenceEntity)
    val createResponse = service.createLicence(aCreateLicenceRequest)
    assertThat(createResponse.licenceStatus).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(createResponse.licenceType).isEqualTo(LicenceType.AP)
    verify(standardTermsRepository, times(1)).saveAllAndFlush(anyList())
    verify(licenceRepository, times(1)).saveAndFlush(any())
  }

  @Test
  fun `service throws a validation exception if an in progress licence exists for this person`() {
    whenever(
      licenceRepository
        .findAllByNomsIdAndStatusCodeIn(
          aCreateLicenceRequest.nomsId!!,
          listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED, LicenceStatus.REJECTED)
        )
    ).thenReturn(listOf(aLicenceEntity))

    val exception = assertThrows<ValidationException> {
      service.createLicence(aCreateLicenceRequest)
    }

    assertThat(exception)
      .isInstanceOf(ValidationException::class.java)
      .withFailMessage("A licence already exists for this person (IN_PROGRESS, SUBMITTED or REJECTED)")

    verify(licenceRepository, times(0)).saveAndFlush(any())
    verify(standardTermsRepository, times(0)).saveAllAndFlush(anyList())
  }

  private companion object {
    val someStandardConditions = listOf(
      StandardCondition(id = 1, code = "goodBehaviour", sequence = 1, text = "Be of good behaviour"),
      StandardCondition(id = 2, code = "notBreakLaw", sequence = 1, text = "Do not break any law"),
      StandardCondition(id = 3, code = "attendMeetings", sequence = 1, text = "Attend meetings"),
    )

    val someEntityStandardTerms = listOf(
      StandardTerm(id = 1, termCode = "goodBehaviour", termSequence = 1, termText = "Be of good behaviour"),
      StandardTerm(id = 2, termCode = "notBreakLaw", termSequence = 1, termText = "Do not break any law"),
      StandardTerm(id = 3, termCode = "attendMeetings", termSequence = 1, termText = "Attend meetings"),
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
      comFirstName = "Stephen",
      comLastName = "Mills",
      comUsername = "X12345",
      comStaffId = 12345,
      comEmail = "stephen.mills@nps.gov.uk",
      comTelephone = "0116 2788777",
      probationAreaCode = "N01",
      probationLduCode = "LDU1",
      standardConditions = someStandardConditions,
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
      comFirstName = "Stephen",
      comLastName = "Mills",
      comUsername = "X12345",
      comStaffId = 12345,
      comEmail = "stephen.mills@nps.gov.uk",
      comTelephone = "0116 2788777",
      probationAreaCode = "N01",
      probationLduCode = "LDU1",
      dateCreated = LocalDateTime.now(),
      createByUsername = "X12345",
      standardTerms = someEntityStandardTerms,
    )
  }
}
