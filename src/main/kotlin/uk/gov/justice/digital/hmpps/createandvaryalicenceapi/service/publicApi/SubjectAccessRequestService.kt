package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Content
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.SarContent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToSarAuditEvents
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToSarLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToSarLicenceEvents
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarAuditEvent as SarAuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarLicenceEvent as SarLicenceEvent

@Service
class SubjectAccessRequestService(
  private val licenceService: LicenceService,
  private val licenceRepository: LicenceRepository,
  private val licenceEventRepository: LicenceEventRepository,
  private val auditEventRepository: AuditEventRepository,
) {

  @Transactional
  fun getSarRecordsById(nomisId: String): SarContent? {
    val licences = licenceRepository.findAllByNomsId(nomisId)

    if (licences.isEmpty()) return null

    val licenceIds = licences.map { it.id }
    val auditEvents = getAuditEvents(licenceIds)
    val licenceEvents = getLicenceEvents(licenceIds)
    val sarLicences = licences.map { transformToSarLicence(licenceService.getLicenceById(it.id)) }

    return SarContent(
      Content(
        licences = sarLicences,
        auditEvents = auditEvents,
        licencesEvents = licenceEvents,
      ),
    )
  }

  private fun getAuditEvents(licenceIds: List<Long>): List<SarAuditEvent> {
    return auditEventRepository.findAllByLicenceIdIn(licenceIds)
      .transformToSarAuditEvents()
  }

  private fun getLicenceEvents(licenceIds: List<Long>): List<SarLicenceEvent> {
    return licenceEventRepository.findAllByLicenceIdIn(licenceIds)
      .transformToSarLicenceEvents()
  }
}
