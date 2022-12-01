package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

class ConditionSpecificLogicTest {

  @Test
  fun `returns no additional data for an arbitrary condition`() {
    val initialData = anAdditionalCondition.copy(conditionCode = "some-code").getInitialData()
    assertThat(initialData).isEmpty()
  }

  @Test
  fun `returns two pieces of additional data for condition 14b`() {
    val initialData = anAdditionalCondition.copy(conditionCode = CONDITION_CODE_FOR_14B).getInitialData()
    assertThat(initialData).hasSize(2)
    assertThat(initialData.find { it.dataField == "endDate" }!!.dataValue).isEqualTo(licence.getElectronicMonitoringEndDate())
  }

  @Nested
  inner class `Calculating electronic monitoring end date` {
    @Test
    fun `a licence with a licence expiry date less than 12 months in the future uses licence expiry date `() {
      val licenceExpiryDate = LocalDate.now().plusMonths(12).minusDays(1)
      assertThat(
        licence.copy(licenceExpiryDate = licenceExpiryDate).getElectronicMonitoringEndDate()
      ).isEqualTo(licenceExpiryDate.format(LONG_DATE_FORMATTER))
    }

    @Test
    fun `a licence with a licence expiry date equal to 12 months in the future uses actual release date when provided`() {
      val licenceExpiryDate = LocalDate.now().plusMonths(12)
      val actualReleaseDate = LocalDate.now().plusMonths(4)

      assertThat(
        licence.copy(licenceExpiryDate = licenceExpiryDate, actualReleaseDate = actualReleaseDate)
          .getElectronicMonitoringEndDate()
      ).isEqualTo(actualReleaseDate.plusYears(1).format(LONG_DATE_FORMATTER))
    }

    @Test
    fun `a licence with a licence expiry date equal to 12 months in the future uses conditional release date when actual release date is not provided`() {
      val licenceExpiryDate = LocalDate.now().plusMonths(12)
      val actualReleaseDate = null
      val conditionalReleaseDate = LocalDate.now().plusMonths(3)

      assertThat(
        licence.copy(
          licenceExpiryDate = licenceExpiryDate,
          actualReleaseDate = actualReleaseDate,
          conditionalReleaseDate = conditionalReleaseDate
        ).getElectronicMonitoringEndDate()
      ).isEqualTo(conditionalReleaseDate.plusYears(1).format(LONG_DATE_FORMATTER))
    }
  }

  private val licence = Licence(
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
    standardConditions = emptyList(),
  )

  private val anAdditionalCondition =
    AdditionalCondition(conditionCode = "a code", licence = licence, conditionVersion = "1.0")
}
