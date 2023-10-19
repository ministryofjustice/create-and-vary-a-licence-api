package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.LicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.POLICY_V1_0
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.POLICY_V2_0
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.POLICY_V2_1
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.LicencePolicy as PublicLicencePolicy

@Service
class PublicLicencePolicyService(
  private val policies: List<LicencePolicy> = listOf(
    POLICY_V1_0,
    POLICY_V2_0,
    POLICY_V2_1,
  ),
) {

  fun getLicencePolicyByVersionNumber(versionNo: String): PublicLicencePolicy {
    val policy = policies.find { it.version == versionNo }
      ?: throw EntityNotFoundException("Policy version $versionNo not found")

    return policy.transformToPublicLicencePolicy()
  }
}
