package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.ILicenceCondition

fun <T : ILicenceCondition> removedConditions(current: List<T>, other: List<T>): List<T> =
  current.filter { pssElement ->
    other.findLast { pss -> pss.code == pssElement.code } == null
  }

fun <T : ILicenceCondition> addedConditions(current: List<T>, other: List<T>): List<T> =
  other.filter { pssElement ->
    current.findLast { pss -> pss.code == pssElement.code } == null
  }

fun <T : ILicenceCondition> amendedConditions(
  current: List<T>,
  other: List<T>
): List<Pair<T, T>> =
  current.mapNotNull { c -> other.find { it.code == c.code && it != c }?.let { Pair(it, c) } }

fun <T : ILicenceCondition> conditionChanges(current: List<T>, other: List<T>) = ConditionChanges(
  removedConditions(current, other),
  addedConditions(current, other),
  amendedConditions(current, other)
)
