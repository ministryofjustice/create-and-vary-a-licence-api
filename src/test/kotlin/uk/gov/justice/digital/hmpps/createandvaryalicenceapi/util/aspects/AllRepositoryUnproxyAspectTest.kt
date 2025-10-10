package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.aspects

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import java.util.Optional

class AllRepositoryUnproxyAspectTest {

  private val aspect = AllRepositoryUnproxyAspect()

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
}
