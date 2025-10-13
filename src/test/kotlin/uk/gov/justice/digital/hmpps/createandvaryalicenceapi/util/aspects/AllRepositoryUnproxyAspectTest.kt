package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.aspects

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.proxy.HibernateProxy
import org.hibernate.proxy.LazyInitializer
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.*

class AllRepositoryUnproxyAspectTest {

  // Testing proxy objects without Hibernate
  private val aspect = object : AllRepositoryUnproxyAspect() {
    var forceUnproxy: Boolean = true
    override fun unProxyIfAbstract(proxy: HibernateProxy): Any = if (forceUnproxy) "UNPROXIED" else proxy
  }

  private val proxy = object : HibernateProxy {
    override fun writeReplace(): Any = mock()
    override fun getHibernateLazyInitializer(): LazyInitializer = mock()
  }

  @Test
  fun `should return null when object is null`() {
    // Given
    val input: Any? = null

    // When
    val result = aspect.unProxy(input)

    // Then
    assertThat(result).isNull()
  }

  @Test
  fun `should return primitive and wrapper types as is`() {
    // Given
    val stringValue = "hello"
    val intValue = 42
    val booleanValue = true
    val doubleValue = 3.14

    // When
    val stringResult = aspect.unProxy(stringValue)
    val intResult = aspect.unProxy(intValue)
    val booleanResult = aspect.unProxy(booleanValue)
    val doubleResult = aspect.unProxy(doubleValue)

    // Then
    assertThat(stringResult).isEqualTo(stringValue)
    assertThat(intResult).isEqualTo(intValue)
    assertThat(booleanResult).isEqualTo(booleanValue)
    assertThat(doubleResult).isEqualTo(doubleValue)
  }

  @Test
  fun `should unproxy collections, optionals, pages and maps recursively`() {
    // Given
    val entity1 = "entity1"
    val entity2 = "entity2"

    val list = listOf(entity1, entity2)
    val set = setOf(entity1, entity2)
    val page = PageImpl(list, PageRequest.of(0, 2), 2)
    val optional = Optional.of(entity1)
    val map = mapOf("first" to entity1, "second" to entity2)

    // When
    val listResult = aspect.unProxy(list)
    val setResult = aspect.unProxy(set)
    val pageResult = aspect.unProxy(page)
    val optionalResult = aspect.unProxy(optional)
    val mapResult = aspect.unProxy(map)

    // Then
    assertThat(listResult).isInstanceOf(List::class.java)
    val typedList = listResult as List<*>
    assertThat(typedList).containsExactly(entity1, entity2)

    assertThat(setResult).isInstanceOf(Set::class.java)
    val typedSet = setResult as Set<*>
    assertThat(typedSet).containsExactlyInAnyOrder(entity1, entity2)

    assertThat(pageResult).isInstanceOf(PageImpl::class.java)
    val typedPage = pageResult as PageImpl<*>
    assertThat(typedPage.content).containsExactly(entity1, entity2)

    assertThat(optionalResult).isInstanceOf(Optional::class.java)
    val typedOptional = optionalResult as Optional<*>
    assertThat(typedOptional.get()).isEqualTo(entity1)

    assertThat(mapResult).isInstanceOf(Map::class.java)
    val typedMap = mapResult as Map<*, *>
    assertThat(typedMap["first"]).isEqualTo(entity1)
    assertThat(typedMap["second"]).isEqualTo(entity2)
  }

  @Test
  fun `should force unproxy`() {
    // Given
    aspect.forceUnproxy = true

    // When
    val result = aspect.unProxy(proxy)

    // Then
    assertThat(result).isEqualTo("UNPROXIED")
  }

  @Test
  fun `should force no unproxy`() {
    // Given
    aspect.forceUnproxy = false

    // When
    val result = aspect.unProxy(proxy)

    // Then
    assertThat(result).isSameAs(proxy)
  }
}
