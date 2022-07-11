package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.ILicenceCondition

data class ConditionVariations(val old: ILicenceCondition, val new: ILicenceCondition)
