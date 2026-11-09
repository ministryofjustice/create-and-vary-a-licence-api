package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi.v2

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ModelHdcCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.PublicCurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.PublicHdcCurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.PublicHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService

@Service
class PublicLicenceServiceV2(private val licenceService: LicenceService) {
  fun getLicenceById(id: Long): Licence {
    val licence = licenceService.getLicenceById(id)
    val publicLicence = licence.transformToPublicLicence()

    return if (licence is ModelHdcCase) {
      PublicHdcLicence(
        licence = publicLicence,
        curfewAddress = licence.curfewAddress?.let {
          PublicHdcCurfewAddress(
            uprn = it.uprn,
            firstLine = it.firstLine,
            secondLine = it.secondLine,
            townOrCity = it.townOrCity,
            county = it.county,
            postcode = it.postcode,
          )
        },
        firstNightCurfewTimes = licence.firstNightCurfewTimes?.toPublicCurfewTimes(),
        curfewTimes = licence.weeklyCurfewTimes.sortedBy { it.curfewTimesSequence }.map { it.toPublicCurfewTimes() },
      )
    } else {
      publicLicence
    }
  }

  private fun CurfewTimes.toPublicCurfewTimes() = PublicCurfewTimes(
    fromDay = fromDay,
    fromTime = fromTime,
    untilDay = untilDay,
    untilTime = untilTime,
  )
}
