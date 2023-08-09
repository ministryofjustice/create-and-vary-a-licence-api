import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.lang.reflect.Method

fun main() {
  val controllers = getAllControllers().mapNotNull {
    val clazz = Class.forName(it.beanClassName)
    val endpoints = clazz.getEndpoints()

    ControllerInfo(
      clazz,
      clazz.isAnnotationPresent(PreAuthorize::class.java),
      endpoints,
      clazz.getAnnotation(PreAuthorize::class.java)?.value,
    )
  }

  val json = ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(
    controllers.map { toJsonDto(it) },
  )

  println(json)
}

private data class EndpointInfo(
  val method: Method,
  val hasEndpointLevelProtection: Boolean,
  val authString: String? = null,
  val missingRoleCheck: Boolean,
)

private data class ControllerInfo(
  val clazz: Class<*>,
  val hasClassLevelProtection: Boolean,
  val endpoints: List<EndpointInfo>,
  val authString: String? = null,
)


private fun getAllControllers() = ClassPathScanningCandidateComponentProvider(false).also {
  it.addIncludeFilter(AnnotationTypeFilter(RestController::class.java))
}.findCandidateComponents("uk.gov.justice.digital.hmpps")

private fun Class<*>.getEndpoints() = getEndpointMethods().map { endpoint ->
  EndpointInfo(
    endpoint,
    endpoint.isAnnotationPresent(PreAuthorize::class.java),
    endpoint.getAnnotation(PreAuthorize::class.java)?.value,
    endpoint.getAnnotation(PreAuthorize::class.java)?.value?.contains("hasAnyRole")?.let { !it } ?: false,
  )
}

private fun toJsonDto(info: ControllerInfo): Map<String, Any?> {
  val protectedEndpoints = info.endpoints.count { it.hasEndpointLevelProtection }
  val hasAnyViolation =
    !info.hasClassLevelProtection && protectedEndpoints != info.endpoints.size && info.endpoints.any { it.missingRoleCheck }
  return mapOf(
    "controllerName" to info.clazz.simpleName,
    "violationDetected" to hasAnyViolation,
    "authorizeAnnotationPresent" to info.hasClassLevelProtection,
    "authString" to info.authString,
    "protectedEndpoints" to "${protectedEndpoints}/${info.endpoints.size}",
    "endpoints" to info.endpoints.map {
      mapOf(
        "name" to it.method.name,
        "authorizeAnnotationPresent" to it.hasEndpointLevelProtection,
        "authString" to it.authString,
        "missingRoleCheck" to it.missingRoleCheck,
      )
    },
  )
}

private fun Class<*>.getEndpointMethods() = this.methods.filter {
  it.annotations.any {
    it.annotationClass.qualifiedName?.startsWith("org.springframework.web.bind.annotation")
      ?: error("no annotation name? ${it.annotationClass}")
  }
}
