package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address

import jakarta.persistence.Entity
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AbstractAddress
import java.time.LocalDateTime

@Entity
@Table(name = "address")
class Address(
  id: Long? = null,
  reference: String,
  uprn: String? = null,
  firstLine: String,
  secondLine: String? = null,
  townOrCity: String,
  county: String? = null,
  postcode: String,
  source: AddressSource,
  val createdTimestamp: LocalDateTime = LocalDateTime.now(),
  var lastUpdatedTimestamp: LocalDateTime = LocalDateTime.now(),
): AbstractAddress(
  id = id,
  reference = reference,
  uprn = uprn,
  firstLine = firstLine,
  secondLine = secondLine,
  townOrCity = townOrCity,
  county = county,
  postcode = postcode,
  source = source,
) {

  override fun toString(): String = listOf(
    reference,
    uprn.orEmpty(),
    firstLine,
    secondLine.orEmpty(),
    townOrCity,
    county.orEmpty(),
    postcode,
    source.name,
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
