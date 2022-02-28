package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.kotlinjpaspecificationdsl.and
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.kotlinjpaspecificationdsl.`in`
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import javax.persistence.criteria.Join
import javax.persistence.criteria.JoinType
import javax.validation.ValidationException

data class LicenceQueryObject(
  val prisonCodes: List<String>? = null,
  val statusCodes: List<LicenceStatus>? = null,
  val staffIds: List<Int>? = null,
  val nomsIds: List<String>? = null,
  val pdus: List<String>? = null,
  val sortBy: String? = null,
  val sortOrder: String? = null
)

fun LicenceQueryObject.toSpecification(): Specification<Licence> = and(
  hasStatusCodeIn(statusCodes),
  hasPrisonCodeIn(prisonCodes),
  hasNomsIdIn(nomsIds),
  hasResponsibleComIn(staffIds),
  hasPdusIn(pdus)
)

fun LicenceQueryObject.getSort(): Sort {
  if (sortBy == null) {
    return Sort.unsorted()
  }

  if (sortOrder == null) {
    return Sort.by(Sort.Direction.ASC, sortBy)
  }

  try {
    return Sort.by(Sort.Direction.fromString(sortOrder), sortBy)
  } catch (e: IllegalArgumentException) {
    throw ValidationException(e.message)
  }
}

fun hasPrisonCodeIn(prisonCodes: List<String>?): Specification<Licence>? = prisonCodes?.let {
  Licence::prisonCode.`in`(it)
}

fun hasStatusCodeIn(statusCodes: List<LicenceStatus>?): Specification<Licence>? = statusCodes?.let {
  Licence::statusCode.`in`(it)
}

fun hasNomsIdIn(nomsIds: List<String>?): Specification<Licence>? = nomsIds?.let {
  Licence::nomsId.`in`(it)
}

fun hasResponsibleComIn(staffIds: List<Int>?): Specification<Licence>? = staffIds?.let {
  return Specification<Licence> { root, _, builder ->
    val joinOffenderManager: Join<Licence, CommunityOffenderManager> = root.join("responsibleCom", JoinType.INNER)
    builder.and(joinOffenderManager.get<Int>("staffIdentifier").`in`(it))
  }
}

fun hasPdusIn(pduCodes: List<String>?): Specification<Licence>? = pduCodes?.let {
  Licence::probationPduCode.`in`(it)
}
