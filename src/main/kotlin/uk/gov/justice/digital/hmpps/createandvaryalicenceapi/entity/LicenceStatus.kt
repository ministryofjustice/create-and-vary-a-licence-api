package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "licence_status")
data class LicenceStatus (
  @Id
  @NotNull
  val statusCode: String = "",

  val description: String? = null,
)