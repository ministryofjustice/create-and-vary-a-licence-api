package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.licence.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.licence.LicenceLinkType

@Entity
class LicenceLink(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  val fromLicenceId: Long,

  val toLicenceId: Long,

  @Enumerated(EnumType.STRING)
  val linkType: LicenceLinkType,
)
