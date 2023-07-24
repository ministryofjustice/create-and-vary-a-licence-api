package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource

import jakarta.validation.Valid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.lang.reflect.Method

class ControllerTest {
  private data class InvalidController(val clazz: Class<*>, val methods: List<Method>)

  @Test
  fun `Check Valid annotation is being applied to endpoints with payloads`() {
    val invalidControllers = getAllControllers().mapNotNull {
      val clazz = Class.forName(it.beanClassName)
      val invalidEndpoints = clazz.getInvalidEndpoints()
      if (invalidEndpoints.isEmpty()) null else InvalidController(clazz, invalidEndpoints)
    }

    assertThat(invalidControllers).isEmpty()
  }

  private fun getAllControllers() = ClassPathScanningCandidateComponentProvider(false).also {
    it.addIncludeFilter(AnnotationTypeFilter(RestController::class.java))
  }.findCandidateComponents("uk.gov.justice.digital.hmpps")

  private fun Class<*>.getInvalidEndpoints(): List<Method> = this.methods.filter {
    it.parameters.any { param ->
      param.isAnnotationPresent(RequestBody::class.java) && !param.isAnnotationPresent(
        Valid::class.java,
      )
    }
  }
}
