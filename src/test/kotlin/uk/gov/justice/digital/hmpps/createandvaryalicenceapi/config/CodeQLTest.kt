package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import org.junit.Test
import java.security.SecureRandom

class CodeQLTest {

  @Test
  fun `Should cause CodeQL to find an issue`() {
    val badPrng = SecureRandom()
    var randomData = 0

    // BAD: Using a constant value as a seed for a random number generator means all numbers it generates are predictable.
    badPrng.setSeed(12345L)
    randomData = badPrng.nextInt(32)

// BAD: System.currentTimeMillis() returns the system time which is predictable.
    badPrng.setSeed(System.currentTimeMillis())
    randomData = badPrng.nextInt(32)

// GOOD: SecureRandom implementations seed themselves securely by default.
    val goodPrng = SecureRandom()
    randomData = goodPrng.nextInt(32)
  }
}
