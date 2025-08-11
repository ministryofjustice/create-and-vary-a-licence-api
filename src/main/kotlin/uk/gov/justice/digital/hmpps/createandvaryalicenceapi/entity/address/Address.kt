package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AbstractIdEntity
import java.time.LocalDateTime

enum class AddressSource {
  MANUAL,
  OS_PLACES,
}

@Entity
@Table(name = "address")
class Address(
  id: Long? = null,

  @Column(nullable = false, unique = true)
  val reference: String,

  // Unique Property Reference Number
  @Column(nullable = true, unique = false)
  var uprn: String? = null,

  @Column(name = "first_line", nullable = false)
  var firstLine: String,

  @Column(name = "second_line")
  var secondLine: String? = null,

  @Column(name = "town_or_city", nullable = false)
  var townOrCity: String,

  @Column
  var county: String? = null,

  @Column(nullable = false)
  var postcode: String,

  @Enumerated(EnumType.STRING)
  @Column(name = "source", nullable = false)
  var source: AddressSource,

  val createdTimestamp: LocalDateTime = LocalDateTime.now(),

  var lastUpdatedTimestamp: LocalDateTime = LocalDateTime.now(),
) : AbstractIdEntity(idInternal = id) {

  override fun toString(): String = listOf(
    reference,
    uprn.orEmpty(),
    firstLine,
    secondLine.orEmpty(),
    townOrCity,
    county.orEmpty(),
    postcode,
    source,
  ).joinToString(",")

  /**
   * Checks equality of address by comparing key address fields with another,
   *
   * This method focuses purely on the meaningful data that defines the address,
   * rather than full object equality or database identity.
   */
  fun isSame(other: Address): Boolean = firstLine == other.firstLine &&
    secondLine.orEmpty() == other.secondLine.orEmpty() &&
    townOrCity == other.townOrCity &&
    county.orEmpty() == other.county.orEmpty() &&
    postcode == other.postcode
}
