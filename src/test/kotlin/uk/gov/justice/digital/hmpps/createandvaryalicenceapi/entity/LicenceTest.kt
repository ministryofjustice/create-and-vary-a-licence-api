package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

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

  @Test
  fun `isActivatedInPssPeriod should return false if LED is null`() {
    val testLicence = Licence(
      licenceExpiryDate = null,
      topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
      licenceActivatedDate = LocalDateTime.now(),
    )

    val isActivatedInPssPeriod = testLicence.isActivatedInPssPeriod()
    assertFalse(isActivatedInPssPeriod)
  }

  @Test
  fun `isActivatedInPssPeriod should returns false if TUSED is null`() {
    val testLicence = Licence(
      licenceExpiryDate = LocalDate.now().plusDays(1),
      topupSupervisionExpiryDate = null,
      licenceActivatedDate = LocalDateTime.now(),
    )

    val isActivatedInPssPeriod = testLicence.isActivatedInPssPeriod()
    assertFalse(isActivatedInPssPeriod)
  }

  @Test
  fun `isActivatedInPssPeriod should returns false if LAD is null`() {
    val testLicence = Licence(
      licenceExpiryDate = LocalDate.now().plusDays(1),
      topupSupervisionExpiryDate = LocalDate.now(),
      licenceActivatedDate = null,
    )

    val isActivatedInPssPeriod = testLicence.isActivatedInPssPeriod()
    assertFalse(isActivatedInPssPeriod)
  }

  @Test
  fun `isInPssPeriod should returns false if all LAD, LED and TUSED is null`() {
    val testLicence = Licence(
      licenceExpiryDate = null,
      topupSupervisionExpiryDate = null,
      licenceActivatedDate = null,
    )

    val isActivatedInPssPeriod = testLicence.isActivatedInPssPeriod()
    assertFalse(isActivatedInPssPeriod)
  }

  @Test
  fun `isActivatedInPssPeriod should return false if LED is before LAD`() {
    val testLicence = Licence(
      licenceExpiryDate = LocalDate.now().plusDays(1),
      topupSupervisionExpiryDate = LocalDate.now().minusDays(1),
      licenceActivatedDate = LocalDateTime.now(),
    )

    val isActivatedInPssPeriod = testLicence.isActivatedInPssPeriod()
    assertFalse(isActivatedInPssPeriod)
  }

  @Test
  fun `isActivatedInPssPeriod should return false if both LED and TUSED are before LAD`() {
    val testLicence = Licence(
      licenceExpiryDate = LocalDate.now().plusDays(1),
      topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
      licenceActivatedDate = LocalDateTime.now(),
    )

    val isActivatedInPssPeriod = testLicence.isActivatedInPssPeriod()
    assertFalse(isActivatedInPssPeriod)
  }

  @Test
  fun `isActivatedInPssPeriod should return true if LED less than LAD less than TUSED`() {
    val testLicence = Licence(
      licenceExpiryDate = LocalDate.now().minusDays(1),
      topupSupervisionExpiryDate = LocalDate.now().plusDays(1),
      licenceActivatedDate = LocalDateTime.now(),
    )

    val isActivatedInPssPeriod = testLicence.isActivatedInPssPeriod()
    assertTrue(isActivatedInPssPeriod)
  }

  @Test
  fun `isActivatedInPssPeriod should return true if LED less than LAD equals TUSED`() {
    val testLicence = Licence(
      licenceExpiryDate = LocalDate.now().minusDays(1),
      topupSupervisionExpiryDate = LocalDate.now(),
      licenceActivatedDate = LocalDateTime.now(),
    )

    val isActivatedInPssPeriod = testLicence.isActivatedInPssPeriod()
    assertTrue(isActivatedInPssPeriod)
  }

  @Test
  fun `earliestReleaseDate should return Tuesday(2018-01-02) if ARD or CRD is Friday(2018-01-05) and day they would normally be released is Friday`() {
    val testLicence = Licence(
      actualReleaseDate = LocalDate.parse("2018-01-05"),
      conditionalReleaseDate = LocalDate.now().minusDays(1),
    )

    val earliestPossibleReleaseDate = testLicence.earliestReleaseDate(bankHolidays, workingDays)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-01-02"))
  }

  @Test
  fun `earliestReleaseDate should return Tuesday(2018-01-02) if ARD or CRD is on Saturday(2018-01-06) and day they would normally be released is Friday`() {
    val testLicence = Licence(
      actualReleaseDate = LocalDate.parse("2018-01-06"),
      conditionalReleaseDate = LocalDate.now().minusDays(1),
    )

    val earliestPossibleReleaseDate = testLicence.earliestReleaseDate(bankHolidays, workingDays)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-01-02"))
  }

  @Test
  fun `earliestReleaseDate should return Tuesday(2018-01-02) if ARD or CRD is on Sunday(2018-01-07) and day they would normally be released is Friday`() {
    val testLicence = Licence(
      actualReleaseDate = LocalDate.parse("2018-01-07"),
      conditionalReleaseDate = LocalDate.now().minusDays(1),
    )

    val earliestPossibleReleaseDate = testLicence.earliestReleaseDate(bankHolidays, workingDays)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-01-02"))
  }

  @Test
  fun `earliestReleaseDate should return Tuesday(2018-03-27) if ARD or CRD is on Bank holiday Friday(2018-03-30) and day they would normally be released is Thursday`() {
    val testLicence = Licence(
      actualReleaseDate = LocalDate.parse("2018-03-30"),
      conditionalReleaseDate = LocalDate.now().minusDays(1),
    )

    val earliestPossibleReleaseDate = testLicence.earliestReleaseDate(bankHolidays, workingDays)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-03-27"))
  }

  @Test
  fun `earliestReleaseDate should return Tuesday(2018-04-27) if ARD or CRD is on Bank holiday Monday(2018-04-02) and day they would normally be released is Friday`() {
    val testLicence = Licence(
      actualReleaseDate = LocalDate.parse("2018-04-02"),
      conditionalReleaseDate = LocalDate.now().minusDays(1),
    )

    val earliestPossibleReleaseDate = testLicence.earliestReleaseDate(bankHolidays, workingDays)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-03-27"))
  }

  @Test
  fun `earliestReleaseDate should return Tuesday(2018-05-29) if ARD or CRD is on Bank holiday Monday(2018-06-04) Friday(2018-06-01) before also bank holiday and day they would normally be released is Thursday`() {
    val testLicence = Licence(
      actualReleaseDate = LocalDate.parse("2018-06-04"),
      conditionalReleaseDate = LocalDate.now().minusDays(1),
    )

    val earliestPossibleReleaseDate = testLicence.earliestReleaseDate(bankHolidays, workingDays)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-05-29"))
  }

  @Test
  fun `earliestReleaseDate should return Tuesday(2018-11-27) if ARD or CRD is on Monday(2018-12-03) with Consecutive Bank holiday Monday(2018-12-03) and Tuesday(2018-12-04) before also bank holiday and day they would normally be released is Friday`() {
    val testLicence = Licence(
      actualReleaseDate = LocalDate.parse("2018-12-03"),
      conditionalReleaseDate = LocalDate.now().minusDays(1),
    )

    val earliestPossibleReleaseDate = testLicence.earliestReleaseDate(bankHolidays, workingDays)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-11-27"))
  }

  @Test
  fun `earliestReleaseDate should return Tuesday(2018-11-27) if ARD or CRD is on Tuesday(2018-12-04) with Consecutive Bank holiday Monday(2018-12-03) and Tuesday(2018-12-04) before also bank holiday and day they would normally be released is Friday`() {
    val testLicence = Licence(
      actualReleaseDate = LocalDate.parse("2018-12-04"),
      conditionalReleaseDate = LocalDate.now().minusDays(1),
    )

    val earliestPossibleReleaseDate = testLicence.earliestReleaseDate(bankHolidays, workingDays)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-11-27"))
  }

  @Test
  fun `earliestReleaseDate should return Wednesday(2018-08-01) if ARD or CRD is on Bank holiday Tuesday(2018-08-07) and day they would normally be released is Monday`() {
    val testLicence = Licence(
      actualReleaseDate = LocalDate.parse("2018-08-07"),
      conditionalReleaseDate = LocalDate.now().minusDays(1),
    )

    val earliestPossibleReleaseDate = testLicence.earliestReleaseDate(bankHolidays, workingDays)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-08-01"))
  }

  @Test
  fun `earliestReleaseDate should return Thursday(2018-09-27) if ARD or CRD is on Bank holiday Wednesday(2018-10-03) and day they would normally be released is Tuesday`() {
    val testLicence = Licence(
      actualReleaseDate = LocalDate.parse("2018-10-03"),
      conditionalReleaseDate = LocalDate.now().minusDays(1),
    )

    val earliestPossibleReleaseDate = testLicence.earliestReleaseDate(bankHolidays, workingDays)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-09-27"))
  }

  @Test
  fun `earliestReleaseDate should return (2018-03-27) if ARD is (2018-03-30) third working day before CRD`() {
    val testLicence = Licence(
      actualReleaseDate = LocalDate.parse("2018-03-30"),
      conditionalReleaseDate = LocalDate.now().minusDays(1),
    )

    val earliestPossibleReleaseDate = testLicence.earliestReleaseDate(bankHolidays, workingDays)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-03-27"))
  }

  @Test
  fun `earliestReleaseDate should return (2018-03-27) if ARD is (2018-04-02) third working day before CRD`() {
    val testLicence = Licence(
      actualReleaseDate = LocalDate.parse("2018-04-02"),
      conditionalReleaseDate = LocalDate.now().minusDays(1),
    )

    val earliestPossibleReleaseDate = testLicence.earliestReleaseDate(bankHolidays, workingDays)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-03-27"))
  }

  @Test
  fun `earliestReleaseDate should return (2018-04-30) if ARD is (2018-05-07) and (2018-05-02) is bank holiday`() {
    val testLicence = Licence(
      actualReleaseDate = LocalDate.parse("2018-05-07"),
      conditionalReleaseDate = LocalDate.now().minusDays(1),
    )

    val earliestPossibleReleaseDate = testLicence.earliestReleaseDate(bankHolidays, workingDays)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-04-30"))
  }

  @Test
  fun `earliestReleaseDate should return (2018-06-07) if ARD is (2018-06-07) as it is not a bank holiday or weekend`() {
    val testLicence = Licence(
      actualReleaseDate = LocalDate.parse("2018-06-07"),
      conditionalReleaseDate = LocalDate.now().minusDays(1),
    )

    val earliestPossibleReleaseDate = testLicence.earliestReleaseDate(bankHolidays, workingDays)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-06-07"))
  }

  @Test
  fun `earliestReleaseDate should return (2018-07-03) if ARD is (2018-07-06) third working day before CRD`() {
    val testLicence = Licence(
      actualReleaseDate = LocalDate.parse("2018-07-06"),
      conditionalReleaseDate = LocalDate.now().minusDays(1),
    )

    val earliestPossibleReleaseDate = testLicence.earliestReleaseDate(bankHolidays, workingDays)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-07-03"))
  }

  @Test
  fun `earliestReleaseDate should return (2018-07-03) if ARD is (2018-07-07) as it is not a bank holiday or weekend`() {
    val testLicence = Licence(
      actualReleaseDate = LocalDate.parse("2018-07-07"),
      conditionalReleaseDate = LocalDate.now().minusDays(1),
    )

    val earliestPossibleReleaseDate = testLicence.earliestReleaseDate(bankHolidays, workingDays)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-07-03"))
  }

  @Test
  fun `earliestReleaseDate should return (2018-07-03) if ARD is (2018-07-08) as it is not a bank holiday or weekend`() {
    val testLicence = Licence(
      actualReleaseDate = LocalDate.parse("2018-07-08"),
      conditionalReleaseDate = LocalDate.now().minusDays(1),
    )

    val earliestPossibleReleaseDate = testLicence.earliestReleaseDate(bankHolidays, workingDays)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-07-03"))
  }

  @Test
  fun `earliestReleaseDate should return (2018-07-03) if CRD is (2018-07-08) as it is not a bank holiday or weekend`() {
    val testLicence = Licence(
      actualReleaseDate = null,
      conditionalReleaseDate = LocalDate.parse("2018-07-08"),
    )

    val earliestPossibleReleaseDate = testLicence.earliestReleaseDate(bankHolidays, workingDays)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-07-03"))
  }

  @Test
  fun `earliestReleaseDate should return null if ARD or CRD is null`() {
    val testLicence = Licence(
      actualReleaseDate = null,
      conditionalReleaseDate = null,
    )

    val earliestPossibleReleaseDate = testLicence.earliestReleaseDate(bankHolidays, workingDays)
    assertThat(earliestPossibleReleaseDate).isNull()
  }

  private companion object {
    val bankHolidays = listOf(
      LocalDate.parse("2018-01-01"),
      LocalDate.parse("2018-03-26"),
      LocalDate.parse("2018-03-30"),
      LocalDate.parse("2018-04-02"),
      LocalDate.parse("2018-05-02"),
      LocalDate.parse("2018-05-07"),
      LocalDate.parse("2018-06-01"),
      LocalDate.parse("2018-06-04"),
      LocalDate.parse("2018-08-07"),
      LocalDate.parse("2018-10-03"),
      LocalDate.parse("2018-12-03"),
      LocalDate.parse("2018-12-04"),
    )
    const val workingDays = 3
  }
}
