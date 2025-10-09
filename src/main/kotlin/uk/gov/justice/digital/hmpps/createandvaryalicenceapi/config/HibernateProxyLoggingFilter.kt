package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonUser

private const val STATE_PACKAGE = "org.hibernate.engine.internal.StatefulPersistenceContext"
private const val MESSAGE = "HHH000179: Narrowing proxy to class"
private val SUPPRESSED_CLASSES = setOf(
  CommunityOffenderManager::class.java.name,
  PrisonUser::class.java.name,
)

/**
 * A logback filter to suppress specific Hibernate proxy warnings that we cannot do anything about.
 *
 * When Hibernate lazily loads a CommunityOffenderManager/Prison (which extends Staff),
 * it gives a proxy object instead of the real entity.
 *
 * <p>This proxy may only know it is a Staff, not the actual subclass.</p>
 *
 * <p>If you do something that needs the real class—like casting, calling ::class,
 * or accessing subclass properties—Hibernate must “narrow” the proxy to
 * CommunityOffenderManager.</p>
 *
 * <p>This triggers the HHH000179 warning, meaning:
 * "Hibernate had to convert the proxy to the real entity class."</p>
*/
class HibernateProxyLoggingFilter : Filter<ILoggingEvent>() {

  override fun decide(event: ILoggingEvent?): FilterReply {
    event?.let {
      val loggerName = it.loggerName
      if (it.level == Level.WARN && loggerName == STATE_PACKAGE) {
        val msg = it.formattedMessage
        if (SUPPRESSED_CLASSES.any { className -> msg.contains(className) && msg.startsWith(MESSAGE) }) {
          return FilterReply.DENY
        }
      }
    }
    return FilterReply.NEUTRAL
  }
}
