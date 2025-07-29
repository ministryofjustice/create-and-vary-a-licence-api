package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarContent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService

@Service
class SubjectAccessRequestService(
  private val licenceService: LicenceService,
  private val licenceRepository: LicenceRepository,
  private val auditEventRepository: AuditEventRepository,
  @Value("\${self.api.link}") private val baseUrl: String,
) {

  @Transactional
  fun getSarRecordsById(nomisId: String): SarContent? {
    val licenceIds = licenceRepository.findAllByNomsId(nomisId).map { it.id }

    if (licenceIds.isEmpty()) return null

    val licenceBuilder = SubjectAccessRequestResponseBuilder(baseUrl)

    licenceIds.forEach {
      licenceBuilder.addLicence(licenceService.getLicenceById(it))
    }

    val auditEvents = auditEventRepository.findAllByLicenceIdIn(licenceIds)

    return licenceBuilder.build(auditEvents)
  }
}
