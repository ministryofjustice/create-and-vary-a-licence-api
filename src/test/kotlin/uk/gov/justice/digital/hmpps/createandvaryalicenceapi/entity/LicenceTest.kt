package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LicenceTest {

  @Test
  fun `isInPssPeriod should return false if LED is null`() {
    val testLicence = Licence(
      licenceExpiryDate = null,
      topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
    )

    val isInPssPeriod = testLicence.isInPssPeriod()
    assertFalse(isInPssPeriod)
  }

  @Test
  fun `isInPssPeriod should returns false if TUSED is null`() {
    val testLicence = Licence(
      licenceExpiryDate = LocalDate.now().plusDays(1),
      topupSupervisionExpiryDate = null,
    )

    val isInPssPeriod = testLicence.isInPssPeriod()
    assertFalse(isInPssPeriod)
  }

  @Test
  fun `isInPssPeriod should returns false if both LED and TUSED is null`() {
    val testLicence = Licence(
      licenceExpiryDate = null,
      topupSupervisionExpiryDate = null,
    )

    val isInPssPeriod = testLicence.isInPssPeriod()
    assertFalse(isInPssPeriod)
  }

  @Test
  fun `isInPssPeriod should return false if LED is before now`() {
    val testLicence = Licence(
      licenceExpiryDate = LocalDate.now().plusDays(1),
      topupSupervisionExpiryDate = LocalDate.now().minusDays(1),
    )

    val isInPssPeriod = testLicence.isInPssPeriod()
    assertFalse(isInPssPeriod)
  }

  @Test
  fun `isInPssPeriod should return false if both LED and TUSED are before now`() {
    val testLicence = Licence(
      licenceExpiryDate = LocalDate.now().plusDays(1),
      topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
    )

    val isInPssPeriod = testLicence.isInPssPeriod()
    assertFalse(isInPssPeriod)
  }

  @Test
  fun `isInPssPeriod should return true if LED less than TODAY less than TUSED`() {
    val testLicence = Licence(
      licenceExpiryDate = LocalDate.now().minusDays(1),
      topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
    )

    val isInPssPeriod = testLicence.isInPssPeriod()
    assertTrue(isInPssPeriod)
  }

  @Test
  fun `isInPssPeriod should return true if LED less than TODAY equals TUSED`() {
    val testLicence = Licence(
      licenceExpiryDate = LocalDate.now().minusDays(1),
      topupSupervisionExpiryDate = LocalDate.now(),
    )

    val isInPssPeriod = testLicence.isInPssPeriod()
    assertTrue(isInPssPeriod)
  }
}
