package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.Batching.Gatherer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.Batching.batchRequests

class BatchingTest {

  @Test
  fun emptyList() {
    val gatherer = mock<Gatherer<Long, String>>()
    val result = batchRequests(10, emptyList<Long>(), gatherer)

    assertThat(result).isEmpty()
    verifyNoInteractions(gatherer)
  }

  @Test
  fun lessThanBatchSize() {
    var callCount = 0
    val gatherer = Gatherer<Int, Int> {
      callCount++
      it
    }

    val result = batchRequests(7, listOf(1, 2, 3, 4, 5, 6), gatherer)

    assertThat(result).containsExactly(1, 2, 3, 4, 5, 6)
    assertThat(callCount).isEqualTo(1)
  }

  @Test
  fun sameAsBatchSize() {
    var callCount = 0
    val gatherer = Gatherer<Int, Int> {
      callCount++
      it
    }

    val result = batchRequests(6, listOf(1, 2, 3, 4, 5, 6), gatherer)

    assertThat(result).containsExactly(1, 2, 3, 4, 5, 6)
    assertThat(callCount).isEqualTo(1)
  }

  @Test
  fun moreThanBatchSize() {
    var callCount = 0
    val gatherer = Gatherer<Int, Int> {
      callCount++
      it
    }

    val result = batchRequests(3, listOf(1, 2, 3, 4, 5, 6, 7, 8, 9), gatherer)

    assertThat(result).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9)
    assertThat(callCount).isEqualTo(3)
  }
}
