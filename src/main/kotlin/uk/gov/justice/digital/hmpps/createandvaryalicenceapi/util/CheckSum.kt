package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import java.security.MessageDigest

private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

fun ByteArray?.toCheckSum() = when {
  this == null -> null
  else -> MessageDigest.getInstance("MD5").digest(this).toHex()
}

fun ByteArray?.verifyCheckSum(expectedCheckSum: String?) {
  val calculatedChecksum = this.toCheckSum()
  check(calculatedChecksum == expectedCheckSum) { "Checksum mismatch (expected: '$expectedCheckSum', actual: '$calculatedChecksum')" }
}
