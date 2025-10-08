package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

@Service
class SubjectAccessRequestService(
  private val licenceService: LicenceService,
  private val licenceRepository: LicenceRepository,
  private val auditEventRepository: AuditEventRepository,
  @param:Value("\${self.api.link}") private val baseUrl: String,
) : HmppsPrisonSubjectAccessRequestService {

  @Transactional
  override fun getPrisonContentFor(prn: String, fromDate: LocalDate?, toDate: LocalDate?): HmppsSubjectAccessRequestContent? {
    val licenceIds = licenceRepository.findAllByNomsId(prn).map { it.id }

    if (licenceIds.isEmpty()) return null

    val licenceBuilder = SubjectAccessRequestResponseBuilder(baseUrl)

    licenceIds.forEach {
      licenceBuilder.addLicence(licenceService.getLicenceById(it))
    }

    val auditEvents = auditEventRepository.findAllByLicenceIdIn(licenceIds)

    return licenceBuilder.build(auditEvents)
  }
}
