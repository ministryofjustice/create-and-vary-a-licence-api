package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.kotlinjpaspecificationdsl

/*
Duplicated work under the Apache 2.0 licence http://www.apache.org/licenses/LICENSE-2.0
Hats off to https://github.com/consoleau/kotlin-jpa-specification-dsl and contributors for the original.
Implemented here as a result of the JCenter repository closing and it not being available in other repositories yet.
*/

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.From
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import kotlin.reflect.KProperty1

// Helper to allow joining to Properties
fun <Z, T, R> From<Z, T>.join(prop: KProperty1<T, R?>): Join<T, R> = this.join(prop.name)

// Helper to enable get by Property
fun <R> Path<*>.get(prop: KProperty1<*, R?>): Path<R> = this.get(prop.name)

// Version of Specification.where that makes the CriteriaBuilder implicit
fun <T> where(makePredicate: CriteriaBuilder.(Root<T>) -> Predicate): Specification<T> = Specification { root, _, criteriaBuilder -> criteriaBuilder.makePredicate(root) }

// helper function for defining Specification that take a Path to a property and send it to a CriteriaBuilder
private fun <T, R> KProperty1<T, R?>.spec(makePredicate: CriteriaBuilder.(path: Path<R>) -> Predicate): Specification<T> = let { property -> where { root -> makePredicate(root.get(property)) } }

// Equality
fun <T, R> KProperty1<T, R?>.equal(x: R): Specification<T> = spec { equal(it, x) }

fun <T, R> KProperty1<T, R?>.notEqual(x: R): Specification<T> = spec { notEqual(it, x) }

// Ignores empty collection otherwise an empty 'in' predicate will be generated which will never match any results
fun <T, R : Any> KProperty1<T, R?>.includedIn(values: Collection<R>): Specification<T> = if (values.isNotEmpty()) {
  spec { path ->
    `in`(path).apply { values.forEach { this.value(it) } }
  }
} else {
  Specification.where(null)
}

// Comparison
fun <T> KProperty1<T, Number?>.le(x: Number) = spec { le(it, x) }

fun <T> KProperty1<T, Number?>.lt(x: Number) = spec { lt(it, x) }
fun <T> KProperty1<T, Number?>.ge(x: Number) = spec { ge(it, x) }
fun <T> KProperty1<T, Number?>.gt(x: Number) = spec { gt(it, x) }
fun <T, R : Comparable<R>> KProperty1<T, R?>.lessThan(x: R) = spec { lessThan(it, x) }
fun <T, R : Comparable<R>> KProperty1<T, R?>.lessThanOrEqualTo(x: R) = spec { lessThanOrEqualTo(it, x) }
fun <T, R : Comparable<R>> KProperty1<T, R?>.greaterThan(x: R) = spec { greaterThan(it, x) }
fun <T, R : Comparable<R>> KProperty1<T, R?>.greaterThanOrEqualTo(x: R) = spec { greaterThanOrEqualTo(it, x) }
fun <T, R : Comparable<R>> KProperty1<T, R?>.between(x: R, y: R) = spec { between(it, x, y) }

// True/False
fun <T> KProperty1<T, Boolean?>.isTrue() = spec { isTrue(it) }

fun <T> KProperty1<T, Boolean?>.isFalse() = spec { isFalse(it) }

// Collections
fun <T, R : Collection<*>> KProperty1<T, R?>.isEmpty() = spec { isEmpty(it) }

fun <T, R : Collection<*>> KProperty1<T, R?>.isNotEmpty() = spec { isNotEmpty(it) }
fun <T, E, R : Collection<E>> KProperty1<T, R?>.isMember(elem: E) = spec { isMember(elem, it) }
fun <T, E, R : Collection<E>> KProperty1<T, R?>.isNotMember(elem: E) = spec { isNotMember(elem, it) }

// Strings
fun <T> KProperty1<T, String?>.like(x: String): Specification<T> = spec { like(it, x) }

fun <T> KProperty1<T, String?>.like(x: String, escapeChar: Char): Specification<T> = spec { like(it, x, escapeChar) }
fun <T> KProperty1<T, String?>.notLike(x: String): Specification<T> = spec { notLike(it, x) }
fun <T> KProperty1<T, String?>.notLike(x: String, escapeChar: Char): Specification<T> = spec { notLike(it, x, escapeChar) }

// And
infix fun <T> Specification<T>.and(other: Specification<T>): Specification<T> = this.and(other)

inline fun <reified T> and(vararg specs: Specification<T>?): Specification<T> = and(specs.toList())

inline fun <reified T> and(specs: Iterable<Specification<T>?>): Specification<T> = combineSpecification(specs, Specification<T>::and)

// Or
infix fun <T> Specification<T>.or(other: Specification<T>): Specification<T> = this.or(other)

inline fun <reified T> or(vararg specs: Specification<T>?): Specification<T> = or(specs.toList())

inline fun <reified T> or(specs: Iterable<Specification<T>?>): Specification<T> = combineSpecification(specs, Specification<T>::or)

// Not
operator fun <T> Specification<T>.not(): Specification<T> = Specification.not(this)

// Combines Specification with an operation
inline fun <reified T> combineSpecification(specs: Iterable<Specification<T>?>, operation: Specification<T>.(Specification<T>) -> Specification<T>): Specification<T> = specs.filterNotNull().fold(emptySpecification()) { existing, new -> existing.operation(new) }

// Empty Specification
inline fun <reified T> emptySpecification(): Specification<T> = Specification.where(null)
