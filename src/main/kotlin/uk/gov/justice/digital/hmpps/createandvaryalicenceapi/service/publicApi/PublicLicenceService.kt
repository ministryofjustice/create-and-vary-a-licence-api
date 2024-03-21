package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ExclusionZoneService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToPublicLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Service
class PublicLicenceService(
  private val licenceRepository: LicenceRepository,
  private val exclusionZoneService: ExclusionZoneService,
  private val licenceService: LicenceService,
) {

  @Transactional
  fun getAllLicencesByCrn(crn: String): List<LicenceSummary> {
    val licences = licenceRepository.findAllByCrnAndStatusCodeIn(crn, LicenceStatus.IN_FLIGHT_LICENCES)

    return licences.map { it.transformToPublicLicenceSummary() }
  }

  @Transactional
  fun getAllLicencesByPrisonNumber(prisonNumber: String): List<LicenceSummary> {
    val licences = licenceRepository.findAllByNomsIdAndStatusCodeIn(prisonNumber, LicenceStatus.IN_FLIGHT_LICENCES)

    return licences.map { it.transformToPublicLicenceSummary() }
  }

  @Transactional
  fun getImageUpload(licenceId: Long, conditionId: Long): ByteArray? {
    return exclusionZoneService.getExclusionZoneImage(licenceId, conditionId)
  }

  fun getLicenceById(id: Long): Licence {
    return licenceService.getLicenceById(id).transformToPublicLicence()
  }
}
