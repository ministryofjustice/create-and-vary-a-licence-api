package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource

class ResourceAlreadyExistsException(message: String?, val existingResourceId: Long) : RuntimeException(message)
