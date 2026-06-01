package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address

import jakarta.persistence.Entity
import jakarta.persistence.Table
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
  createdTimestamp: LocalDateTime = LocalDateTime.now(),
  lastUpdatedTimestamp: LocalDateTime = LocalDateTime.now(),
) : AbstractAddress(
  id = id,
  reference = reference,
  uprn = uprn,
  firstLine = firstLine,
  secondLine = secondLine,
  townOrCity = townOrCity,
  county = county,
  postcode = postcode,
  source = source,
  createdTimestamp = createdTimestamp,
  lastUpdatedTimestamp = lastUpdatedTimestamp,
)
