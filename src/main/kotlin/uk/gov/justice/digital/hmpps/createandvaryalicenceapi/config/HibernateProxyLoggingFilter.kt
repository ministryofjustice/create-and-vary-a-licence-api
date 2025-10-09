package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply

private const val STATE_PACKAGE = "org.hibernate.engine.internal.StatefulPersistenceContext"

private val SUPPRESSED_MESSAGES = setOf(
  "HHH000179: Narrowing proxy to class uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager - this operation breaks ==",
  "HHH000179: Narrowing proxy to class uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonUser - this operation breaks ==",
)

class HibernateProxyLoggingFilter : Filter<ILoggingEvent>() {

  override fun decide(event: ILoggingEvent?): FilterReply {
    event?.let {
      val loggerName = it.loggerName
      val msg = it.formattedMessage
      if (it.level == Level.WARN && loggerName == STATE_PACKAGE && msg in SUPPRESSED_MESSAGES) {
        return FilterReply.DENY
      }
    }
    return FilterReply.NEUTRAL
  }
}
