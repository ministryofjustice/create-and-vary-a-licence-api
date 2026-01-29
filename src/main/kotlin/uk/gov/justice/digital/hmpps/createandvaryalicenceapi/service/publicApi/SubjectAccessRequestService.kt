package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.TimeServedExternalRecordsRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

@Transactional(readOnly = true)
@Service
class SubjectAccessRequestService(
  private val licenceService: LicenceService,
  private val licenceRepository: LicenceRepository,
  private val externalRecordsRepository: TimeServedExternalRecordsRepository,
  @param:Value("\${self.api.link}") private val baseUrl: String,
) : HmppsPrisonSubjectAccessRequestService {

  override fun getPrisonContentFor(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? {
    val licenceIds = licenceRepository.findAllByNomsId(prn).map { it.id }

    val externalRecords = externalRecordsRepository.findAllByNomsId(prn)

    if (licenceIds.isEmpty() && externalRecords.isEmpty()) return null

    val licenceBuilder = SubjectAccessRequestResponseBuilder(baseUrl)

    licenceIds.forEach {
      licenceBuilder.addLicence(licenceService.getLicenceById(it))
    }

    externalRecords.forEach {
      licenceBuilder.addTimeServedExternalRecord(it)
    }

    return licenceBuilder.build()
  }
}
