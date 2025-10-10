package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.aspects

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.hibernate.Hibernate
import org.hibernate.proxy.HibernateProxy
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import java.lang.reflect.Modifier
import java.util.*

/**
 * Aspect that intercepts all calls to Spring Data repositories and unproxies any returned entities
 * if they are abstract classes. This helps prevent issues related to lazy loading and proxy casting.
 *
 * <p>Hibernate creates proxy implementations at runtime for abstract entities. When such a proxy is
 * returned from a repository method, it may lead to {@code ClassCastException} if the proxy is cast
 * to a concrete class.
 *
 * <p>Example scenario:
 * <ul>
 *   <li>{@code LicenceService} returns a {@code Staff} object (abstract).</li>
 *   <li>Code attempts to cast it to {@code PrisonUser} (concrete).</li>
 *   <li>This fails because the proxy is not a {@code PrisonUser}, but a Hibernate-generated subclass of {@code Staff}.</li>
 * </ul>
 *
 * <p>This aspect unproxies such objects when returned directly from repository methods.
 * However, it does not resolve issues where proxy objects are accessed indirectly,
 * such as via lazy-loaded relationships (e.g., {@code licence.updatedBy}).
 */
@Aspect
@Component
class AllRepositoryUnproxyAspect {

  @Around("execution(* org.springframework.data.repository.Repository+.*(..))")
  fun unproxyRepositoryResult(joinPoint: ProceedingJoinPoint): Any? = unproxy(joinPoint.proceed())

  fun unproxy(obj: Any?): Any? {
    if (obj == null) return null

    return when (obj) {
      is HibernateProxy -> unproxyIfAbstract(obj)
      is Collection<*> -> obj.map { unproxy(it) }
      is Page<*> -> obj.map { unproxy(it) }
      is Optional<*> -> obj.map { unproxy(it) }
      else -> obj
    }
  }

  fun unproxyIfAbstract(proxy: HibernateProxy): Any {
    val implClass = proxy.hibernateLazyInitializer.persistentClass
    return if (Modifier.isAbstract(implClass.modifiers)) {
      Hibernate.unproxy(proxy)
    } else {
      proxy
    }
  }
}
