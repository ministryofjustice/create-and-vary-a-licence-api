package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ModelHdcCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.PublicCurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.PublicHdcCurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.PublicHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.PublicLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.upload.UploadFileConditionsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToPublicLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Service
class PublicLicenceService(
  private val licenceRepository: LicenceRepository,
  private val licenceService: LicenceService,
  private val uploadFileConditionsService: UploadFileConditionsService,
  @param:Value($$"${feature.toggle.publicApiHdcDetails.enabled}") private val publicApiHdcDetailsEnabled: Boolean = false,
) {

  fun getLicenceById(id: Long): Licence {
    val licence = licenceService.getLicenceById(id)

    return if (publicApiHdcDetailsEnabled && licence is ModelHdcCase) {
      PublicHdcLicence(
        licence = licence.transformToPublicLicence(),
        curfewAddress = toPublicHdcCurfewAddress(licence),
        firstNightCurfewTimes = licence.firstNightCurfewTimes?.toPublicCurfewTimes(),
        curfewTimes = licence.weeklyCurfewTimes.sortedBy { it.curfewTimesSequence }.map { it.toPublicCurfewTimes() },
      )
    } else {
      licence.transformToPublicLicence()
    }
  }

  private fun toPublicHdcCurfewAddress(licence: ModelHdcCase): PublicHdcCurfewAddress? = licence.curfewAddress?.let {
    PublicHdcCurfewAddress(
      uprn = it.uprn,
      firstLine = it.firstLine,
      secondLine = it.secondLine,
      townOrCity = it.townOrCity,
      county = it.county,
      postcode = it.postcode,
    )
  }

  @Transactional
  fun getAllLicencesByCrn(crn: String): List<PublicLicenceSummary> {
    val licences = licenceRepository.findAllByCrnAndStatusCodeIn(crn, LicenceStatus.IN_FLIGHT_LICENCES)

    return licences.map { it.transformToPublicLicenceSummary() }
  }

  @Transactional
  fun getAllLicencesByPrisonNumber(prisonNumber: String): List<PublicLicenceSummary> {
    val licences = licenceRepository.findAllByNomsIdAndStatusCodeIn(prisonNumber, LicenceStatus.IN_FLIGHT_LICENCES)

    return licences.map { it.transformToPublicLicenceSummary() }
  }

  @Transactional
  fun getImageUpload(licenceId: Long, conditionId: Long) = uploadFileConditionsService.getImage(licenceId, conditionId)

  private fun CurfewTimes.toPublicCurfewTimes() = PublicCurfewTimes(
    fromDay = fromDay,
    fromTime = fromTime,
    untilDay = untilDay,
    untilTime = untilTime,
  )
}
