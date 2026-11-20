package uk.gov.justice.digital.hmpps.createandvaryalicenceapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.Lazy
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.MigrationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcEventService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcLifecycleService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceCreationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService

class ComponentDependencyCycleTest {
  @Test
  fun `component constructor dependencies must not contain cycles`() {
    val scanner = ClassPathScanningCandidateComponentProvider(false)
    scanner.addIncludeFilter(AnnotationTypeFilter(Component::class.java))
    val components = scanner.findCandidateComponents("uk.gov.justice.digital.hmpps.createandvaryalicenceapi")
      .map { Class.forName(requireNotNull(it.beanClassName), false, javaClass.classLoader) }
      .toSet()

    assertThat(components).contains(
      HdcLifecycleService::class.java,
      HdcEventService::class.java,
      LicenceService::class.java,
      LicenceCreationService::class.java,
      MigrationService::class.java,
    )
    val cycle = findCycle(components)
    assertThat(cycle)
      .withFailMessage { "Circular component constructor dependency: ${cycle?.joinToString(" -> ") { it.name }}" }
      .isNull()
  }

  @Test
  fun `detects direct and indirect cycles`() {
    assertThat(findCycle(setOf(DirectA::class.java, DirectB::class.java)))
      .containsExactly(DirectA::class.java, DirectB::class.java, DirectA::class.java)
    assertThat(findCycle(setOf(IndirectA::class.java, IndirectB::class.java, IndirectC::class.java)))
      .containsExactly(IndirectA::class.java, IndirectB::class.java, IndirectC::class.java, IndirectA::class.java)
    assertThat(findCycle(setOf(IndirectA::class.java, IndirectB::class.java))).isNull()
    assertThat(findCycle(setOf(InterfaceConsumer::class.java, InterfaceImplementation::class.java)))
      .containsExactly(InterfaceConsumer::class.java, InterfaceImplementation::class.java, InterfaceConsumer::class.java)
  }

  private fun findCycle(components: Set<Class<*>>): List<Class<*>>? {
    val visited = mutableSetOf<Class<*>>()
    val path = mutableListOf<Class<*>>()

    fun visit(component: Class<*>): List<Class<*>>? {
      val cycleStart = path.indexOf(component)
      if (cycleStart >= 0) return path.drop(cycleStart) + component
      if (!visited.add(component)) return null

      path.add(component)
      val dependencies = component.declaredConstructors
        .filterNot { it.isSynthetic }
        .flatMap { it.parameterTypes.asList() }
        .flatMap { parameter -> components.filter { parameter.isAssignableFrom(it) } }
      for (dependency in dependencies) {
        visit(dependency)?.let { return it }
      }
      path.removeAt(path.lastIndex)
      return null
    }

    for (component in components) {
      visit(component)?.let { return it }
    }
    return null
  }

  private class DirectA(@Lazy val dependency: DirectB)
  private class DirectB(val dependency: DirectA)
  private class IndirectA(val dependency: IndirectB)
  private class IndirectB(val dependency: IndirectC)
  private class IndirectC(val dependency: IndirectA)
  private interface Dependency
  private class InterfaceConsumer(val dependency: Dependency)
  private class InterfaceImplementation(val dependency: InterfaceConsumer) : Dependency
}
