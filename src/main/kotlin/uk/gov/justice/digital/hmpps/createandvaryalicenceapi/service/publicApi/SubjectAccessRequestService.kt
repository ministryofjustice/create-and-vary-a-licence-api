package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.Content
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarContent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.transformToSarAuditEvents
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.transformToSarLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarAuditEvent as SarAuditEvent

@Service
class SubjectAccessRequestService(
  private val licenceService: LicenceService,
  private val licenceRepository: LicenceRepository,
  private val auditEventRepository: AuditEventRepository,
) {

  @Transactional
  fun getSarRecordsById(nomisId: String): SarContent? {
    val licences = licenceRepository.findAllByNomsId(nomisId)

    if (licences.isEmpty()) return null

    val licenceIds = licences.map { it.id }
    val auditEvents = getAuditEvents(licenceIds)
    val sarLicences = licences.map { licenceService.getLicenceById(it.id) }.transformToSarLicence()

    return SarContent(
      Content(
        licences = sarLicences,
        auditEvents = auditEvents,
      ),
    )
  }

  private fun getAuditEvents(licenceIds: List<Long>): List<SarAuditEvent> {
    return auditEventRepository.findAllByLicenceIdIn(licenceIds)
      .transformToSarAuditEvents()
  }
}
