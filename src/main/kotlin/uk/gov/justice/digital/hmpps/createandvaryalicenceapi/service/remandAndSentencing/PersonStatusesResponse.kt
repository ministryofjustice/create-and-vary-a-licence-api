package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.remandAndSentencing

data class PersonStatus(val isOnRemand: Boolean, val isCurrentlyOnRecall: Boolean)

data class PersonStatusesResponse(val peopleStatus: Map<String, PersonStatus>)
