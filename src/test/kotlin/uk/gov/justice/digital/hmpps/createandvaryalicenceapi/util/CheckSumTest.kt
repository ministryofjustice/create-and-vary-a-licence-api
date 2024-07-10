package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets.UTF_8

class CheckSumTest {
  @Nested
  inner class ToChecksum {
    @Test
    fun `when value present`() {
      val someValue = "someValue".toByteArray(UTF_8).toCheckSum()

      assertThat(someValue).isEqualTo("e830fe9dbfcf848b5ccb6e7f6a848cf4")
    }

    @Test
    fun `when value is absent`() {
      val nullArray: ByteArray? = null
      val someValue = nullArray.toCheckSum()

      assertThat(someValue).isNull()
    }
  }

  @Nested
  inner class VerifyCheckSum {
    @Test
    fun `when value present and valid checksum`() {
      val byteArray = "someValue".toByteArray(UTF_8)
      val checkSum = byteArray.toCheckSum()

      byteArray.verifyCheckSum(checkSum)
    }

    @Test
    fun `when value present and invalid checksum`() {
      val byteArray = "someValue".toByteArray(UTF_8)
      val checkSum = "NOT THE CHECKSUM"

      assertThatThrownBy { byteArray.verifyCheckSum(checkSum) }.isInstanceOf(IllegalStateException::class.java)
        .hasMessage("Checksum mismatch (expected: 'NOT THE CHECKSUM', actual: 'e830fe9dbfcf848b5ccb6e7f6a848cf4')")
    }

    @Test
    fun `when value absent and checksum absent`() {
      val byteArray: ByteArray? = null
      val checkSum: String? = null

      byteArray.verifyCheckSum(checkSum)
    }

    @Test
    fun `when value absent but checksum present`() {
      val byteArray: ByteArray? = null
      val checkSum = "THE CHECKSUM"

      assertThatThrownBy { byteArray.verifyCheckSum(checkSum) }.isInstanceOf(IllegalStateException::class.java)
        .hasMessage("Checksum mismatch (expected: 'THE CHECKSUM', actual: 'null')")
    }
  }
}
