package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.ProtectedByIngress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.PublicEndpoint
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method

class EndpointSecurityTest {

  private val controllersToIgnore = listOf(
    "uk.gov.justice.hmpps.sqs.HmppsQueueResource",
    "uk.gov.justice.hmpps.sqs.HmppsReactiveQueueResource",
    "uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DashboardDefinitionController",
    "uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.DataApiSyncController",
    "uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ReportDefinitionController",
    "uk.gov.justice.digital.hmpps.digitalprisonreportinglib.productCollection.ProductCollectionController",
  )

  private data class EndpointInfo(
    val method: String,
    val hasEndpointLevelProtection: Boolean,
  )

  private data class ControllerInfo(
    val controller: String,
    val unprotectedEndpoints: List<EndpointInfo>,
  ) {
    override fun toString(): String {
      val endpointDescription = unprotectedEndpoints.joinToString(separator = "\n * ", prefix = "\n * ") { it.method }
      return "\n$controller:$endpointDescription".trimEnd()
    }
  }

  @Test
  fun `Ensure endpoints are checking roles`() {
    val controllers = getAllUnprotectedControllers()

    if (controllers.isNotEmpty()) {
      fail("Role checks missing in following locations: ${controllers.joinToString("\n")}\n")
    }
  }

  private fun getAllUnprotectedControllers() = ClassPathScanningCandidateComponentProvider(false)
    .also { it.addIncludeFilter(AnnotationTypeFilter(RestController::class.java)) }
    .findCandidateComponents("uk.gov.justice")
    .map { Class.forName(it.beanClassName) }
    .filter { !it.isProtectedByAnnotation() && !controllersToIgnore.contains(it.name) }
    .map { ControllerInfo(it.toString(), it.getUnprotectedEndpoints()) }
    .filter { it.unprotectedEndpoints.isNotEmpty() }

  private fun Class<*>.getUnprotectedEndpoints() = this.methods
    .filter { it.isEndpoint() }
    .map { EndpointInfo(it.toString(), it.isProtectedByAnnotation()) }
    .filter { ep -> !ep.hasEndpointLevelProtection }

  private fun Method.isEndpoint() = this.annotations.any {
    it.annotationClass.qualifiedName!!.startsWith("org.springframework.web.bind.annotation")
  }

  private fun AnnotatedElement.isProtectedByAnnotation(): Boolean {
    if (ANNOTATIONS_THAT_DENOTE_EXCLUSION.any { this.isAnnotationPresent(it) }) return true
    val annotation = getAnnotation(PreAuthorize::class.java) ?: return false
    return annotation.value.contains("hasAnyRole") || annotation.value.contains("hasRole")
  }

  companion object {
    val ANNOTATIONS_THAT_DENOTE_EXCLUSION = setOf(ProtectedByIngress::class.java, PublicEndpoint::class.java)
  }
}
