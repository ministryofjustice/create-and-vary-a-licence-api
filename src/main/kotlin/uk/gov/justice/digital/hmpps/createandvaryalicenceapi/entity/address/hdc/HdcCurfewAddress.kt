package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.hdc

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.AbstractAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.AddressSource
import java.time.LocalDateTime

@Entity
@Table(name = "hdc_curfew_address")
class HdcCurfewAddress(
  id: Long? = null,

  @OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.REFRESH])
  @JoinColumn(name = "licence_id", nullable = false)
  var licence: Licence,

  @Column(name = "accommodation_type")
  @Enumerated(EnumType.STRING)
  var accommodationType: AccommodationType? = null,

  @Column(name = "post_release_residential_checks_completed")
  var postReleaseResidentialChecksCompleted: Boolean? = null,

  @Column(name = "post_release_residential_checks_not_completed_reason")
  var postReleaseResidentialChecksNotCompletedReason: String? = null,

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
) {

  fun copy(licence: Licence) = HdcCurfewAddress(
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
    licence = licence,
    accommodationType = accommodationType,
    postReleaseResidentialChecksCompleted = postReleaseResidentialChecksCompleted,
  )
}
