package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicencePolicyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.LicencePolicy as PublicLicencePolicy

@Service
class PublicLicencePolicyService(
  private val licencePolicyService: LicencePolicyService,
) {

  fun getLicencePolicyByVersionNumber(versionNo: String): PublicLicencePolicy {
    val policy = licencePolicyService.allPolicies().find { it.version == versionNo }
      ?: throw EntityNotFoundException("Policy version $versionNo not found")

    return policy.transformToPublicLicencePolicy()
  }

  fun getLatestLicencePolicy(): PublicLicencePolicy {
    val transformToPublicLicencePolicy = licencePolicyService.currentPolicy()
    return transformToPublicLicencePolicy.transformToPublicLicencePolicy()
  }
}
