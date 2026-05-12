package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CurfewTimes

@Component
class CurfewTimesMapper {

  companion object {
    fun copy(entity: CurfewTimes?): CurfewTimes? {
      if (entity == null) return null

      return CurfewTimes(
        id = null,
        fromDay = entity.fromDay,
        fromTime = entity.fromTime,
        untilDay = entity.untilDay,
        untilTime = entity.untilTime,
        curfewTimesSequence = entity.curfewTimesSequence,
      )
    }

    fun copyList(entities: List<CurfewTimes>?): MutableList<CurfewTimes> = entities
      ?.mapNotNull { copy(it) }
      ?.toMutableList()
      ?: mutableListOf()
  }
}
