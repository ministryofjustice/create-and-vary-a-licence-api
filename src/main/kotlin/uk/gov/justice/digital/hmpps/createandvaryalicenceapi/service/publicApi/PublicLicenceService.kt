package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Service
class PublicLicenceService(
  private val licenceRepository: LicenceRepository,
  private val additionalConditionRepository: AdditionalConditionRepository,
  private val additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository,
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
    licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("Licence $licenceId not found") }

    val additionalCondition = additionalConditionRepository
      .findById(conditionId)
      .orElseThrow { EntityNotFoundException("Condition $conditionId not found") }

    val uploadIds = additionalCondition.additionalConditionUploadSummary.map { it.uploadDetailId }
    if (uploadIds.isEmpty()) {
      throw EntityNotFoundException("Condition $conditionId upload details not found")
    }

    val upload = additionalConditionUploadDetailRepository
      .findById(uploadIds.first())
      .orElseThrow { EntityNotFoundException("Condition $conditionId upload details not found") }

    return upload.fullSizeImage
  }

  fun getLicenceById(id: Long): Licence {
    return licenceService.getLicenceById(id).transformToPublicLicence()
  }
}
