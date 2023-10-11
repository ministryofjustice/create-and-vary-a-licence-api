package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceStatus as PublicLicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceType as PublicLicenceType

@Service
class PublicLicenceService(
  private val licenceRepository: LicenceRepository,
) {

  @Transactional
  fun getAllLicencesByCrn(crn: String): List<LicenceSummary> {
    val licences = licenceRepository.findAllByCrnAndStatusCodeIn(crn, LicenceStatus.IN_FLIGHT_LICENCES)

    val modelLicenceSummary = licences.map {
      transformToPublicLicenceSummary(it, it.typeCode.mapToPublicLicenceType(), it.statusCode.mapToPublicLicenceStatus())
    }

    return modelLicenceSummary
  }

  private fun LicenceType.mapToPublicLicenceType() =
    when {
      this == LicenceType.AP -> PublicLicenceType.AP
      this == LicenceType.PSS -> PublicLicenceType.PSS
      this == LicenceType.AP_PSS -> PublicLicenceType.AP_PSS
      else -> error("No matching licence type found")
    }

  private fun LicenceStatus.mapToPublicLicenceStatus() =
    when {
      this == LicenceStatus.IN_PROGRESS -> PublicLicenceStatus.IN_PROGRESS
      this == LicenceStatus.SUBMITTED -> PublicLicenceStatus.SUBMITTED
      this == LicenceStatus.APPROVED -> PublicLicenceStatus.APPROVED
      this == LicenceStatus.ACTIVE -> PublicLicenceStatus.ACTIVE
      this == LicenceStatus.REJECTED -> PublicLicenceStatus.REJECTED
      this == LicenceStatus.INACTIVE -> PublicLicenceStatus.INACTIVE
      this == LicenceStatus.VARIATION_IN_PROGRESS -> PublicLicenceStatus.VARIATION_IN_PROGRESS
      this == LicenceStatus.VARIATION_SUBMITTED -> PublicLicenceStatus.VARIATION_SUBMITTED
      this == LicenceStatus.VARIATION_APPROVED -> PublicLicenceStatus.VARIATION_APPROVED
      else -> error("No matching licence status found")
    }
}
