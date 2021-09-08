package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "licence_document")
data class LicenceDocument(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  val id: Long = -1,

  @NotNull
  val licenceId: Long = -1,

  @NotNull
  val createdTime: LocalDateTime = LocalDateTime.now(),

  val s3Location: String? = null,
  val documentName: String? = null,
  val expiryTime: LocalDateTime? = null,
  val description: String? = null,
  val metaData: String? = null,
)
