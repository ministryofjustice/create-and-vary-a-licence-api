package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.hdc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

class HdcAddressWithdrawnIntegrationTest : HdcEventsIntegrationTestBase() {

  @ParameterizedTest
  @CsvSource("1, A1234AA, 2, SUBMITTED", "2, B1234BB, 1, APPROVED")
  @Sql(
    "classpath:test_data/seed-hdc-approved-and-submitted-licences.sql",
    "classpath:test_data/seed-hdc-address-withdrawal.sql",
    "classpath:test_data/seed-hdc-withdrawal-separate-cases.sql",
  )
  fun `Given an approved or submitted licence without an edit when its address is withdrawn then it moves to in progress`(
    licenceId: Long,
    nomsNumber: String,
    otherLicenceId: Long,
    otherStatus: LicenceStatus,
  ) {
    // Given
    val eventJson = mapper.writeValueAsString(createHdcStatusChangedEvent(licenceId, 12345L, nomsNumber))

    // When
    sendMessage(eventJson, HdcCvlEventType.ADDRESS_WITHDRAWN.name)

    // Then
    assertSqsProcessed()
    val revertedLicence = testRepository.findLicence(licenceId)
    assertThat(revertedLicence.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(revertedLicence.approvedByUsername).isNull()
    assertThat(revertedLicence.approvedByName).isNull()
    assertThat(revertedLicence.approvedDate).isNull()
    assertThat(testRepository.findLicence(otherLicenceId).statusCode).isEqualTo(otherStatus)
    val remainingAddresses = testRepository.findAllHdcCurfewAddresses()
    assertThat(remainingAddresses).hasSize(1)
    assertThat(remainingAddresses.single().licence.id).isEqualTo(otherLicenceId)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-hdc-approved-and-submitted-licences.sql",
    "classpath:test_data/seed-hdc-address-withdrawal.sql",
    "classpath:test_data/seed-hdc-withdrawal-edit.sql",
  )
  fun `Given a submitted edit when the address is withdrawn then the approved licence is inactivated and the edit is in progress`() {
    // Given
    val eventJson = mapper.writeValueAsString(createHdcStatusChangedEvent(2L, 12345L, "A1234AA"))

    // When
    sendMessage(eventJson, HdcCvlEventType.ADDRESS_WITHDRAWN.name)

    // Then
    assertSqsProcessed()
    assertThat(testRepository.findLicence(1L).statusCode).isEqualTo(LicenceStatus.INACTIVE)
    assertThat(testRepository.findLicence(2L).statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(testRepository.findAllHdcCurfewAddresses()).isEmpty()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-hdc-approved-and-submitted-licences.sql",
    "classpath:test_data/seed-hdc-address-withdrawal.sql",
    "classpath:test_data/seed-hdc-withdrawal-edit.sql",
    "classpath:test_data/seed-hdc-withdrawal-in-progress-edit.sql",
  )
  fun `Given an in progress edit when the address is withdrawn then the approved licence is inactivated and the edit remains in progress`() {
    // Given
    val eventJson = mapper.writeValueAsString(createHdcStatusChangedEvent(2L, 12345L, "A1234AA"))

    // When
    sendMessage(eventJson, HdcCvlEventType.ADDRESS_WITHDRAWN.name)

    // Then
    assertSqsProcessed()
    assertThat(testRepository.findLicence(1L).statusCode).isEqualTo(LicenceStatus.INACTIVE)
    assertThat(testRepository.findLicence(2L).statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(testRepository.findAllHdcCurfewAddresses()).isEmpty()
  }
}
