package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

@Entity
@Table(name = "licence_document")
data class LicenceDocument(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @param:Positive
  val id: Long? = null,

  @param:NotNull
  val licenceId: Long = -1,

  @param:NotNull
  val createdTime: LocalDateTime = LocalDateTime.now(),

  val s3Location: String? = null,
  val documentName: String? = null,
  val expiryTime: LocalDateTime? = null,
  val description: String? = null,
  val metaData: String? = null,
)
