package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.User
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.OffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.WorkLoadAllocationResponse

@Component
class OffenderManagerMapper {

  fun mapFrom(
    staff: User,
    staffAllocation: WorkLoadAllocationResponse,
  ): OffenderManager {
    val team = staff.teams.firstOrNull { it.code == staffAllocation.teamCode }

    return OffenderManager(
      staffIdentifier = staff.id,
      code = staff.code,
      username = staff.username?.trim()?.uppercase(),
      email = staff.email,
      forename = staff.name.forename,
      surname = staff.name.surname,
      providerCode = staff.provider.code,
      providerDescription = staff.provider.description,
      teamCode = staffAllocation.teamCode,
      teamDescription = team?.description ?: "",
      boroughCode = team?.borough?.code ?: "",
      boroughDescription = team?.borough?.description ?: "",
      districtCode = team?.district?.code ?: "",
      districtDescription = team?.district?.description ?: "",
      crn = staffAllocation.crn,
    )
  }

  fun mapFrom(cm: CommunityManager): OffenderManager = OffenderManager(
    staffIdentifier = cm.id,
    code = cm.code,
    username = cm.username?.trim()?.uppercase(),
    email = cm.email,
    forename = cm.name.forename,
    surname = cm.name.surname,
    providerCode = cm.provider.code,
    providerDescription = cm.provider.description,
    teamCode = cm.team.code,
    teamDescription = cm.team.description,
    boroughCode = cm.team.borough.code,
    boroughDescription = cm.team.borough.description,
    districtCode = cm.team.district.code,
    districtDescription = cm.team.district.description,
    crn = cm.case.crn,
  )
}
