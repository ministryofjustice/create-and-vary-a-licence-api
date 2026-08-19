package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

interface ModelHdcCase : Licence {
  val weeklyCurfewTimes: List<CurfewTimes>
  val firstNightCurfewTimes: CurfewTimes?
  val curfewAddress: HdcCurfewAddress?
}
