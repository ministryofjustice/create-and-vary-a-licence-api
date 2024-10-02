package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "hdc_curfew_times")
data class HdcCurfewTimes(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @NotNull
  val id: Long = -1,

  @ManyToOne
  @JoinColumn(name = "licence_id", nullable = false)
  var licence: Licence,

  var curfewTimesSequence: Int? = null,
  val fromDay: LocalTime? = null,
  val fromTime: LocalTime? = null,
  val untilDay: LocalTime? = null,
  val untilTime: LocalTime? = null,
  val createdTimestamp: LocalDateTime? = null,
) {
  override fun toString(): String {
    return "HdcCurfewTimes(" +
      "id=$id, " +
      "licence=${licence.id}, " +
      "curfewTimesSequence=$curfewTimesSequence, " +
      "fromDay=$fromDay, " +
      "fromTime=$fromTime, " +
      "untilDay=$untilDay, " +
      "untilTime=$untilTime" +
      "created_timestamp=$createdTimestamp" +
      ")"
  }
}
