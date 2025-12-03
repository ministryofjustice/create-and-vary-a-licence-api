package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Staff
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.Address
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.timeserved.TimeServedExternalRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.timeserved.TimeServedProbationConfirmContact
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StandardConditionRepository
import java.util.Optional

@Repository
interface TestLicenceRepository : JpaRepository<Licence, Long> {
  @EntityGraph(attributePaths = ["standardConditions", "appointment", "appointment.address", "responsibleCom", "responsibleCom.savedAppointmentAddresses"])
  override fun findById(id: Long): Optional<Licence>

  @EntityGraph(attributePaths = ["standardConditions", "appointment", "appointment.address", "responsibleCom", "responsibleCom.savedAppointmentAddresses"])
  override fun findAll(): List<Licence>
}

@Repository
interface TestAuditEventRepository : JpaRepository<AuditEvent, Long> {
  fun findAllByLicenceIdIn(licenceIds: List<Long>): List<AuditEvent>
  fun findAllByLicenceIdNull(): List<AuditEvent>
}

@Repository
interface TestStaffRepository : JpaRepository<Staff, Long> {
  @EntityGraph(attributePaths = ["savedAppointmentAddresses"])
  fun findByUsernameIgnoreCase(username: String): Staff?
}

@Repository
interface TestBespokeConditionRepository : JpaRepository<BespokeCondition, Long> {
  fun findByLicenceId(licenceId: Long): List<BespokeCondition>
}

@Repository
interface TestAdditionalConditionRepository : JpaRepository<AdditionalCondition, Long> {
  fun findByLicenceId(licenceId: Long): List<AdditionalCondition>
}

@Repository
interface TestAddressRepository : JpaRepository<Address, Long>

@Repository
interface TestTimeServedExternalRecordRepository : JpaRepository<TimeServedExternalRecord, Long> {
  fun findByNomsIdAndBookingId(nomsId: String, bookingId: Long): TimeServedExternalRecord?
}

@Repository
interface TestTimeServedProbationConfirmContactRepository : JpaRepository<TimeServedProbationConfirmContact, Long> {
  fun findByLicenceId(licenceId: Long): TimeServedProbationConfirmContact?
}

@Component
@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
class TestRepository(
  private val jdbcTemplate: JdbcTemplate,
  private val staffRepository: TestStaffRepository,
  private val licenceRepository: TestLicenceRepository,
  private val auditEventRepository: TestAuditEventRepository,
  private val addressRepository: TestAddressRepository,
  private val bespokeConditionRepository: TestBespokeConditionRepository,
  private val additionalConditionRepository: TestAdditionalConditionRepository,
  private val standardConditionRepository: StandardConditionRepository,
  private val testTimeServedExternalRecordRepository: TestTimeServedExternalRecordRepository,
  private val testTimeServedProbationConfirmContactRepository: TestTimeServedProbationConfirmContactRepository,
) {

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  fun clearAll() {
    jdbcTemplate.execute("DISCARD ALL")
  }

  fun findTimeServedProbationConfirmContact(id: Long = 1L): TimeServedProbationConfirmContact? {
    val probationConfirmContact = testTimeServedProbationConfirmContactRepository.findByLicenceId(id)
    assertThat(probationConfirmContact).isNotNull
    return probationConfirmContact
  }

  fun findLicence(id: Long = 1L): Licence {
    val licence = licenceRepository.findById(id).orElse(null)
    assertThat(licence).isNotNull

    licence.bespokeConditions = bespokeConditionRepository.findByLicenceId(licenceId = licence.id).toMutableList()
    licence.additionalConditions = additionalConditionRepository.findByLicenceId(licenceId = licence.id).toMutableList()
    return licence
  }

  fun findAllLicence(assertNotEmpty: Boolean = true): List<Licence> {
    val licences = licenceRepository.findAll()
      .sortedBy { it.id }
    if (assertNotEmpty) assertThat(licences).isNotEmpty
    return licences
  }

  fun countLicence(): Long = licenceRepository.count()

  fun findAllAuditEventsByLicenceIdIn(list: List<Long>, assertNotEmpty: Boolean = true): List<AuditEvent> {
    val events = auditEventRepository.findAllByLicenceIdIn(list)
    if (assertNotEmpty) assertThat(events).isNotEmpty
    return events
  }

  fun findAllAuditEventsByLicenceIdNull(): List<AuditEvent> {
    val events = auditEventRepository.findAllByLicenceIdNull()
    assertThat(events).isNotEmpty
    return events
  }

  fun getAuditEventCount(): Long = auditEventRepository.count()

  fun findAllAuditEvents(): List<AuditEvent> = auditEventRepository.findAll()

  fun findFirstAuditEvent(licenceId: Long = 1L): AuditEvent {
    val event = auditEventRepository.findAllByLicenceIdIn(listOf(licenceId)).firstOrNull()
    assertThat(event).isNotNull
    return event!!
  }

  fun findStaff(userName: String = "test-client"): Staff {
    val staff = staffRepository.findByUsernameIgnoreCase(userName)
    assertThat(staff).isNotNull
    return staff!!
  }

  fun findAllAddresses(assertNotEmpty: Boolean = true): List<Address> {
    val addresses = addressRepository.findAll()
    if (assertNotEmpty) assertThat(addresses).isNotEmpty
    return addresses
  }

  fun getStandardConditionCount(): Long = standardConditionRepository.count()

  fun getAdditionalConditions(licenceId: Long, assertNotEmpty: Boolean = true): List<AdditionalCondition> {
    val list = additionalConditionRepository.findByLicenceId(licenceId)
    if (assertNotEmpty) assertThat(list).isNotEmpty
    return list
  }

  fun getBespokeConditions(licenceId: Long, assertNotEmpty: Boolean = true): List<BespokeCondition> {
    val list = bespokeConditionRepository.findByLicenceId(licenceId)
    if (assertNotEmpty) assertThat(list).isNotEmpty
    return list
  }

  fun findTimeServedExternalRecordByNomsIdAndBookingId(
    nomsId: String,
    bookingId: Long,
    assertNotNull: Boolean = true,
  ): TimeServedExternalRecord? {
    val reason = testTimeServedExternalRecordRepository.findByNomsIdAndBookingId(nomsId, bookingId)
    if (assertNotNull) assertThat(reason).isNotNull
    reason?.updatedByCa?.fullName
    return reason
  }
}
