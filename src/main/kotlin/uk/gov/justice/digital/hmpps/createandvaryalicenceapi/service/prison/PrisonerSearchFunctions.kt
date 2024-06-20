package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

fun PrisonerSearchPrisoner.getReleaseDate() = confirmedReleaseDate ?: conditionalReleaseDate
