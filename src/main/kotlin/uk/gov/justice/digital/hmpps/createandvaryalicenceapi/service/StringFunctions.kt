package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

fun properCase(word: String) = if (word.isNotEmpty()) word[0].uppercase() + word.lowercase().substring(1) else word

/**
 * Converts a name (first name, last name, middle name, etc.) to proper case equivalent, handling double-barreled names
 * correctly (i.e. each part in a double-barreled is converted to proper case).
 * @param name name to be converted.
 * @returns name converted to proper case.
 */
fun properCaseName(name: String) = if (name.isBlank()) "" else name.split('-').joinToString("-") { properCase(it) }

fun convertToTitleCase(sentence: String) =
  if (sentence.isBlank()) "" else sentence.split(" ").joinToString(" ") { properCaseName(it) }

fun formatAddress(address: String) = address.split(", ").filter { (it.trim().isNotEmpty()) }.joinToString(", ")

fun String.startsWithVowel() = setOf('a', 'e', 'i', 'o', 'u').contains(this.elementAtOrNull(0)?.lowercaseChar())

/**
 * Adjusts the case of a value according to the rule specified.
 * @param caseRule
 * @param value
 */
fun String.adjustCase(caseRule: String?): String {
  if (caseRule == null) {
    return this
  }
  return when (caseRule.lowercase()) {
    "lower" -> this.lowercase()
    "upper" -> this.uppercase()
    "capitalised" -> convertToTitleCase(this)
    else -> this
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
