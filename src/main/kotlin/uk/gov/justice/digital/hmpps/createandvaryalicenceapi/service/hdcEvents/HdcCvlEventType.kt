package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

enum class HdcCvlEventType {
  OPT_IN,
  OPT_OUT,
  REFUSE,
  POSTPONE,
  RESUME,
  ADDRESS_WITHDRAWN,
  RESIDENTIAL_ADDRESS_CHANGED_TO_APPROVED_PREMISES,
  COM_COMPLETES_ADDRESS_CHECKS,
}
