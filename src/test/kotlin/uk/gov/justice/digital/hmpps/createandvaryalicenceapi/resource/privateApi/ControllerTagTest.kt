package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import io.swagger.v3.oas.annotations.tags.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.web.bind.annotation.RestController
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method

class ControllerTagTest {
  private data class ControllerInfo(
    val controller: String,
    val unTaggedEndpoints: List<String>,
  ) {
    override fun toString(): String {
      val endpointDescription = unTaggedEndpoints.joinToString(separator = "\n * ", prefix = "\n * ") { it }
      return "\n$controller:$endpointDescription".trimEnd()
    }
  }

  @Test
  fun `Ensure endpoints have tags`() {
    val controllers = getAllUnTaggedControllers()

    if (controllers.isNotEmpty()) {
      fail("Tags missing in following locations: ${controllers.joinToString("\n")}\n")
    }
  }

  private fun getAllUnTaggedControllers() = ClassPathScanningCandidateComponentProvider(false)
    .also { it.addIncludeFilter(AnnotationTypeFilter(RestController::class.java)) }
    .findCandidateComponents("uk.gov.justice")
    .map { Class.forName(it.beanClassName) }
    .filter { !it.hasTagAnnotation() }
    .map { ControllerInfo(it.toString(), it.getUnTaggedEndpoints()) }
    .filter { it.unTaggedEndpoints.isNotEmpty() }

  private fun Class<*>.getUnTaggedEndpoints() = this.methods
    .filter { it.isEndpoint() }
    .filter { !it.hasTagAnnotation() }
    .map { it.toString() }

  private fun Method.isEndpoint() = this.annotations.any {
    it.annotationClass.qualifiedName!!.startsWith("org.springframework.web.bind.annotation")
  }

  private fun AnnotatedElement.hasTagAnnotation() = getAnnotation(Tag::class.java) != null
}
