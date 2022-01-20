package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.kotlinjpaspecificationdsl.and
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.kotlinjpaspecificationdsl.`in`
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import javax.validation.ValidationException

data class LicenceQueryObject(
  val prisonCodes: List<String>? = null,
  val statusCodes: List<LicenceStatus>? = null,
  val staffIds: List<Int>? = null,
  val nomsIds: List<String>? = null,
  val sortBy: String? = null,
  val sortOrder: String? = null
)

fun LicenceQueryObject.toSpecification(): Specification<Licence> = and(
  hasStatusCodeIn(statusCodes),
  hasPrisonCodeIn(prisonCodes),
  hasNomsIdIn(nomsIds),
  hasStaffIdIn(staffIds)
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
  Licence::prisonCode.`in`(prisonCodes)
}

fun hasStatusCodeIn(statusCodes: List<LicenceStatus>?): Specification<Licence>? = statusCodes?.let {
  Licence::statusCode.`in`(statusCodes)
}

fun hasNomsIdIn(nomsIds: List<String>?): Specification<Licence>? = nomsIds?.let {
  Licence::nomsId.`in`(nomsIds)
}

fun hasStaffIdIn(staffIds: List<Int>?): Specification<Licence>? = staffIds?.let {
  Licence::comStaffId.`in`(staffIds)
}
