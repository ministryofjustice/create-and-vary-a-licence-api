package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCaseLoad

@Service
class CaCaseloadService {
  fun getPrisonView(prisons: List<String>, searchString: String): List<CaCaseLoad> = emptyList()
  fun getProbationView(prisons: List<String>, searchString: String): List<CaCaseLoad> = emptyList()
}
