package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository

import au.com.console.jpaspecificationdsl.and
import au.com.console.jpaspecificationdsl.`in`
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

data class LicenceQueryObject(
  val prisonCodes: List<String>? = null,
  val statusCodes: List<LicenceStatus>? = null,
  val staffIds: List<Int>? = null,
  val nomsIds: List<String>? = null,
)

fun LicenceQueryObject.toSpecification(): Specification<Licence> = and(
  hasStatusCodeIn(statusCodes),
  hasPrisonCodeIn(prisonCodes),
  hasNomsIdIn(nomsIds),
  hasStaffIdIn(staffIds)
)

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
