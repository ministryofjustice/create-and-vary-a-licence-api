package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.upload.pdf

data class UploadPdfExtract(
  var description: String,
  var fullSizeImage: ByteArray,
  var thumbnailImage: ByteArray,
)
