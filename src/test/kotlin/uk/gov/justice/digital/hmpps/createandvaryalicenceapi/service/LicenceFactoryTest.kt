package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LicenceFactoryTest {

  @Test
  fun `should check if a string contains a valid CRO number`() {
    assertThat(LicenceFactory.isValidCro(null)).isFalse
    assertThat(LicenceFactory.isValidCro("")).isFalse

    // ends with a digit excluding I, O and S
    assertThat(LicenceFactory.isValidCro("23456/12S")).isFalse

    // missing / after serial number
    assertThat(LicenceFactory.isValidCro("2345612A")).isFalse

    // no leading zeros allowed in serial number
    assertThat(LicenceFactory.isValidCro("03456/12A")).isFalse

    // serial number be between 1 and 6 digits with no leading zero
    assertThat(LicenceFactory.isValidCro("/12A")).isFalse
    assertThat(LicenceFactory.isValidCro("023456/12A")).isFalse
    assertThat(LicenceFactory.isValidCro("1234567/12A")).isFalse

    // year has to be two digits
    assertThat(LicenceFactory.isValidCro("123456/5A")).isFalse
    assertThat(LicenceFactory.isValidCro("123456/123A")).isFalse

    // Must contain one check digit character which is not I, O or S
    assertThat(LicenceFactory.isValidCro("123456/12AB")).isFalse
    assertThat(LicenceFactory.isValidCro("23456/12I")).isFalse
    assertThat(LicenceFactory.isValidCro("23456/12O")).isFalse
    assertThat(LicenceFactory.isValidCro("23456/12S")).isFalse

    assertThat(LicenceFactory.isValidCro("23456/12A")).isTrue
    assertThat(LicenceFactory.isValidCro("4/12G")).isTrue

    // no fingerprints held format
    //   must start with SF
    assertThat(LicenceFactory.isValidCro("SE40/4A")).isFalse
    assertThat(LicenceFactory.isValidCro("AF40/3B")).isFalse

    // year digits after SF must be in range 39 - 95
    assertThat(LicenceFactory.isValidCro("SF38/8F")).isFalse
    assertThat(LicenceFactory.isValidCro("SF96/5K")).isFalse

    // the serial number must be between 1 and 6 digits with no leading zero
    assertThat(LicenceFactory.isValidCro("SF64/0123R")).isFalse
    assertThat(LicenceFactory.isValidCro("SF39/R")).isFalse
    assertThat(LicenceFactory.isValidCro("SF39/1234567G")).isFalse

    // must have one check digit which cannot be I, O or S
    assertThat(LicenceFactory.isValidCro("SF56/1234")).isFalse
    assertThat(LicenceFactory.isValidCro("SF56/1234I")).isFalse
    assertThat(LicenceFactory.isValidCro("SF56/9875O")).isFalse
    assertThat(LicenceFactory.isValidCro("SF56/329303S")).isFalse

    assertThat(LicenceFactory.isValidCro("SF39/6W")).isTrue
    assertThat(LicenceFactory.isValidCro("SF95/123456X")).isTrue

    assertThat(LicenceFactory.isValidCro("466835/10U")).isTrue
    // for (cro in listOf()) {
    //   if (!LicenceFactory.isValidCro(cro)) println(cro)
    // }
  }
}
