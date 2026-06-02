package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address

import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.MappedSuperclass
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AbstractIdEntity
import java.time.LocalDateTime

@MappedSuperclass
abstract class AbstractAddress(
  id: Long? = null,
  val reference: String,
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
  @Column(name = "created_timestamp", nullable = false, updatable = false)
  val createdTimestamp: LocalDateTime,
  @Column(name = "last_updated_timestamp", nullable = false)
  var lastUpdatedTimestamp: LocalDateTime,
) : AbstractIdEntity(idInternal = id) {

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

  fun isSame(other: Address): Boolean = firstLine == other.firstLine &&
    secondLine.orEmpty() == other.secondLine.orEmpty() &&
    townOrCity == other.townOrCity &&
    county.orEmpty() == other.county.orEmpty() &&
    postcode == other.postcode
}
