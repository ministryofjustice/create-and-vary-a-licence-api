package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

object Batching {
  fun interface Gatherer<ID, RESULT> {
    fun gather(ids: List<ID>): List<RESULT>?
  }

  fun <ID, RESULT> batchRequests(
    size: Int,
    ids: Collection<ID>,
    gatherer: Gatherer<ID, RESULT>,
  ): List<RESULT> {
    if (ids.isEmpty()) return emptyList()
    val batches = ids.chunked(size)
    val results = batches.map { batch -> gatherer.gather(batch) ?: emptyList() }
    return results.flatten()
  }
}
