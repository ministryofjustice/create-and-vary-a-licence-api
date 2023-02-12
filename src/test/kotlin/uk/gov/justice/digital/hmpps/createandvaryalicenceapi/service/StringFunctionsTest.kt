package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class StringFunctionsTest {
  @Nested
  inner class `Convert to title case` {
    @Test
    fun `empty string`() {
      assertThat(convertToTitleCase("")).isEqualTo("")
    }

    @Test
    fun `Lower Case`() {
      assertThat(convertToTitleCase("robert")).isEqualTo("Robert")
    }

    @Test
    fun `Upper Case`() {
      assertThat(convertToTitleCase("ROBERT")).isEqualTo("Robert")
    }

    @Test
    fun `Mixed Case`() {
      assertThat(convertToTitleCase("RoBErT")).isEqualTo("Robert")
    }

    @Test
    fun `Multiple words`() {
      assertThat(convertToTitleCase("RobeRT SMiTH")).isEqualTo("Robert Smith")
    }

    @Test
    fun `Leading spaces`() {
      assertThat(convertToTitleCase("  RobeRT")).isEqualTo("  Robert")
    }

    @Test
    fun `Trailing spaces`() {
      assertThat(convertToTitleCase("RobeRT  ")).isEqualTo("Robert  ")
    }

    @Test
    fun `Hyphenated`() {
      assertThat(convertToTitleCase("Robert-John SmiTH-jONes-WILSON")).isEqualTo("Robert-John Smith-Jones-Wilson")
    }
  }

  @Nested
  inner class `Format address` {
    @Test
    fun empty() {
      assertThat(formatAddress("")).isEqualTo("")
    }

    @Test
    fun `one piece`() {
      assertThat(formatAddress("1 Main Road")).isEqualTo("1 Main Road")
    }

    @Test
    fun `two pieces`() {
      assertThat(formatAddress("1 Main Road, Bridgport")).isEqualTo("1 Main Road, Bridgport")
    }

    @Test
    fun `two pieces with empty piece in between`() {
      assertThat(formatAddress("1 Main Road, , Bridgport")).isEqualTo("1 Main Road, Bridgport")
    }

    @Test
    fun `empty pieces`() {
      assertThat(formatAddress(" , , ")).isEqualTo("")
    }
  }

  @Nested
  inner class `Starts with a vowel` {
    @Test
    fun `empty string`() {
      assertThat("".startsWithVowel()).isFalse
    }

    @Test
    fun `animal starts with vowel`() {
      assertThat("animal".startsWithVowel()).isTrue
    }

    @Test
    fun `zoo does not start with vowel`() {
      assertThat("zoo".startsWithVowel()).isFalse
    }

    @Test
    fun `case insensitive`() {
      assertThat("A".startsWithVowel()).isTrue
    }

    @Test
    fun `Z is not a vowel`() {
      assertThat("Z".startsWithVowel()).isFalse
    }
  }

  @Nested
  inner class `format as list` {
    @Test
    fun `empty list`() {
      assertThat(emptyList<String>().formatUsing("AND")).isEqualTo("")
    }

    @Test
    fun `single item`() {
      assertThat(listOf("word").formatUsing("AND")).isEqualTo("word")
    }

    @Test
    fun `two items joined with and`() {
      assertThat(listOf("word", "weight").formatUsing("AND")).isEqualTo("word and weight")
    }

    @Test
    fun `two items joined with or`() {
      assertThat(listOf("word", "weight").formatUsing("OR")).isEqualTo("word or weight")
    }

    @Test
    fun `multiple items joined with and`() {
      assertThat(listOf("word", "weight", "sound").formatUsing("AND")).isEqualTo("word, weight and sound")
    }

    @Test
    fun `multiple items joined with or`() {
      assertThat(listOf("word", "weight", "sound").formatUsing("OR")).isEqualTo("word, weight or sound")
    }
  }
}
