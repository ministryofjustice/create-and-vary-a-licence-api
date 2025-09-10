package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.Address
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.AddressSource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentPersonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentTimeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ContactNumberRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.AddAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AddressRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentPersonType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.stream.Stream
import kotlin.jvm.optionals.getOrNull
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as LicenceEntity

@SpringBootTest(
  webEnvironment = RANDOM_PORT,
  properties = ["spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true"],
)
class AppointmentIntegrationTest(
  @param:Autowired private val licenceRepository: LicenceRepository,
) : IntegrationTestBase() {

  @Autowired
  private lateinit var addressRepository: AddressRepository

  @Autowired
  lateinit var auditEventRepository: AuditEventRepository

  @Autowired
  lateinit var staffRepository: StaffRepository

  @BeforeEach
  fun reset() {
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update person to meet at initial appointment`() {
    webTestClient.put()
      .uri("/licence/id/1/appointmentPerson")
      .bodyValue(anUpdateAppointmentPersonRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.appointmentPersonType).isEqualTo(anUpdateAppointmentPersonRequest.appointmentPersonType)
    assertThat(result?.appointmentPerson).isEqualTo(anUpdateAppointmentPersonRequest.appointmentPerson)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `update initial appointment time should throw validation error when Appointment time is null and Appointment type is SPECIFIC_DATE_TIME`() {
    webTestClient.put()
      .uri("/licence/id/1/appointmentTime")
      .bodyValue(anAppointmentTimeRequest.copy(appointmentTime = null))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isBadRequest
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `update initial appointment time should not throw validation error when Appointment time is null but Appointment type is not SPECIFIC_DATE_TIME`() {
    webTestClient.put()
      .uri("/licence/id/1/appointmentTime")
      .bodyValue(
        anAppointmentTimeRequest.copy(
          appointmentTime = null,
          appointmentTimeType = AppointmentTimeType.IMMEDIATE_UPON_RELEASE,
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.appointmentTime).isNull()
    assertThat(result?.appointmentTimeType)
      .isEqualTo(AppointmentTimeType.IMMEDIATE_UPON_RELEASE)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update time of the initial appointment`() {
    webTestClient.put()
      .uri("/licence/id/1/appointmentTime")
      .bodyValue(anAppointmentTimeRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.appointmentTime)
      .isEqualTo(anAppointmentTimeRequest.appointmentTime?.truncatedTo(ChronoUnit.MINUTES))
    assertThat(result?.appointmentTimeType)
      .isEqualTo(anAppointmentTimeRequest.appointmentTimeType)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update the contact number for the officer on a licence`() {
    webTestClient.put()
      .uri("/licence/id/1/contact-number")
      .bodyValue(aContactNumberRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.appointmentContact).isEqualTo(aContactNumberRequest.telephone)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update the largest contact number for the officer on a licence`() {
    webTestClient.put()
      .uri("/licence/id/1/contact-number")
      .bodyValue(aContactNumberRequest.copy(telephone = "+44 20 7946 0958 #98765"))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.appointmentContact).isEqualTo("+44 20 7946 0958 #98765")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `When adding appointment not preferred address manually entered Then everything saves as expected`() {
    // Given
    val uri = "/licence/id/1/appointment/address"
    val addAddressRequest = buildAddAddressRequest(source = AddressSource.MANUAL)

    // When
    val result = putRequest(uri, addAddressRequest)

    // Then
    result.expectStatus().isOk

    val licence = getLicence()
    val savedAddress = getAndAssertAddress(licence)
    assertThat(savedAddress.id).isEqualTo(1)
    assertThat(addAddressRequest).usingRecursiveComparison().ignoringFields("isPreferredAddress")
      .isEqualTo(savedAddress)

    assertThat(savedAddress.reference).isNotNull()
    assertThatCode { UUID.fromString(savedAddress.reference) }.doesNotThrowAnyException()
    assertThat(savedAddress.createdTimestamp).isCloseTo(LocalDateTime.now(), within(20, ChronoUnit.SECONDS))
    assertThat(savedAddress.lastUpdatedTimestamp).isCloseTo(LocalDateTime.now(), within(20, ChronoUnit.SECONDS))

    val auditEvent = auditEventRepository.findAllByLicenceIdIn(listOf(1)).last()
    assertThat(auditEvent.summary).isEqualTo("Updated initial appointment details for Person One")
    assertThat(auditEvent.changes!!["previousValue"]).isNull()
    assertThat(auditEvent.changes!!["value"] as String).isEqualTo("221B,Baker Street,London,Greater London,NW1 6XE")
    assertThat(auditEvent.changes).doesNotContainKey("savedToStaffMember")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `When adding a preferred appointment address Then everything saves as expected`() {
    // Given
    val uri = "/licence/id/1/appointment/address"
    val addAddressRequest = buildAddAddressRequest(isPreferredAddress = true)

    // When
    val result = putRequest(uri, addAddressRequest)

    // Then
    result.expectStatus().isOk
    val licence = getLicence()
    val savedAddress = getAndAssertAddress(licence)
    assertThat(savedAddress.id).isEqualTo(2)
    assertThat(addressRepository.findAll().size).isEqualTo(2)
    val staffAddress = licence.responsibleCom.savedAppointmentAddresses
    assertThat(staffAddress.size).isEqualTo(1)
    assertThat(staffAddress).doesNotContain(savedAddress)
    val auditEvent = auditEventRepository.findAllByLicenceIdIn(listOf(1)).last()
    assertThat(auditEvent.changes).containsEntry("savedToStaffMember", "test-client")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `When adding an appointment address that is not preferred Then everything saves as expected`() {
    // Given
    val uri = "/licence/id/1/appointment/address"
    val addAddressRequest = buildAddAddressRequest(isPreferredAddress = false)

    // When
    val result = putRequest(uri, addAddressRequest)

    // Then
    result.expectStatus().isOk
    val licence = getLicence()
    val savedAddress = getAndAssertAddress(licence)
    val staff = staffRepository.findByUsernameIgnoreCase("test-client")
    assertThat(staff?.savedAppointmentAddresses).doesNotContain(savedAddress)
    val auditEvent = auditEventRepository.findAllByLicenceIdIn(listOf(1)).last()
    assertThat(auditEvent.changes).doesNotContainEntry("savedToStaffMember", "test-client")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1-with-address.sql",
  )
  fun `When updating a preferred appointment address Then everything saves as expected`() {
    // Given
    val uri = "/licence/id/1/appointment/address"
    val addAddressRequest = buildAddAddressRequest(isPreferredAddress = true)

    // When
    val result = putRequest(uri, addAddressRequest)

    // Then
    result.expectStatus().isOk
    val licence = getLicence()
    val savedAddress = getAndAssertAddress(licence)
    assertThat(savedAddress.id).isEqualTo(1)
    assertThat(addressRepository.findAll().size).isEqualTo(3)
    val staff = staffRepository.findByUsernameIgnoreCase("test-client")
    assertThat(staff?.savedAppointmentAddresses?.last()?.id).isEqualTo(3)
    val auditEvent = auditEventRepository.findAllByLicenceIdIn(listOf(licence.id)).last()
    assertThat(auditEvent.changes).containsEntry("savedToStaffMember", "test-client")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1-with-address.sql",
    "classpath:test_data/seed-licence-id-2.sql",
  )
  fun `When updating a preferred appointment from a address then no duplicate saved address`() {
    // Given
    val uri = "/licence/id/2/appointment/address"
    val addAddressRequest = buildAddAddressRequest(
      isPreferredAddress = true,
      firstLine = "123 Test Street",
      secondLine = "Apt 4B",
      townOrCity = "Testville",
      county = "Testshire",
      postcode = "TE5 7AA",
    )

    // When
    val result = putRequest(uri, addAddressRequest)

    // Then
    result.expectStatus().isOk
    val licence = getLicence(id = 2)
    val savedAddress = getAndAssertAddress(licence)
    assertThat(savedAddress.id).isEqualTo(4)
    assertThat(addressRepository.findAll().size).isEqualTo(4)
    assertThat(licence.responsibleCom.savedAppointmentAddresses.size).isEqualTo(2)
    assertThat(licence.responsibleCom.savedAppointmentAddresses.last().id).isEqualTo(3)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1-with-address.sql",
  )
  fun `When updating an appointment address that is not preferred Then everything saves as expected`() {
    // Given
    val uri = "/licence/id/1/appointment/address"
    val addAddressRequest = buildAddAddressRequest(isPreferredAddress = false)

    // When
    val result = putRequest(uri, addAddressRequest)

    // Then
    result.expectStatus().isOk
    val licence = getLicence()
    val savedAddress = getAndAssertAddress(licence)
    assertThat(addressRepository.findAll().size).isEqualTo(2)
    assertThat(savedAddress.id).isEqualTo(1)
    val savedAppointmentAddresses = licence.responsibleCom.savedAppointmentAddresses
    assertThat(savedAppointmentAddresses.size).isEqualTo(1)
    assertThat(savedAppointmentAddresses.last().id).isEqualTo(2)
    val auditEvent = auditEventRepository.findAllByLicenceIdIn(listOf(1)).last()
    assertThat(auditEvent.changes).doesNotContainEntry("savedToStaffMember", "test-client")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1-with-address.sql",
  )
  fun `When updating appointment address manually entered Then everything saves as expected`() {
    // Given
    val uri = "/licence/id/1/appointment/address"
    val addAddressRequest = buildAddAddressRequest(source = AddressSource.MANUAL, isPreferredAddress = false)

    // When
    val result = putRequest(uri, addAddressRequest)

    // Then
    result.expectStatus().isOk

    val licence = getLicence()
    val savedAddress = getAndAssertAddress(licence)
    assertThat(savedAddress.id).isEqualTo(1)
    assertThat(addAddressRequest).usingRecursiveComparison().ignoringFields("isPreferredAddress")
      .isEqualTo(savedAddress)
    assertThat(savedAddress.reference).isEqualTo("550e8400-e29b-41d4-a716-446655440000")
    assertThatCode { UUID.fromString(savedAddress.reference) }.doesNotThrowAnyException()
    assertThat(savedAddress.createdTimestamp).isCloseTo(LocalDateTime.now(), within(20, ChronoUnit.SECONDS))
    assertThat(savedAddress.lastUpdatedTimestamp).isCloseTo(LocalDateTime.now(), within(20, ChronoUnit.SECONDS))

    val auditEvent = auditEventRepository.findAllByLicenceIdIn(listOf(licence.id)).last()
    assertThat(auditEvent.summary).isEqualTo("Updated initial appointment details for Person One")
    assertThat(auditEvent.changes!!["previousValue"] as String).isEqualTo("123 Test Street,Apt 4B,Testville,Testshire,TE5 7AA")
    assertThat(auditEvent.changes!!["value"] as String).isEqualTo("221B,Baker Street,London,Greater London,NW1 6XE")
    assertThat(auditEvent.changes).doesNotContainKey("savedToStaffMember")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1-with-address.sql",
  )
  fun `When updating appointment address using look up Then everything saves as expected`() {
    // Given
    val uri = "/licence/id/1/appointment/address"
    val addAddressRequest =
      buildAddAddressRequest(uprn = "NEW_UPRN", source = AddressSource.OS_PLACES, isPreferredAddress = true)

    // When
    val result = putRequest(uri, addAddressRequest)

    // Then
    result.expectStatus().isOk

    val savedAddress = getAndAssertAddress()
    assertThat(savedAddress.reference).isEqualTo("550e8400-e29b-41d4-a716-446655440000")
    assertThatCode { UUID.fromString(savedAddress.reference) }.doesNotThrowAnyException()
    assertThat(savedAddress.uprn).isEqualTo("NEW_UPRN")
    assertThat(savedAddress.id).isEqualTo(1)

    val staff = staffRepository.findByUsernameIgnoreCase("test-client")
    assertThat(staff?.savedAppointmentAddresses).anySatisfy {
      assertThat(it.uprn).isEqualTo("NEW_UPRN")
    }

    val auditEvent = auditEventRepository.findAllByLicenceIdIn(listOf(1)).last()
    assertThat(auditEvent.changes).containsEntry("savedToStaffMember", "test-client")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-prrd-licence-id-1-with-address.sql",
  )
  fun `When updating appointment address for PRRD then everything is saved as expected`() {
    // Given
    val uri = "/licence/id/1/appointment/address"
    val addAddressRequest = buildAddAddressRequest(uprn = "NEW_UPRN", source = AddressSource.OS_PLACES)

    // When
    val result = putRequest(uri, addAddressRequest)

    // Then
    result.expectStatus().isOk

    val savedAddress = getAndAssertAddress()
    assertThat(savedAddress.reference).isEqualTo("550e8400-e29b-41d4-a716-446655440000")
    assertThatCode { UUID.fromString(savedAddress.reference) }.doesNotThrowAnyException()
    assertThat(savedAddress.uprn).isEqualTo("NEW_UPRN")
    assertThat(savedAddress.id).isEqualTo(1)

    val staff = staffRepository.findByUsernameIgnoreCase("test-client")
    assertThat(staff?.savedAppointmentAddresses).isEmpty()

    val auditEvent = auditEventRepository.findAllByLicenceIdIn(listOf(1)).last()
    assertThat(auditEvent.changes).doesNotContainKey("savedToStaffMember")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1-with-address.sql",
  )
  fun `When updating appointment address manually and uprn is given then exception expected`() {
    // Given
    val uri = "/licence/id/1/appointment/address"
    val addAddressRequest = buildAddAddressRequest(uprn = "NEW_UPRN", source = AddressSource.MANUAL)

    // When
    val result = putRequest(uri, addAddressRequest)

    // Then
    val errorResponse = getErrorResponse(result)
    assertThat(errorResponse!!.userMessage).contains("Validation failed for one or more fields.")
    assertThat(errorResponse.developerMessage).contains("Unique Property Reference Number must be provided only with source OS_PLACES")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1-with-address.sql",
  )
  fun `When updating appointment address using look up and uprn is not given then exception expected`() {
    // Given
    val uri = "/licence/id/1/appointment/address"
    val addAddressRequest = buildAddAddressRequest(uprn = null, source = AddressSource.OS_PLACES)

    // When
    val result = putRequest(uri, addAddressRequest)

    // Then
    val errorResponse = getErrorResponse(result)
    assertThat(errorResponse!!.userMessage).contains("Validation failed for one or more fields.")
    assertThat(errorResponse.developerMessage).contains("Unique Property Reference Number must be provided only with source OS_PLACES")
  }

  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  @Test
  fun `When updating address should accept AddAddressRequest with optional fields as null`() {
    // Given
    val uri = "/licence/id/1/appointment/address"

    val addAddressRequest = buildAddAddressRequest(
      uprn = null,
      secondLine = null,
      county = null,
    )

    // When
    val result = putRequest(uri, addAddressRequest)

    // Then
    result.expectStatus().isOk
    val savedAddress = getAndAssertAddress()
    // reference self populates if null, test above tests for this
    assertThat(savedAddress.secondLine).isNull()
    assertThat(savedAddress.county).isNull()
  }

  @ParameterizedTest
  @ValueSource(strings = ["firstLine", "townOrCity", "postcode"])
  fun `should return 400 Bad Request when required string field is blank`(blankField: String) {
    // Given
    val jsonRequest = createAddressJson(blankField, true)

    val uri = "/licence/id/1/appointment/address"

    // When
    val result = putRequest(uri, jsonRequest)

    // Then
    val errorResponse = getErrorResponse(result)
    assertThat(errorResponse!!.userMessage).contains("Validation failed for one or more fields.")
    assertThat(errorResponse.developerMessage).contains("must not be blank")
  }

  @ParameterizedTest
  @ValueSource(strings = ["INVALID", "123 ABC", "C@10 1AA", "CF10-1AA", ""])
  fun `When updating address should return 400 Bad Request if postcode format is invalid`(invalidPostcode: String) {
    // Given
    val uri = "/licence/id/1/appointment/address"

    val request = buildAddAddressRequest(postcode = invalidPostcode)

    // When
    val result = putRequest(uri, request)

    // Then
    result.expectStatus().isBadRequest
      .expectBody()
      .consumeWith { response ->
        val responseString = response.responseBody?.toString(Charsets.UTF_8)
        assertThat(responseString).contains("Validation failed")
      }
  }

  @ParameterizedTest
  @MethodSource("invalidReferencesWhenSourceOSPlaces")
  fun `When source is OS_PLACES should return 400 Bad Request if reference is null or blank`(invalidReference: String?) {
    // Given
    val uri = "/licence/id/1/appointment/address"
    val request = buildAddAddressRequest(source = AddressSource.OS_PLACES, uprn = invalidReference)

    // When
    val result = putRequest(uri, request)

    // Then
    val errorResponse = getErrorResponse(result)
    assertThat(errorResponse!!.userMessage).contains("Validation failed")
    assertThat(errorResponse.developerMessage).contains("Unique Property Reference Number must be provided only with source OS_PLACES")
    invalidReference?.let {
      if (it.isEmpty()) {
        assertThat(errorResponse.developerMessage).contains("size must be between 1 and 12")
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["firstLine", "townOrCity", "postcode", "source"])
  fun `When updating address should return 400 Bad Request if required field is missing`(missingField: String) {
    // Given
    val jsonRequest = createAddressJson(missingField, false)
    val uri = "/licence/id/1/appointment/address"

    // When
    val result = putRequest(uri, jsonRequest)

    // Then
    val errorResponse = getErrorResponse(result)
    assertThat(errorResponse!!.userMessage).contains("Bad request: JSON parse error: Instantiation of")
  }

  private fun createAddressJson(excludeField: String, blank: Boolean?): MutableMap<String, Any?> {
    val baseRequest = mutableMapOf<String, Any?>(
      "reference" to "200010019924",
      "firstLine" to "123 High Street",
      "secondLine" to "Flat 1",
      "townOrCity" to "Cardiff",
      "county" to "South Glamorgan",
      "postcode" to "CF10 1AA",
      "country" to "ENGLAND",
      "source" to "MANUAL",
    )

    baseRequest[excludeField] = if (blank == true) "" else null
    return baseRequest
  }

  fun putRequest(
    uri: String,
    requestBody: Any,
    roles: List<String> = listOf("ROLE_CVL_ADMIN"),
  ) = webTestClient.put()
    .uri(uri)
    .headers(setAuthorisation(roles = roles))
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(requestBody)
    .accept(MediaType.APPLICATION_JSON)
    .exchange()

  private fun getErrorResponse(
    result: WebTestClient.ResponseSpec,
    exceptedStatus: HttpStatusCode = HttpStatusCode.valueOf(400),
  ): ErrorResponse? {
    result.expectStatus().isEqualTo(exceptedStatus)
    val errorResponse = result.expectBody(ErrorResponse::class.java).returnResult().responseBody
    assertThat(errorResponse).isNotNull
    return errorResponse
  }

  private fun getLicence(id: Long = 1): LicenceEntity {
    val licence = licenceRepository.findById(id).getOrNull()
    assertThat(licence).isNotNull
    return licence!!
  }

  private fun getAndAssertAddress(licence: LicenceEntity = getLicence()): Address {
    assertThat(licence.appointment).isNotNull
    val appointment = licence.appointment!!
    assertThat(appointment.address).isNotNull
    assertThat(appointment.addressText).isNotNull
    val address = appointment.address!!
    val expectedAppointmentAddress = with(address) {
      listOf(
        firstLine,
        secondLine.orEmpty(),
        townOrCity,
        county.orEmpty(),
        postcode,
      ).joinToString(",")
    }
    assertThat(appointment.addressText).isEqualTo(expectedAppointmentAddress)
    return address
  }

  fun buildAddAddressRequest(
    source: AddressSource = AddressSource.MANUAL,
    uprn: String? = null,
    firstLine: String = "221B",
    secondLine: String? = "Baker Street",
    townOrCity: String = "London",
    county: String? = "Greater London",
    postcode: String = "NW1 6XE",
    isPreferredAddress: Boolean = false,
  ): AddAddressRequest = AddAddressRequest(
    uprn = uprn,
    firstLine = firstLine,
    secondLine = secondLine,
    townOrCity = townOrCity,
    county = county,
    postcode = postcode,
    source = source,
    isPreferredAddress = isPreferredAddress,
  )

  private companion object {

    val anUpdateAppointmentPersonRequest = AppointmentPersonRequest(
      appointmentPersonType = AppointmentPersonType.SPECIFIC_PERSON,
      appointmentPerson = "John Smith",
    )

    val anAppointmentTimeRequest = AppointmentTimeRequest(
      appointmentTime = LocalDateTime.now().plusDays(10),
      appointmentTimeType = AppointmentTimeType.SPECIFIC_DATE_TIME,
    )

    val aContactNumberRequest = ContactNumberRequest(
      telephone = "0114 2565555",
      telephoneAlternative = "07700 900000",
    )

    val govUkApiMockServer = GovUkMockServer()

    @JvmStatic
    fun invalidReferencesWhenSourceOSPlaces(): Stream<String?> = Stream.of(null, "", " ", "   ")

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      govUkApiMockServer.stop()
    }
  }
}
