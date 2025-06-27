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
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.Address
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.AddressSource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.Country
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentPersonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentTimeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ContactNumberRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.AddAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentPersonType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.stream.Stream
import kotlin.jvm.optionals.getOrNull

@SpringBootTest(
  webEnvironment = RANDOM_PORT,
  properties = ["spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true"],
)
class AppointmentIntegrationTest(
  @Autowired private val licenceRepository: LicenceRepository,
) : IntegrationTestBase() {

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
  fun `Update the address where the initial appointment will take place`() {
    // Given
    val uri = "/licence/id/1/appointment/address"
    val addAddressRequest = buildAddAddressRequest()

    // When
    val result = putRequest(uri, addAddressRequest)

    // Then
    result.expectStatus().isOk

    val savedAddress = getAddress()
    assertThat(savedAddress.id).isEqualTo(1)
    assertThat(addAddressRequest)
      .usingRecursiveComparison()
      .ignoringFields("id", "lastUpdatedTimestamp", "createdTimestamp")
      .isEqualTo(savedAddress)

    assertThat(savedAddress.createdTimestamp).isCloseTo(LocalDateTime.now(), within(20, ChronoUnit.SECONDS))
    assertThat(savedAddress.lastUpdatedTimestamp).isCloseTo(LocalDateTime.now(), within(20, ChronoUnit.SECONDS))
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1-with-address.sql",
  )
  fun `Update the address where the licence already has an appointment address but a new one is given with out reference`() {
    // Given
    val uri = "/licence/id/1/appointment/address"
    val addAddressRequest = buildAddAddressRequest(reference = null)

    // When
    val result = putRequest(uri, addAddressRequest)

    // Then
    result.expectStatus().isOk

    val savedAddress = getAddress()
    assertThat(addAddressRequest)
      .usingRecursiveComparison()
      .ignoringFields("id", "lastUpdatedTimestamp", "createdTimestamp", "reference")
      .isEqualTo(savedAddress)

    assertThat(savedAddress.reference).isNotEqualTo("REF-123456")
    assertThatCode { UUID.fromString(savedAddress.reference) }.doesNotThrowAnyException()
    assertThat(savedAddress.id).isEqualTo(2)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1-with-address.sql",
  )
  fun `Update the address where the licence already has an appointment address but a new one is given with reference`() {
    // Given
    val uri = "/licence/id/1/appointment/address"
    val addAddressRequest = buildAddAddressRequest(reference = "NEW_TEST_REFERENCE")

    // When
    val result = putRequest(uri, addAddressRequest)

    // Then
    result.expectStatus().isOk

    val savedAddress = getAddress()
    assertThat(savedAddress.reference).isEqualTo("NEW_TEST_REFERENCE")
    assertThat(savedAddress.id).isEqualTo(2)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1-with-address.sql",
  )
  fun `Update the address where the licence already has an appointment address but the reference is the same as the existing`() {
    // Given
    val uri = "/licence/id/1/appointment/address"
    val addAddressRequest = buildAddAddressRequest()

    // When
    val result = putRequest(uri, addAddressRequest)

    // Then
    result.expectStatus().isOk

    val savedAddress = getAddress()
    assertThat(savedAddress.reference).isEqualTo("REF-123456")
    assertThat(savedAddress.id).isEqualTo(1)
  }

  @Test
  fun `When licence is not found then not found is given`() {
    // Given
    val uri = "/licence/id/1/appointment/address"
    val addAddressRequest = buildAddAddressRequest()

    // When
    val result = putRequest(uri, addAddressRequest)

    // Then
    result.expectStatus().isNotFound
  }

  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  @Test
  fun `When updating address should accept AddAddressRequest with optional fields as null`() {
    // Given
    val uri = "/licence/id/1/appointment/address"

    val addAddressRequest = buildAddAddressRequest(
      reference = null,
      secondLine = null,
      county = null,
      country = null,
    )

    // When
    val result = putRequest(uri, addAddressRequest)

    // Then
    result.expectStatus().isOk
    val savedAddress = getAddress()
    // reference self populates if null, test above tests for this
    assertThat(savedAddress.secondLine).isNull()
    assertThat(savedAddress.county).isNull()
    assertThat(savedAddress.country).isNull()
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
    result.expectStatus().isBadRequest
    val errorResponse = result.expectBody(ErrorResponse::class.java).returnResult().responseBody
    assertThat(errorResponse).isNotNull
    assertThat(errorResponse!!.userMessage).contains("Validation failed for one or more fields.")
    assertThat(errorResponse.developerMessage).contains("must not be blank")
  }

  @ParameterizedTest
  @ValueSource(strings = ["INVALID", "123 ABC", "C@10 1AA", "CF101AA", "CF10-1AA", ""])
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
    val request = buildAddAddressRequest(source = AddressSource.OS_PLACES, reference = invalidReference)

    // When
    val result = putRequest(uri, request)

    // Then
    result.expectStatus().isBadRequest
    val errorResponse = result.expectBody(ErrorResponse::class.java).returnResult().responseBody
    assertThat(errorResponse).isNotNull
    assertThat(errorResponse!!.userMessage).contains("Validation failed")

    if (invalidReference?.length == 0) {
      assertThat(errorResponse.developerMessage).contains("Reference must be provided when source is OS_PLACES; size must be between 1 and 36")
    } else {
      assertThat(errorResponse.developerMessage).contains("Reference must be provided when source is OS_PLACES")
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
    result.expectStatus().isBadRequest
    val errorResponse = result.expectBody(ErrorResponse::class.java).returnResult().responseBody
    assertThat(errorResponse).isNotNull
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

  private fun getAddress(id: Long = 1): Address {
    val licence = licenceRepository.findById(id).getOrNull()
    assertThat(licence).isNotNull
    assertThat(licence!!.licenceAppointmentAddress).isNotNull
    return licence.licenceAppointmentAddress!!
  }

  fun buildAddAddressRequest(
    reference: String? = "REF-123456",
    firstLine: String = "221B",
    secondLine: String? = "Baker Street",
    townOrCity: String = "London",
    county: String? = "Greater London",
    postcode: String = "NW1 6XE",
    country: Country? = Country.ENGLAND,
    source: AddressSource = AddressSource.MANUAL,
  ): AddAddressRequest = AddAddressRequest(
    reference = reference,
    firstLine = firstLine,
    secondLine = secondLine,
    townOrCity = townOrCity,
    county = county,
    postcode = postcode,
    country = country,
    source = source,
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
