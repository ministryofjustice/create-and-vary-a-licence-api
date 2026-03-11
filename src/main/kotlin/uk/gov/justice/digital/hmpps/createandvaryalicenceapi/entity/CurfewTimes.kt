package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.Positive
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "curfew_times")
class CurfewTimes(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @field:Positive
  open val id: Long? = null,
  val curfewTimesSequence: Int? = null,
  @Enumerated(EnumType.STRING)
  val fromDay: DayOfWeek? = null,
  val fromTime: LocalTime? = null,
  @Enumerated(EnumType.STRING)
  val untilDay: DayOfWeek? = null,
  val untilTime: LocalTime? = null,
  val createdTimestamp: LocalDateTime? = null,
) {
  override fun toString(): String = "CurfewTimes(" +
    "id=$id, " +
    "curfewTimesSequence=$curfewTimesSequence, " +
    "fromDay=$fromDay, " +
    "fromTime=$fromTime, " +
    "untilDay=$untilDay, " +
    "untilTime=$untilTime" +
    "created_timestamp=$createdTimestamp" +
    ")"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CurfewTimes) return false
    if (id != other.id) return false
    return true
  }

  override fun hashCode(): Int = super.hashCode()
}
