package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.aspects

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.hibernate.Hibernate
import org.hibernate.proxy.HibernateProxy
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import java.lang.reflect.Modifier
import java.util.Optional

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
  fun unProxyRepositoryResult(joinPoint: ProceedingJoinPoint): Any? {
    val method = (joinPoint.signature as MethodSignature).method
    val returnType = method.returnType

    if (isPrimitiveOrWrapperType(returnType)) {
      return joinPoint.proceed()
    }

    val returnedObject = joinPoint.proceed()
    return unProxy(returnedObject)
  }

  fun unProxy(returnedObject: Any?): Any? {
    if (returnedObject == null) return null
    if (isPrimitiveOrWrapper(returnedObject)) return returnedObject

    return when (returnedObject) {
      is HibernateProxy -> unProxyIfAbstract(returnedObject)
      is Set<*> -> returnedObject.mapTo(mutableSetOf()) { unProxy(it) }
      is List<*> -> returnedObject.map { unProxy(it) }
      is Collection<*> -> returnedObject.map { unProxy(it) }
      is Page<*> -> returnedObject.map { unProxy(it) }
      is Optional<*> -> returnedObject.map { unProxy(it) }
      is Map<*, *> -> returnedObject.mapValues { unProxy(it.value) }
      else -> returnedObject
    }
  }

  fun unProxyIfAbstract(proxy: HibernateProxy): Any {
    val implClass = proxy.hibernateLazyInitializer.persistentClass
    return if (Modifier.isAbstract(implClass.modifiers)) {
      Hibernate.unproxy(proxy)
    } else {
      proxy
    }
  }

  private fun isPrimitiveOrWrapper(returnedObject: Any): Boolean = isPrimitiveOrWrapperType(returnedObject.javaClass)

  private fun isPrimitiveOrWrapperType(clazz: Class<*>): Boolean = clazz.isPrimitive ||
    clazz == java.lang.String::class.java ||
    clazz == java.lang.Integer::class.java ||
    clazz == java.lang.Boolean::class.java ||
    clazz == java.lang.Long::class.java ||
    clazz.isEnum ||
    clazz == java.lang.Double::class.java ||
    clazz == java.lang.Character::class.java ||
    clazz == java.lang.Float::class.java ||
    clazz == java.lang.Short::class.java ||
    clazz == java.lang.Byte::class.java ||
    Number::class.java.isAssignableFrom(clazz)
}
