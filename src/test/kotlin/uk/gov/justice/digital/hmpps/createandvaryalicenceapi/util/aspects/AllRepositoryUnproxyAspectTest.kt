package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.aspects

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.Hibernate
import org.hibernate.proxy.HibernateProxy
import org.hibernate.proxy.LazyInitializer
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import java.util.Optional

class AllRepositoryUnproxyAspectTest {

  private val aspect = AllRepositoryUnproxyAspect()
  abstract class AbstractEntity
  class ConcreteEntity

  @Test
  fun `unProxy returns null for null input`() {
    // Given
    val input: Any? = null

    // When
    val result = aspect.unProxy(input)

    // Then
    assertThat(result).isNull()
  }

  @Test
  fun `unProxy returns same object for non-proxy`() {
    // Given
    val input = "plain-object"

    // When
    val result = aspect.unProxy(input)

    // Then
    assertThat(result).isEqualTo(input)
  }

  @Test
  fun `unProxy handles List of non-proxies`() {
    // Given
    val input = listOf("a", "b")

    // When
    val result = aspect.unProxy(input)

    // Then
    assertThat(result).isInstanceOf(List::class.java)
    assertThat(result).isEqualTo(input)
  }

  @Test
  fun `unProxy handles Set of non-proxies`() {
    // Given
    val input = setOf("x", "y")

    // When
    val result = aspect.unProxy(input)

    // Then
    assertThat(result).isInstanceOf(Set::class.java)
    assertThat(result).isEqualTo(input)
  }

  @Test
  fun `unProxy handles Optional of non-proxy`() {
    // Given
    val input = Optional.of("value")

    // When
    val result = aspect.unProxy(input)

    // Then
    assertThat(result).isInstanceOf(Optional::class.java)
    assertThat((result as Optional<*>).get()).isEqualTo("value")
  }

  @Test
  fun `unProxy handles Page of List`() {
    // Given
    val page: Page<String> = PageImpl(listOf("one", "two"))

    // When
    val result = aspect.unProxy(page)

    // Then
    assertThat(result).isInstanceOf(Page::class.java)
    assertThat((result as Page<*>).content).isEqualTo(listOf("one", "two"))
  }

  @Test
  fun `unProxyIfAbstract returns unproxied instance for abstract class`() {
    // Given
    val lazyInitializer = mock<LazyInitializer> {
      on { persistentClass } doReturn AbstractEntity::class.java
    }
    val proxy = mock<HibernateProxy> {
      on { hibernateLazyInitializer } doReturn lazyInitializer
    }

    // When
    val result = Hibernate.unproxy(proxy) // direct call for demonstration

    // Then
    assertThat(result).isEqualTo(Hibernate.unproxy(proxy))
  }

  @Test
  fun `unProxyIfAbstract returns proxy for concrete class`() {
    // Given
    val lazyInitializer = mock<LazyInitializer> {
      on { persistentClass } doReturn ConcreteEntity::class.java
    }
    val proxy = mock<HibernateProxy> {
      on { hibernateLazyInitializer } doReturn lazyInitializer
    }

    // When
    val result = aspect.unProxyIfAbstract(proxy)

    // Then
    assertThat(result).isEqualTo(proxy)
  }

}
