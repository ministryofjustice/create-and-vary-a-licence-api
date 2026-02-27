package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import jakarta.persistence.criteria.JoinType
import jakarta.validation.ValidationException
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.kotlinjpaspecificationdsl.and
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.kotlinjpaspecificationdsl.includedIn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Deprecated(message = "Don't use licence query - instead create repo specific queries")
data class LicenceQueryObject(
  val statusCodes: List<LicenceStatus>? = null,
  val nomsIds: List<String>? = null,
  val sortBy: String? = null,
  val sortOrder: String? = null,
)

fun LicenceQueryObject.toSpecification(): Specification<Licence> = and(
  hasStatusCodeIn(statusCodes),
  hasNomsIdIn(nomsIds),
).and { root, query, _ ->
  root.fetch<Licence, CommunityOffenderManager>("responsibleCom", JoinType.LEFT)
  query.distinct(true)
  null
}

fun LicenceQueryObject.getSort(): Sort = when {
  sortBy == null -> Sort.unsorted()
  sortOrder == null -> Sort.by(Sort.Direction.ASC, sortBy)
  else -> {
    try {
      Sort.by(Sort.Direction.fromString(sortOrder), sortBy)
    } catch (e: IllegalArgumentException) {
      throw ValidationException(e.message, e)
    }
  }
}

fun hasStatusCodeIn(statusCodes: List<LicenceStatus>?): Specification<Licence>? = statusCodes?.let {
  Licence::statusCode.includedIn(it)
}

fun hasNomsIdIn(nomsIds: List<String>?): Specification<Licence>? = nomsIds?.let {
  Licence::nomsId.includedIn(it)
}
