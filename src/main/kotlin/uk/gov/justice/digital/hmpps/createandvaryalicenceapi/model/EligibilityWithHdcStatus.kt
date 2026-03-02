package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcStatus

data class EligibilityWithHdcStatus(
  @get:Schema(description = "The eligibility assessment")
  val assessment: EligibilityAssessment,
  @get:Schema(description = "The current HDC status of the case", example = "NOT_A_HDC_RELEASE")
  val currentHdcStatus: HdcStatus = HdcStatus.NOT_A_HDC_RELEASE
)
