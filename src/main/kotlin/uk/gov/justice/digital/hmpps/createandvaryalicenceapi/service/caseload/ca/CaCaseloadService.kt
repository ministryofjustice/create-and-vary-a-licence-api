package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonCaseAdminSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.PrisonUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison.CaPrisonCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.probation.CaProbationCaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab

@Service
class CaCaseloadService(
  private val prisonCaseloadService: CaPrisonCaseloadService,
  private val probationCaseloadService: CaProbationCaseloadService,
) {
  fun getPrisonOmuCaseload(prisonCaseload: Set<String>, searchString: String?): List<CaCase> = prisonCaseloadService.getPrisonOmuCaseload(prisonCaseload, searchString)

  fun getProbationOmuCaseload(
    prisonCaseload: Set<String>,
    searchString: String?,
  ): List<CaCase> = probationCaseloadService.getProbationOmuCaseload(prisonCaseload, searchString)

  fun searchForOffenderOnPrisonCaseAdminCaseload(body: PrisonUserSearchRequest): PrisonCaseAdminSearchResult {
    val prisonCases = getPrisonOmuCaseload(body.prisonCaseloads, body.query)

    val (attentionNeededCases, inPrisonCases) = prisonCases.partition { it.tabType == CaViewCasesTab.ATTENTION_NEEDED }

    val probationCases = getProbationOmuCaseload(body.prisonCaseloads, body.query)

    return PrisonCaseAdminSearchResult(inPrisonCases, probationCases, attentionNeededCases)
  }
}
