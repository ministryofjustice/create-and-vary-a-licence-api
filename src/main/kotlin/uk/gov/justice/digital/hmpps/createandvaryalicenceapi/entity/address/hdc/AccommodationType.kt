package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.hdc

enum class AccommodationType(val cvlAccommodationType: CvlAccommodationType) {
  RESIDENTIAL(CvlAccommodationType.RESIDENTIAL),
  CAS(CvlAccommodationType.CAS),
  CAS_1_APPROVED_PREMISES(CvlAccommodationType.CAS),
  CAS_2(CvlAccommodationType.CAS),
}
