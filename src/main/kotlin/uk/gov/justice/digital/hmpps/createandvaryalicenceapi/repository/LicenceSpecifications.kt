package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import jakarta.validation.ValidationException
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.kotlinjpaspecificationdsl.and
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.kotlinjpaspecificationdsl.`includedIn`
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

data class LicenceQueryObject(
  val prisonCodes: List<String>? = null,
  val statusCodes: List<LicenceStatus>? = null,
  val staffIds: List<Int>? = null,
  val nomsIds: List<String>? = null,
  val pdus: List<String>? = null,
  val probationAreaCodes: List<String>? = null,
  val sortBy: String? = null,
  val sortOrder: String? = null,
)

fun LicenceQueryObject.toSpecification(): Specification<Licence> = and(
  hasStatusCodeIn(statusCodes),
  hasPrisonCodeIn(prisonCodes),
  hasNomsIdIn(nomsIds),
  hasResponsibleComIn(staffIds),
  hasPdusIn(pdus),
  hasProbationAreaCodeIn(probationAreaCodes),
)
  .and { root, query, _ ->
    root.fetch<Licence, CommunityOffenderManager>("responsibleCom", JoinType.INNER)
    query.restriction
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

fun hasPrisonCodeIn(prisonCodes: List<String>?): Specification<Licence>? = prisonCodes?.let {
  Licence::prisonCode.includedIn(it)
}

fun hasStatusCodeIn(statusCodes: List<LicenceStatus>?): Specification<Licence>? = statusCodes?.let {
  Licence::statusCode.includedIn(it)
}

fun hasNomsIdIn(nomsIds: List<String>?): Specification<Licence>? = nomsIds?.let {
  Licence::nomsId.includedIn(it)
}

fun hasResponsibleComIn(staffIds: List<Int>?): Specification<Licence>? = staffIds?.let {
  return Specification<Licence> { root, _, builder ->
    val joinOffenderManager: Join<Licence, CommunityOffenderManager> = root.join("responsibleCom", JoinType.INNER)
    builder.and(joinOffenderManager.get<Int>("staffIdentifier").`in`(it))
  }
}

fun hasPdusIn(pduCodes: List<String>?): Specification<Licence>? = pduCodes?.let {
  Licence::probationPduCode.includedIn(it)
}

fun hasProbationAreaCodeIn(areaCodes: List<String>?): Specification<Licence>? = areaCodes?.let {
  Licence::probationAreaCode.includedIn(it)
}
