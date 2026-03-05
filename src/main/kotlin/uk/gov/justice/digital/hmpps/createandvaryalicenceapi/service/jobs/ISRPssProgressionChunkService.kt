package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.ISRProgressionLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType

@Service
class ISRPssProgressionChunkService(
  private val repository: ISRProgressionLicenceRepository,
) {

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun processApPssLicenceChunk(licenceIds: List<Long>) {
    if (licenceIds.isEmpty()) return

    val updatedCount = repository.updateTypeCodeToAp(licenceIds, LicenceType.AP_PSS.toString())

    val standardDeletedCount = repository.deletePssStandardConditions(licenceIds)

    val additionalDeletedCount = repository.deletePssAdditionalConditions(licenceIds)

    log.info(
      "ISR progression AP+Pss chunk completed. size={}, updatedToAP={}, standardDeleted={}, additionalDeleted={}",
      licenceIds.size,
      updatedCount,
      standardDeletedCount,
      additionalDeletedCount,
    )
  }

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)
  }
}
