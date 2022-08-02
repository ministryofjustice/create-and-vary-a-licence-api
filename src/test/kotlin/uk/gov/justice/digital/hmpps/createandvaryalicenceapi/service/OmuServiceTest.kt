package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.OmuContact
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateOmuEmailRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.OmuContactRepository
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException

class OmuServiceTest {
  private val omuContactRepository = mock<OmuContactRepository>()
  private val omuService = OmuService(omuContactRepository)

  @BeforeEach
  fun reset() {
    reset(omuContactRepository)
  }

  @Test
  fun `gets existing OMU email`() {
    val expectedOmuContact =
      OmuContact(email = "test.omu@testing.com", dateCreated = LocalDateTime.now(), prisonCode = "FPI")
    whenever(omuContactRepository.findByPrisonCode("FPI")).thenReturn(expectedOmuContact)
    val result = omuService.getOmuContactEmail("FPI")

    verify(omuContactRepository, times(1)).findByPrisonCode("FPI")
    assertThat(result).isEqualTo(expectedOmuContact)
  }

  @Test
  fun `return 404 when OMU email does not exist`() {
    whenever(omuContactRepository.findByPrisonCode("FPI")).thenThrow(EntityNotFoundException("FPI"))

    val exception = assertThrows<EntityNotFoundException> {
      omuContactRepository.findByPrisonCode("FPI")
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)
    verify(omuContactRepository, times(1)).findByPrisonCode("FPI")
  }

  @Test
  fun `updates existing OMU email`() {
    val expectedOmuContact =
      OmuContact(email = "test.omu@testing.com", dateCreated = LocalDateTime.now(), prisonCode = "FPI")
    whenever(omuContactRepository.saveAndFlush(any())).thenReturn(expectedOmuContact)
    val omuContactRequest = UpdateOmuEmailRequest(email = "test.omu@testing.com")

    val result = omuService.updateOmuEmail("FPI", omuContactRequest)

    verify(omuContactRepository, times(1)).findByPrisonCode("FPI")
    verify(omuContactRepository, times(1)).saveAndFlush(any())
    assertThat(result).isEqualTo(expectedOmuContact)
  }

  @Test
  fun `create new OMU email if one does not exist`() {
    val expectedOmuContact =
      OmuContact(email = "test.omu@testing.com", dateCreated = LocalDateTime.now(), prisonCode = "FPI")
    whenever(omuContactRepository.findByPrisonCode("FPI")).thenReturn(null)
    whenever(omuContactRepository.saveAndFlush(any())).thenReturn(expectedOmuContact)
    val omuContactRequest = UpdateOmuEmailRequest(email = "test.omu@testing.com")

    val result = omuService.updateOmuEmail("FPI", omuContactRequest)

    verify(omuContactRepository, times(1)).findByPrisonCode("FPI")
    verify(omuContactRepository, times(1)).saveAndFlush(any())
    assertThat(result).isEqualTo(expectedOmuContact)
  }

  @Test
  fun `deletes OMU email if one exists`() {
    val expectedOmuContact =
      OmuContact(email = "test.omu@testing.com", dateCreated = LocalDateTime.now(), prisonCode = "FPI")
    whenever(omuContactRepository.findByPrisonCode("FPI")).thenReturn(expectedOmuContact)

    omuService.deleteOmuEmail("FPI")

    verify(omuContactRepository, times(1)).findByPrisonCode("FPI")
    verify(omuContactRepository, times(1)).delete((expectedOmuContact))
  }

  @Test
  fun `return 404 when trying to delete OMU email that does not exists`() {
    whenever(omuContactRepository.findByPrisonCode("FPI")).thenReturn(null)

    val exception = assertThrows<EntityNotFoundException> {
      omuService.deleteOmuEmail("FPI")
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)
    verify(omuContactRepository, times(1)).findByPrisonCode("FPI")
    verify(omuContactRepository, times(0)).delete((any()))
  }
}
