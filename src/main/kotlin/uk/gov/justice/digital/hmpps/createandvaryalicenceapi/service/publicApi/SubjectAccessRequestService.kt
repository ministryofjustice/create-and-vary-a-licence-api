package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Content
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.SarContent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToModelAuditEvents
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToModelEvents

@Service
class SubjectAccessRequestService(
  private val licenceService: LicenceService,
  private val licenceRepository: LicenceRepository,
  private val licenceEventRepository: LicenceEventRepository,
  private val auditEventRepository: AuditEventRepository,
) {
  @Transactional
  fun getSarRecordsById(nomisId: String): SarContent? {
    val licences =
      licenceRepository.findAllByNomsId(nomisId)
    if (licences.isEmpty()) return null
    val licenceIds = licences.map { it.id }
    return SarContent(
      Content(
        licences = licences.map { licenceService.getLicenceById(it.id) },
        auditEvents = getAuditEvents(licenceIds),
        licencesEvents = getLicenceEvents(licenceIds),
      ),
    )
  }

  private fun getAuditEvents(licenceIds: List<Long>): List<uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent> {
    return auditEventRepository.findAllByLicenceIdIn(licenceIds)
      .transformToModelAuditEvents()
  }

  private fun getLicenceEvents(licenceIds: List<Long>): List<LicenceEvent> {
    return licenceEventRepository.findAllByLicenceIdIn(licenceIds)
      .transformToModelEvents()
  }
}
