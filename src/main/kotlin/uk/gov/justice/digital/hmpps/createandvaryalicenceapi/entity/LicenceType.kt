package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "licence_type")
data class LicenceType(
  @Id
  @NotNull
  val typeCode: String = "",

  val description: String? = null,
)
