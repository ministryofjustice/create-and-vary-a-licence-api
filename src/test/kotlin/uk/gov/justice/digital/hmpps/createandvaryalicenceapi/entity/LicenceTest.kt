package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import java.time.LocalDate
import java.time.LocalDateTime

class LicenceTest {

  @Test
  fun `isInPssPeriod should return false if LED is null`() {
    val testLicence = createCrdLicence().copy(
      licenceExpiryDate = null,
      topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
    )

    val isInPssPeriod = testLicence.isInPssPeriod()
    assertFalse(isInPssPeriod)
  }

  @Test
  fun `isInPssPeriod should returns false if TUSED is null`() {
    val testLicence = createCrdLicence().copy(
      licenceExpiryDate = LocalDate.now().plusDays(1),
      topupSupervisionExpiryDate = null,
    )

    val isInPssPeriod = testLicence.isInPssPeriod()
    assertFalse(isInPssPeriod)
  }

  @Test
  fun `isInPssPeriod should returns false if both LED and TUSED is null`() {
    val testLicence = createCrdLicence().copy(
      licenceExpiryDate = null,
      topupSupervisionExpiryDate = null,
    )

    val isInPssPeriod = testLicence.isInPssPeriod()
    assertFalse(isInPssPeriod)
  }

  @Test
  fun `isInPssPeriod should return false if LED is before now`() {
    val testLicence = createCrdLicence().copy(
      licenceExpiryDate = LocalDate.now().plusDays(1),
      topupSupervisionExpiryDate = LocalDate.now().minusDays(1),
    )

    val isInPssPeriod = testLicence.isInPssPeriod()
    assertFalse(isInPssPeriod)
  }

  @Test
  fun `isInPssPeriod should return false if both LED and TUSED are before now`() {
    val testLicence = createCrdLicence().copy(
      licenceExpiryDate = LocalDate.now().plusDays(1),
      topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
    )

    val isInPssPeriod = testLicence.isInPssPeriod()
    assertFalse(isInPssPeriod)
  }

  @Test
  fun `isInPssPeriod should return true if LED less than TODAY less than TUSED`() {
    val testLicence = createCrdLicence().copy(
      licenceExpiryDate = LocalDate.now().minusDays(1),
      topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
    )

    val isInPssPeriod = testLicence.isInPssPeriod()
    assertTrue(isInPssPeriod)
  }

  @Test
  fun `isInPssPeriod should return true if LED less than TODAY equals TUSED`() {
    val testLicence = createCrdLicence().copy(
      licenceExpiryDate = LocalDate.now().minusDays(1),
      topupSupervisionExpiryDate = LocalDate.now(),
    )

    val isInPssPeriod = testLicence.isInPssPeriod()
    assertTrue(isInPssPeriod)
  }

  @Test
  fun `isActivatedInPssPeriod should return false if LED is null`() {
    val testLicence = createCrdLicence().copy(
      licenceExpiryDate = null,
      topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
      licenceActivatedDate = LocalDateTime.now(),
    )

    val isActivatedInPssPeriod = testLicence.isActivatedInPssPeriod()
    assertFalse(isActivatedInPssPeriod)
  }

  @Test
  fun `isActivatedInPssPeriod should returns false if TUSED is null`() {
    val testLicence = createCrdLicence().copy(
      licenceExpiryDate = LocalDate.now().plusDays(1),
      topupSupervisionExpiryDate = null,
      licenceActivatedDate = LocalDateTime.now(),
    )

    val isActivatedInPssPeriod = testLicence.isActivatedInPssPeriod()
    assertFalse(isActivatedInPssPeriod)
  }

  @Test
  fun `isActivatedInPssPeriod should returns false if LAD is null`() {
    val testLicence = createCrdLicence().copy(
      licenceExpiryDate = LocalDate.now().plusDays(1),
      topupSupervisionExpiryDate = LocalDate.now(),
      licenceActivatedDate = null,
    )

    val isActivatedInPssPeriod = testLicence.isActivatedInPssPeriod()
    assertFalse(isActivatedInPssPeriod)
  }

  @Test
  fun `isInPssPeriod should returns false if all LAD, LED and TUSED is null`() {
    val testLicence = createCrdLicence().copy(
      licenceExpiryDate = null,
      topupSupervisionExpiryDate = null,
      licenceActivatedDate = null,
    )

    val isActivatedInPssPeriod = testLicence.isActivatedInPssPeriod()
    assertFalse(isActivatedInPssPeriod)
  }

  @Test
  fun `isActivatedInPssPeriod should return false if LED is before LAD`() {
    val testLicence = createCrdLicence().copy(
      licenceExpiryDate = LocalDate.now().plusDays(1),
      topupSupervisionExpiryDate = LocalDate.now().minusDays(1),
      licenceActivatedDate = LocalDateTime.now(),
    )

    val isActivatedInPssPeriod = testLicence.isActivatedInPssPeriod()
    assertFalse(isActivatedInPssPeriod)
  }

  @Test
  fun `isActivatedInPssPeriod should return false if both LED and TUSED are before LAD`() {
    val testLicence = createCrdLicence().copy(
      licenceExpiryDate = LocalDate.now().plusDays(1),
      topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
      licenceActivatedDate = LocalDateTime.now(),
    )

    val isActivatedInPssPeriod = testLicence.isActivatedInPssPeriod()
    assertFalse(isActivatedInPssPeriod)
  }

  @Test
  fun `isActivatedInPssPeriod should return true if LED less than LAD less than TUSED`() {
    val testLicence = createCrdLicence().copy(
      licenceExpiryDate = LocalDate.now().minusDays(1),
      topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
      licenceActivatedDate = LocalDateTime.now(),
    )

    val isActivatedInPssPeriod = testLicence.isActivatedInPssPeriod()
    assertTrue(isActivatedInPssPeriod)
  }

  @Test
  fun `isActivatedInPssPeriod should return true if LED less than LAD equals TUSED`() {
    val testLicence = createCrdLicence().copy(
      licenceExpiryDate = LocalDate.now().minusDays(1),
      topupSupervisionExpiryDate = LocalDate.now(),
      licenceActivatedDate = LocalDateTime.now(),
    )

    val isActivatedInPssPeriod = testLicence.isActivatedInPssPeriod()
    assertTrue(isActivatedInPssPeriod)
  }
}
