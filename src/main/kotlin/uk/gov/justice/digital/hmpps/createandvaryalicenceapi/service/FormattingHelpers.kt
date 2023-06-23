package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.Case.CAPITALISED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.Case.LOWER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.Case.UPPER

fun String?.getPlaceholderNames(): List<String> =
  this?.let {
    "\\{(.*?)}".toRegex().findAll(this)
      .map { it.groupValues[1].trim() }
      .toList()
  } ?: emptyList()

fun String.replacePlaceholder(placeholder: String, replacement: String) =
  this.replaceFirst("{$placeholder}", replacement)

private fun properCase(word: String) =
  if (word.isNotEmpty()) word[0].uppercase() + word.lowercase().substring(1) else word

/**
 * Converts a name (first name, last name, middle name, etc.) to proper case equivalent, handling double-barreled names
 * correctly (i.e. each part in a double-barreled is converted to proper case).
 */
private fun properCaseName(name: String) =
  if (name.isBlank()) "" else name.split('-').joinToString("-") { properCase(it) }

fun String.convertToTitleCase() =
  if (this.isBlank()) "" else this.split(" ").joinToString(" ") { properCaseName(it) }

fun formatAddress(address: String) = address.split(", ").filter { (it.trim().isNotEmpty()) }.joinToString(", ")

fun String.startsWithVowel() = setOf('a', 'e', 'i', 'o', 'u').contains(this.elementAtOrNull(0)?.lowercaseChar())

enum class Case(@JsonValue val description: String) {
  LOWER("lower"),
  UPPER("upper"),
  CAPITALISED("capitalised"),
}

/**
 * Adjusts the case of a value according to the rule specified.
 */
fun String.adjustCase(case: Case?): String {
  if (case == null) {
    return this
  }
  return when (case) {
    LOWER -> this.lowercase()
    UPPER -> this.uppercase()
    CAPITALISED -> this.convertToTitleCase()
  }
}

fun List<String>.formatUsing(listType: String): String = when (this.size) {
  0 -> ""
  1 -> this.first()
  else ->
    this
      .dropLast(1)
      .joinToString(separator = ", ", postfix = " ${listType.lowercase()} ${this.last()}")
}
