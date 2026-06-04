package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcCurfewAddress

@Schema(description = "Home Detention Curfew licence data")
data class HdcLicenceDataResponse(

  @field:Schema(
    description = "Unique identifier of the HDC licence",
    example = "12345",
  )
  val licenceId: Long? = null,

  @field:Schema(
    description = "Curfew address associated with the licence",
  )
  val curfewAddress: HdcCurfewAddress? = null,

  @field:Schema(
    description = "Curfew times for the first night of the licence",
  )
  val firstNightCurfewTimes: CurfewTimes? = null,

  @field:Schema(
    description = "Weekly curfew schedule for the licence",
  )
  val weeklyCurfewTimes: List<CurfewTimes>? = null,
)
